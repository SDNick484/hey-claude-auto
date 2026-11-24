package io.github.sdnick484.heyclaude.spike

import java.util.concurrent.CopyOnWriteArraySet

/**
 * The one conversation, held in memory only. The spike keeps nothing across
 * process death: it starts again from the seed message.
 */
object Conversation {
    const val ID = "claude"
    const val TITLE = "Claude"

    /** The standing message. The car needs at least one message to hang a reply on. */
    const val SEED = "What do you need?"

    data class Message(
        val fromClaude: Boolean,
        val text: String,
        val timeMillis: Long,
        val read: Boolean,
    )

    private val lock = Any()
    private val messages = ArrayList<Message>()
    private val listeners = CopyOnWriteArraySet<() -> Unit>()

    fun snapshot(): List<Message> = synchronized(lock) {
        seedIfEmpty()
        messages.toList()
    }

    fun addFromUser(text: String) = change {
        messages += Message(fromClaude = false, text = text, timeMillis = now(), read = true)
    }

    fun addFromClaude(text: String) = change {
        messages += Message(fromClaude = true, text = text, timeMillis = now(), read = false)
    }

    fun markAllRead() = change {
        messages.replaceAll { it.copy(read = true) }
    }

    /** Forgets everything and goes back to the seed message alone. */
    fun reset() = change {
        messages.clear()
    }

    fun addListener(listener: () -> Unit) {
        listeners += listener
    }

    fun removeListener(listener: () -> Unit) {
        listeners -= listener
    }

    private fun change(block: () -> Unit) {
        synchronized(lock) {
            seedIfEmpty()
            block()
            seedIfEmpty()
        }
        listeners.forEach { it() }
    }

    private fun seedIfEmpty() {
        if (messages.isEmpty()) {
            messages += Message(fromClaude = true, text = SEED, timeMillis = now(), read = false)
        }
    }

    private fun now() = System.currentTimeMillis()
}
