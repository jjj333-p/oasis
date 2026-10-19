package org.example.demo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// halcyon
import tigase.halcyon.core.xmpp.toBareJID

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import tigase.halcyon.core.xmpp.stanzas.MessageType
import tigase.halcyon.core.xmpp.toJID

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

var started = false


@Composable
fun App(padding: Dp) {

    val openChat = remember { mutableStateOf<Chat?>(null) }
    val chats: SnapshotStateList<Chat> = remember { mutableStateListOf() }
    val chatsByJID: SnapshotStateMap<String, Chat> = remember { mutableStateMapOf() }
    val accounts = remember { mutableStateListOf<Account>() }

    val state = UIState(
        openChat = openChat,
        chats = chats,
        chatsByJID = chatsByJID,
    )


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
                            .clickable { openChat.value = chat } // Update state on click
                            .padding(16.dp)
                    )
                }
            }
        }

        // RIGHT PANE: Chat View (Messages)
        // weight(1f) tells this box to fill all remaining horizontal space
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            if (openChat.value == null) {
                // Progressive disclosure: show a placeholder until a chat is selected
                Text("Select a chat to start messaging", modifier = Modifier.padding(24.dp))
            } else {
                // "chat" ui
                Column(modifier = Modifier.fillMaxSize()) {
                    var sendBoxContent by remember { mutableStateOf("") }

                    // Header
                    Text(
                        text = "Chatting with ${openChat.value!!.prettyName}",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                    HorizontalDivider()

                    val thismsglistfromremember by remember { openChat.value!!.messages }

                    // Message List
                    // reverseLayout = true forces the list to start from the bottom
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        reverseLayout = true
                    ) {
                        println("redoing message list")

                        // When using reverseLayout, feed the items in reverse order
                        // so the newest message stays at the absolute bottom.
                        val messages = thismsglistfromremember.reversed()

                        // create the ui for each message
                        items(messages.size) { msgNum ->
                            println("working on message $msgNum")
                            val msg = messages.getOrNull(msgNum) ?: return@items

                            // the message ui
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "${msg.displayFrom}: ${msg.underlyingMessage.body}",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).weight(1f)
                                )
                                Button(
                                    onClick = {
                                        openChat.value!!.replyingTo.value = msg
                                        println(openChat.value!!.replyingTo.value)
                                    },
                                    shape = MaterialTheme.shapes.medium,
                                    content = { Text("Reply") },
                                    enabled = true
                                )
                            }
                        }
                    }

                    // little in reply to ui box
                    val replyMsgState = remember { openChat.value!!.replyingTo }
                    val replyMsg = replyMsgState.value
                    if (replyMsg != null) {
                        Text(
                            "In reply to ${replyMsg.displayFrom}:",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                            VerticalDivider(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp)
                            )
                            Text(
                                replyMsg.underlyingMessage.body ?: "",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp).weight(1f)
                            )
                        }
                    }

                    // message send box + button to send
                    Row(modifier = Modifier.fillMaxWidth()) {

                        // helper function so all this bs isnt inline
                        fun sendTheBullshit() {
                            val whatWeWantToSend = sendBoxContent
                            sendBoxContent = ""
                            openChat.value!!.account.sendText(openChat.value!!.jid.toJID(), whatWeWantToSend)
//                            openChat!!.messages.value += OasisMessage("You", msg)
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

                        // send button
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

    // runner to start account
    LaunchedEffect(Unit) {
        if (started) return@LaunchedEffect
        accounts.add(
            Account(
                state
            )
        )
        started = true
    }

}