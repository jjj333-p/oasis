package org.example.demo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable

// halcyon
import tigase.halcyon.core.builder.createHalcyon
import tigase.halcyon.core.xmpp.modules.MessageReceivedEvent
import tigase.halcyon.core.xmpp.modules.muc.MUCModule
import tigase.halcyon.core.xmpp.toBareJID
import kotlinx.serialization.json.Json

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import tigase.halcyon.core.Halcyon
import tigase.halcyon.core.xmpp.stanzas.MessageType

fun readConfigFile(filePath: String): String {
    val path = Path(filePath)

    // Check if file exists
    if (!SystemFileSystem.exists(path)) {
        return "{}" // Return empty JSON or default state
    }

    // Read the entire file into a string
    return SystemFileSystem.source(path).buffered().use { source ->
        source.readString()
    }
}

@Serializable
data class Config(
    val jid: String,
    val pass: String,
    val prejoin: List<String>?
)

data class OasisMessage(
    val fromDisplay: String,
    val body: String,
    val replyToID: String?
)

data class Chat(
    val prettyName: String,
    val jid: String,
    val messages: MutableState<List<OasisMessage>>
)

var halcyon: Halcyon? = null


@Composable
fun App(padding: Dp) {

    var openChat by remember { mutableStateOf<Chat?>(null) }

    val chats: SnapshotStateList<Chat> = remember { mutableStateListOf() }
    val chatsByJID: SnapshotStateMap<String, Chat> = remember { mutableStateMapOf() }

    // The main horizontal split
    Row(modifier = Modifier.fillMaxSize()) {

        // LEFT PANE: Chat List (Sidebar)
        Surface(
            modifier = Modifier.width(250.dp).fillMaxHeight(),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(chats.size) { chatNum ->
                    val chat = chats.getOrNull(chatNum) ?: return@items
                    Text(
                        text = chat.prettyName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openChat = chat } // Update state on click
                            .padding(16.dp)
                    )
                }
            }
        }

        // RIGHT PANE: Chat View (Messages)
        // weight(1f) tells this box to fill all remaining horizontal space
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            if (openChat == null) {
                // Progressive disclosure: show a placeholder until a chat is selected
                Text("Select a chat to start messaging", modifier = Modifier.padding(24.dp))
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    var sendBoxContent by remember { mutableStateOf("") }

                    // Header
                    Text(
                        text = "Chatting with ${openChat!!.prettyName}",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp)
                    )

                    HorizontalDivider()
                    val thismsglistfromremember by remember { openChat!!.messages }

                    // Message List
                    // reverseLayout = true forces the list to start from the bottom
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        reverseLayout = true
                    ) {
                        println("redoing message list")

                        val messages = thismsglistfromremember.reversed()
                        // When using reverseLayout, feed the items in reverse order
                        // so the newest message stays at the absolute bottom.
                        items(messages.size) { msgNum ->
                            println("working on message $msgNum")
                            val msg = messages.getOrNull(msgNum) ?: return@items
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "${msg.fromDisplay}: ${msg.body}",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).weight(1f)
                                )
                                Button(
                                    onClick = {
                                        sendBoxContent =
                                            msg.body.split('\n').joinToString(
                                                separator = "\n> ", prefix = "> ", postfix = "\n"
                                            ) + sendBoxContent
                                    },
                                    shape = MaterialTheme.shapes.medium,
                                    content = { Text("Reply") },
                                    enabled = msg.replyToID != null
                                )
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {


                        fun sendTheBullshit() {
                            val whatWeWantToSend = sendBoxContent
                            sendBoxContent = ""
                            halcyon!!.request.message {
                                to = openChat!!.jid.toBareJID()
                                type = MessageType.Chat
                                body = whatWeWantToSend
                            }.send()
                            openChat!!.messages.value += OasisMessage("You", whatWeWantToSend, null)
                        }

                        // Input Area (Text field + Send button would go here)
                        OutlinedTextField(
                            value = sendBoxContent,
                            onValueChange = {
                                sendBoxContent = it
                            },
                            modifier = Modifier
                                .weight(1f)
//                                .fillMaxWidth()
                                .padding(16.dp)
                                .onKeyEvent {
                                    if (
                                        it.type == KeyEventType.KeyUp &&
                                        it.key == Key.Enter &&
                                        (it.isShiftPressed || it.isCtrlPressed)
                                    ) {
                                        sendTheBullshit()
                                        return@onKeyEvent true
                                    }
                                    return@onKeyEvent false
                                },
                            placeholder = { Text("Type a message...") }
                        )

                        Button(
                            onClick = { sendTheBullshit() },
                            shape = MaterialTheme.shapes.medium,
                            content = { Text("Send message") },
                            enabled = sendBoxContent != ""
                        )
                    }
                }

            }
        }
    }

    LaunchedEffect(Unit) {
        val config = Json.decodeFromString<Config>(readConfigFile("/home/joseph/Documents/xmpp-bot/demo/config.json"))

//        val botScope = CoroutineScope(Dispatchers.Default)

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
            var chat = chatsByJID[from]
            if (chat == null) {
                println("chat is null")
                chat = Chat(
                    prettyName = from,
                    jid = from,
                    messages = mutableStateOf(listOf(OasisMessage("Gay Person", body, id)))
//                    messages = mutableListOf(OasisMessage(from, body))
                )
                chatsByJID[from] = chat
                chats.add(chat)
            } else {
                // TODO: this doesnt seem to be working
                println("chat with $from already exists")
                chat.messages.value += OasisMessage("Gay person", body, id)
            }
//            chatWindow.add("from:$from, body:$body")
        }

        halcyon!!.connect()
    }
}