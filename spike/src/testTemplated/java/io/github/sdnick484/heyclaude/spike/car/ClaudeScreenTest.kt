package io.github.sdnick484.heyclaude.spike.car

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.os.Looper
import androidx.car.app.OnDoneCallback
import androidx.car.app.messaging.model.ConversationItem
import androidx.car.app.model.ListTemplate
import androidx.car.app.testing.TestCarContext
import io.github.sdnick484.heyclaude.spike.Conversation
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The car screen cannot be exercised without a head unit, so this at least
 * proves the template builds without the library rejecting it, and that the
 * host's two callbacks reach the echo.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ClaudeScreenTest {
    private val app: Application = RuntimeEnvironment.getApplication()
    private val manager = app.getSystemService(NotificationManager::class.java)
    private val screen by lazy { ClaudeScreen(TestCarContext.createCarContext(app)) }
    private val ignored = object : OnDoneCallback {}

    @Before
    fun setUp() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        Conversation.reset()
    }

    @Test
    fun `the screen lists one conversation holding the seed message`() {
        val conversation = conversationShown()

        assertEquals("Claude", conversation.title.toString())
        assertEquals(
            listOf("What do you need?"),
            conversation.messages.map { it.body.toString() },
        )
        assertEquals("Claude", conversation.messages.single().sender?.name)
    }

    @Test
    fun `a reply dictated on the car screen is echoed back and announced`() {
        conversationShown().conversationCallbackDelegate.sendTextReply("is anyone there", ignored)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4))

        assertEquals(
            listOf("What do you need?", "is anyone there", "You said: is anyone there"),
            conversationShown().messages.map { it.body.toString() },
        )
        assertEquals("the echo is also posted as a notification", 1, manager.activeNotifications.size)
    }

    @Test
    fun `mark as read from the car screen marks every message read`() {
        conversationShown().conversationCallbackDelegate.sendMarkAsRead(ignored)
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(conversationShown().messages.all { it.isRead })
    }

    private fun conversationShown(): ConversationItem {
        val template = screen.onGetTemplate() as ListTemplate
        return template.singleList!!.items.single() as ConversationItem
    }
}
