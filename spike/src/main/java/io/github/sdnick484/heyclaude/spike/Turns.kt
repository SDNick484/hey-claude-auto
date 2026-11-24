package io.github.sdnick484.heyclaude.spike

import android.content.Context
import android.os.Handler
import android.os.Looper

/**
 * The whole "backend" of the spike: echo what was said, after a pause.
 *
 * The pause stands in for the relay. A real answer takes seconds to arrive, and
 * what Android Auto does with a message that turns up a few seconds after the
 * driver finished speaking is part of what the head-unit test has to show.
 */
object Turns {
    private const val ECHO_DELAY_MS = 3_000L
    private val main = Handler(Looper.getMainLooper())

    /**
     * @param via which entry point delivered the text, for the event log.
     * @param done called once the echo has been posted.
     */
    fun onUserSaid(context: Context, text: String, via: String, done: () -> Unit = {}) {
        val app = context.applicationContext
        EventLog.add("HEARD via $via: \"$text\"")
        Conversation.addFromUser(text)
        // Shows the reply in the phone's shade and ends its "sending" spinner.
        Notifier.refreshIfShowing(app)
        main.postDelayed({
            Conversation.addFromClaude("You said: $text")
            Notifier.post(app, alert = true)
            EventLog.add("ECHO posted as a new message")
            done()
        }, ECHO_DELAY_MS)
    }

    fun onMarkedRead(via: String) {
        EventLog.add("MARK-AS-READ via $via")
        Conversation.markAllRead()
    }

    /** Starts over with only "What do you need?" waiting. */
    fun postStandingMessage(context: Context, quiet: Boolean) {
        Conversation.reset()
        Notifier.post(context.applicationContext, alert = true, quiet = quiet)
        EventLog.add("STANDING message posted (${if (quiet) "quiet channel" else "heads-up channel"})")
    }
}
