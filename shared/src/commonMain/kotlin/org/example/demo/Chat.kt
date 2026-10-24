package org.example.demo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import tigase.halcyon.core.xmpp.toJID

data class Chat(
    val account: Account,
    val prettyName: String,
    val jid: String,
    val _messages: MutableState<List<OasisMessage>>,
    val replyingTo: MutableState<OasisMessage?>
) {
    private val messagesByID = mutableMapOf<String, OasisMessage>()
    private val thisChat = this

    fun addMessage(message: OasisMessage) {
        _messages.value += message
        val mID = message.underlyingMessage.id ?: return
        messagesByID[mID] = message
    }

    fun getMessagesList() = _messages
    fun getMessageByID(mID: String) = messagesByID[mID]

    @Composable
    fun Display() {
        // "chat" ui
        Column(modifier = Modifier.fillMaxSize()) {
            var sendBoxContent by remember { mutableStateOf("") }

            // Header
            Text(
                text = thisChat.prettyName,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )
            HorizontalDivider()

            val msgList by remember { thisChat._messages }

            // Message List
            // reverseLayout = true forces the list to start from the bottom
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                reverseLayout = true
            ) {
                println("redoing message list")

                // When using reverseLayout, feed the items in reverse order
                // so the newest message stays at the absolute bottom.
                val messages = msgList.reversed()

                // create the ui for each message
                items(messages.size) { msgNum ->
                    println("working on message $msgNum")
                    val msg = messages.getOrNull(msgNum) ?: return@items

                    msg.Display(false)
                }
            }

            // little in reply to ui box
            val replyMsgState = remember { thisChat.replyingTo }
            val replyMsg = replyMsgState.value
            if (replyMsg != null) {
                Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    // this is ass but works for now
                    Button(
                        onClick = {
                            thisChat.replyingTo.value = null
                        },
                        shape = MaterialTheme.shapes.medium,
                        content = { Text("❌") },
                        enabled = true
                    )
                    VerticalDivider(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp)
                    )
                    replyMsg.Display(true)
                }
            }

            // message send box + button to send
            Row(modifier = Modifier.fillMaxWidth()) {

                // helper function so all this bs isnt inline
                fun sendTheBullshit() {
                    val whatWeWantToSend = sendBoxContent
                    sendBoxContent = ""
                    thisChat.account.sendText(thisChat.jid.toJID(), whatWeWantToSend)
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