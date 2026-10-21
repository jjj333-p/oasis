package org.example.demo

// halcyon

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import tigase.halcyon.core.Halcyon
import tigase.halcyon.core.builder.createHalcyon
import tigase.halcyon.core.xmpp.JID
import tigase.halcyon.core.xmpp.modules.MessageReceivedEvent
import tigase.halcyon.core.xmpp.modules.muc.MUCModule
import tigase.halcyon.core.xmpp.stanzas.MessageType
import tigase.halcyon.core.xmpp.toBareJID


@Serializable
data class Config(
    val jid: String,
    val pass: String,
    val prejoin: List<String>?
)

data class UIState(
    val openChat: MutableState<Chat?>,
    val chats: MutableList<Chat>,
    val chatsByJID: SnapshotStateMap<String, Chat>
)


class Account(s: UIState) {
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
                    _messages = mutableStateOf(listOf<OasisMessage>()),
                    account = this,
                    replyingTo = mutableStateOf(null)
                )
                this.state.chatsByJID[from] = chat
                this.state.chats.add(chat)
            } else {
                println("chat with $from already exists")
            }
            chat.addMessage(
                OasisMessage("Gay person", it.stanza, chat)
            )
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

        relevantChat.addMessage(
            OasisMessage("you", sentMsg.stanza, relevantChat)
        )
    }
}
