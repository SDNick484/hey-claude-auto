package io.github.sdnick484.heyclaude.spike.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.messaging.model.CarMessage
import androidx.car.app.messaging.model.ConversationCallback
import androidx.car.app.messaging.model.ConversationItem
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.CarText
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Template
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import io.github.sdnick484.heyclaude.spike.Conversation
import io.github.sdnick484.heyclaude.spike.EventLog
import io.github.sdnick484.heyclaude.spike.R
import io.github.sdnick484.heyclaude.spike.Turns

/**
 * The car screen: a list with one conversation in it.
 *
 * The library refuses a conversation with no messages ("Message list cannot be
 * empty"), so the conversation always carries at least the seed message.
 */
class ClaudeScreen(carContext: CarContext) : Screen(carContext) {
    private val onConversationChanged: () -> Unit = { invalidate() }

    private val callback = object : ConversationCallback {
        override fun onMarkAsRead() {
            Turns.onMarkedRead(via = "car screen (ConversationCallback.onMarkAsRead)")
        }

        override fun onTextReply(replyText: String) {
            Turns.onUserSaid(
                carContext,
                replyText,
                via = "car screen (ConversationCallback.onTextReply)",
            )
        }
    }

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                EventLog.add("CAR SCREEN created: Android Auto opened the templated app")
                Conversation.addListener(onConversationChanged)
            }

            override fun onDestroy(owner: LifecycleOwner) {
                Conversation.removeListener(onConversationChanged)
                EventLog.add("CAR SCREEN destroyed")
            }
        })
    }

    override fun onGetTemplate(): Template {
        val me = Person.Builder().setName("Me").setKey("me").build()
        val claude = Person.Builder().setName(Conversation.TITLE).setKey(Conversation.ID).build()

        // The docs recommend sending no more than the five most recent messages.
        val messages = Conversation.snapshot().takeLast(5).map { message ->
            CarMessage.Builder()
                .setSender(if (message.fromClaude) claude else me)
                .setBody(CarText.create(message.text))
                .setReceivedTimeEpochMillis(message.timeMillis)
                .setRead(message.read)
                .build()
        }

        val conversation = ConversationItem.Builder(
            Conversation.ID,
            CarText.create(Conversation.TITLE),
            me,
            messages,
            callback,
        )
            .setIcon(
                CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_bubble))
                    .build(),
            )
            .setGroupConversation(false)
            .build()

        return ListTemplate.Builder()
            .setHeader(
                Header.Builder()
                    .setTitle(Conversation.TITLE)
                    .setStartHeaderAction(Action.APP_ICON)
                    .build(),
            )
            .setSingleList(ItemList.Builder().addItem(conversation).build())
            .build()
    }
}
