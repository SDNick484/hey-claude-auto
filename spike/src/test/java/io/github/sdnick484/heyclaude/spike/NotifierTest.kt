package io.github.sdnick484.heyclaude.spike

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Guards the things Android Auto checks before it will treat a notification as
 * a message it can read out and reply to. If one of these is wrong the head
 * unit simply ignores the app, which would make the hands-on test worthless.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotifierTest {
    private val app: Application = RuntimeEnvironment.getApplication()
    private val manager = app.getSystemService(NotificationManager::class.java)

    @Before
    fun setUp() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        Conversation.reset()
    }

    @Test
    fun `the standing message is a messaging notification Android Auto accepts`() {
        Turns.postStandingMessage(app, quiet = false)

        val posted = postedNotification()
        val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(posted)!!
        assertEquals("Claude", style.conversationTitle)
        assertFalse(style.isGroupConversation)
        assertEquals(listOf("What do you need?"), style.messages.map { it.text.toString() })
        assertEquals("Claude", style.messages.single().person?.name)

        val reply = NotificationCompat.getAction(posted, 0)!!
        assertEquals(NotificationCompat.Action.SEMANTIC_ACTION_REPLY, reply.semanticAction)
        assertFalse(reply.showsUserInterface)
        assertEquals(1, reply.remoteInputs!!.size)

        val markAsRead = NotificationCompat.getInvisibleActions(posted).single()
        assertEquals(
            NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ,
            markAsRead.semanticAction,
        )
        assertFalse(markAsRead.showsUserInterface)
    }

    @Test
    fun `a reply sent through the notification is echoed back as a new message`() {
        Turns.postStandingMessage(app, quiet = false)

        sendReply(postedNotification(), "is anyone there")
        shadowOf(Looper.getMainLooper()).idle()

        // Before the pause is over: the reply is recorded, and nothing new is announced.
        assertEquals(
            listOf("What do you need?", "is anyone there"),
            Conversation.snapshot().map { it.text },
        )
        assertTrue(postedNotification().flags and Notification.FLAG_ONLY_ALERT_ONCE != 0)

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4))

        assertEquals(
            listOf("What do you need?", "is anyone there", "You said: is anyone there"),
            Conversation.snapshot().map { it.text },
        )
        // Only the new message is shown, so only the new message is read out.
        val style = NotificationCompat.MessagingStyle
            .extractMessagingStyleFromNotification(postedNotification())!!
        assertEquals(listOf("You said: is anyone there"), style.messages.map { it.text.toString() })
        assertEquals(0, postedNotification().flags and Notification.FLAG_ONLY_ALERT_ONCE)
    }

    @Test
    fun `marking as read leaves the message in place to be replied to`() {
        Turns.postStandingMessage(app, quiet = false)

        NotificationCompat.getInvisibleActions(postedNotification()).single().actionIntent!!.send()
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(Conversation.snapshot().all { it.read })
        assertEquals(1, manager.activeNotifications.size)
    }

    private fun postedNotification(): Notification =
        manager.activeNotifications.single().notification

    /** Fires the reply action the way the system does once it has the driver's words. */
    private fun sendReply(notification: Notification, text: String) {
        val reply = NotificationCompat.getAction(notification, 0)!!
        val fillIn = Intent()
        RemoteInput.addResultsToIntent(
            reply.remoteInputs!!,
            fillIn,
            Bundle().apply { putCharSequence(Notifier.KEY_REPLY_TEXT, text) },
        )
        reply.actionIntent!!.send(app, 0, fillIn)
    }
}
