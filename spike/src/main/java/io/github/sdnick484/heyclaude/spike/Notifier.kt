package io.github.sdnick484.heyclaude.spike

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat

/**
 * Posts the conversation as the kind of notification Android Auto understands:
 * a MessagingStyle notification with a reply action and a mark-as-read action.
 *
 * There is exactly one notification, for the one conversation. It is never
 * cancelled by the app, because the question under test is whether a message
 * that has already been heard can still be replied to.
 */
object Notifier {
    const val ACTION_REPLY = "io.github.sdnick484.heyclaude.spike.REPLY"
    const val ACTION_MARK_READ = "io.github.sdnick484.heyclaude.spike.MARK_READ"
    const val KEY_REPLY_TEXT = "reply_text"

    /** Heads-up on the phone and, per the docs, in the car. */
    private const val CHANNEL_LOUD = "claude_loud"

    /** No sound and no heads-up. Used to see whether a quiet message is still reachable. */
    private const val CHANNEL_QUIET = "claude_quiet"

    private const val NOTIFICATION_ID = 1
    private const val REQUEST_REPLY = 1
    private const val REQUEST_MARK_READ = 2
    private const val REQUEST_OPEN = 3

    private var lastChannel = CHANNEL_LOUD

    fun canPost(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Posts or updates the notification.
     *
     * @param alert whether this update should announce itself. An update that
     *   only records the driver's own reply should not.
     * @param quiet post on the silent channel instead of the heads-up one.
     */
    fun post(context: Context, alert: Boolean, quiet: Boolean = false) {
        if (!canPost(context)) {
            EventLog.add("NOT posted: notification permission has not been granted")
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        ensureChannels(manager)
        lastChannel = if (quiet) CHANNEL_QUIET else CHANNEL_LOUD

        val notification = NotificationCompat.Builder(context, lastChannel)
            .setSmallIcon(R.drawable.ic_bubble)
            .setStyle(messagingStyle(context))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setOnlyAlertOnce(!alert)
            .setContentIntent(openApp(context))
            .addAction(replyAction(context))
            // The docs allow either action to be invisible. Mark-as-read is, as in their sample.
            .addInvisibleAction(markAsReadAction(context))
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    /** Updates the notification only if it is currently showing, without announcing it. */
    fun refreshIfShowing(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.activeNotifications.any { it.id == NOTIFICATION_ID }) {
            post(context, alert = false, quiet = lastChannel == CHANNEL_QUIET)
        }
    }

    /**
     * Shows the newest run of Claude's messages plus anything the driver has
     * said since. Everything older is passed as history, which is context for
     * the system and is not displayed.
     */
    private fun messagingStyle(context: Context): NotificationCompat.MessagingStyle {
        val me = Person.Builder().setName("Me").setKey("me").build()
        val claude = Person.Builder()
            .setName(Conversation.TITLE)
            .setKey(Conversation.ID)
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_bubble))
            .build()
        val style = NotificationCompat.MessagingStyle(me)
            .setConversationTitle(Conversation.TITLE)
            .setGroupConversation(false)

        val all = Conversation.snapshot()
        var start = all.size
        while (start > 0 && !all[start - 1].fromClaude) start--
        while (start > 0 && all[start - 1].fromClaude) start--

        all.forEachIndexed { index, message ->
            // A null sender means the device user.
            val entry = NotificationCompat.MessagingStyle.Message(
                message.text,
                message.timeMillis,
                if (message.fromClaude) claude else null,
            )
            if (index < start) style.addHistoricMessage(entry) else style.addMessage(entry)
        }
        return style
    }

    private fun replyAction(context: Context): NotificationCompat.Action {
        val intent = Intent(context, ReplyReceiver::class.java).setAction(ACTION_REPLY)
        // Mutable so that the system, or Android Auto, can attach the reply text.
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_REPLY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        val remoteInput = RemoteInput.Builder(KEY_REPLY_TEXT).setLabel("Reply").build()
        return NotificationCompat.Action.Builder(R.drawable.ic_bubble, "Reply", pending)
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
            .setShowsUserInterface(false)
            .addRemoteInput(remoteInput)
            .build()
    }

    private fun markAsReadAction(context: Context): NotificationCompat.Action {
        val intent = Intent(context, ReplyReceiver::class.java).setAction(ACTION_MARK_READ)
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_MARK_READ,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Action.Builder(R.drawable.ic_bubble, "Mark as read", pending)
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
            .setShowsUserInterface(false)
            .build()
    }

    private fun openApp(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            REQUEST_OPEN,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun ensureChannels(manager: NotificationManager) {
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_LOUD,
                "Messages from Claude",
                NotificationManager.IMPORTANCE_HIGH,
            ),
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_QUIET,
                "Messages from Claude (quiet)",
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }
}
