package io.github.sdnick484.heyclaude.spike

import android.util.Log
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CopyOnWriteArraySet

/**
 * A record of what Android Auto asked the app to do, shown on the phone and
 * written to logcat under the tag [TAG]. It exists so the head-unit test can be
 * reported precisely: which callback fired, when, and with what text.
 *
 * This logs dictated text. That is fine for an echo test and is exactly what
 * the real app must not do for passphrase turns.
 */
object EventLog {
    const val TAG = "HeyClaudeSpike"
    private const val MAX_LINES = 200

    private val clock = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val lines = ArrayDeque<String>()
    private val listeners = CopyOnWriteArraySet<() -> Unit>()

    fun add(event: String) {
        Log.i(TAG, event)
        synchronized(lines) {
            lines.addLast("${LocalTime.now().format(clock)}  $event")
            while (lines.size > MAX_LINES) lines.removeFirst()
        }
        listeners.forEach { it() }
    }

    /** Newest first. */
    fun text(): String = synchronized(lines) { lines.reversed().joinToString("\n") }

    fun addListener(listener: () -> Unit) {
        listeners += listener
    }

    fun removeListener(listener: () -> Unit) {
        listeners -= listener
    }
}
