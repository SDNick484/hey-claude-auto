package io.github.sdnick484.heyclaude.spike

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput

/**
 * Receives the two notification actions. Android Auto fires the same pending
 * intents as the phone's notification shade does, so this is the entry point
 * for a voice reply in the car and for a typed reply on the phone.
 */
class ReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Notifier.ACTION_REPLY -> {
                val text = RemoteInput.getResultsFromIntent(intent)
                    ?.getCharSequence(Notifier.KEY_REPLY_TEXT)
                    ?.toString()
                if (text.isNullOrBlank()) {
                    EventLog.add("REPLY action fired with no text")
                    return
                }
                // Keep the process alive until the echo has been posted.
                val pending = goAsync()
                Turns.onUserSaid(context, text, via = "notification reply action") {
                    pending.finish()
                }
            }

            Notifier.ACTION_MARK_READ -> Turns.onMarkedRead(via = "notification action")
        }
    }
}
