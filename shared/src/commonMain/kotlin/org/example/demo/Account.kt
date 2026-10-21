package org.example.demo

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import kotlinx.serialization.Serializable

// halcyon
import tigase.halcyon.core.builder.createHalcyon
import tigase.halcyon.core.xmpp.modules.MessageReceivedEvent
import tigase.halcyon.core.xmpp.modules.muc.MUCModule
import tigase.halcyon.core.xmpp.toBareJID
import kotlinx.serialization.json.Json

import tigase.halcyon.core.Halcyon
import tigase.halcyon.core.requests.modifyMessage
import tigase.halcyon.core.xmpp.JID
import tigase.halcyon.core.xmpp.stanzas.Message
import tigase.halcyon.core.xmpp.stanzas.MessageType
import kotlin.collections.plus


@Serializable
data class Config(
    val jid: String,
    val pass: String,
    val prejoin: List<String>?
)

data class OasisMessage(
    val displayFrom: String,
    val underlyingMessage: Message
)

data class Chat(
    val account: Account,
    val prettyName: String,
    val jid: String,
    val messages: MutableState<List<OasisMessage>>,
    val replyingTo: MutableState<OasisMessage?>
)

data class UIState(
    val openChat: MutableState<Chat?>,
    val chats: MutableList<Chat>,
    val chatsByJID: SnapshotStateMap<String, Chat>
)


class Account constructor(s: UIState) {
    val state = s
    var halcyon: Halcyon? = null

    //        val botScope = CoroutineScope(Dispatchers.Default)
    init {
        val config = Json.decodeFromString<Config>(readConfigFile("/home/joseph/Documents/xmpp-bot/demo/config.json"))

        halcyon = createHalcyon {
            install(MUCModule)
            auth {
                userJID = config.jid.toBareJID()
                password { config.pass }
            }
        }

        halcyon!!.eventBus.register(MessageReceivedEvent) {
            val from = it.stanza.from!!.toString()
            val body = it.stanza.body ?: return@register
            val id = it.stanza.id ?: return@register
            var chat = this.state.chatsByJID[from]
            if (chat == null) {
                println("chat is null")
                chat = Chat(
                    prettyName = from,
                    jid = from,
                    messages = mutableStateOf(listOf(OasisMessage("Gay Person", it.stanza))),
                    account = this,
                    replyingTo = mutableStateOf(null)
//                    messages = mutableListOf(OasisMessage(from, body))
                )
                this.state.chatsByJID[from] = chat
                this.state.chats.add(chat)
            } else {
                // TODO: this doesnt seem to be working
                println("chat with $from already exists")
                chat.messages.value += OasisMessage("Gay person", it.stanza)
            }
//            chatWindow.add("from:$from, body:$body")
        }

        halcyon!!.connect()
    }

    fun sendText(t: JID, text: String) {

        val openChat = this.state.openChat.value

        val replyTo = openChat?.replyingTo?.value
        println("reply $replyTo")
        this.state.openChat.value?.replyingTo?.value = null

        val msg = halcyon!!.request.message {
            to = t
            type = MessageType.Chat // TODO
            body = text
        }

        if (replyTo != null) {
            msg.setReply(
                replyTo.underlyingMessage.id!!,
                replyTo.underlyingMessage.from?.toString() ?: replyTo.underlyingMessage.to?.toString() ?: "",
                replyTo.underlyingMessage.body
            )
        }

        val sentMsg = msg.send()

        val relevantChat = openChat ?: this.state.chatsByJID[sentMsg.jid.toString()]
                ?: throw RuntimeException("Cannot find a relevant chat to add to history")

        relevantChat.messages.value += OasisMessage("you",sentMsg.stanza)
    }
}
