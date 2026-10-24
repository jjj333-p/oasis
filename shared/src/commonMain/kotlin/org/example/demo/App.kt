package org.example.demo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString

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

    val openChatState = remember { mutableStateOf<Chat?>(null) }
    val chats: SnapshotStateList<Chat> = remember { mutableStateListOf() }
    val chatsByJID: SnapshotStateMap<String, Chat> = remember { mutableStateMapOf() }
    val accounts = remember { mutableStateListOf<Account>() }
    var showJoinDialogue by remember { mutableStateOf(false) }

    val state = UIState(
        openChat = openChatState,
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

                // 1. A single item for the top button
                item {
                    Button(
                        onClick = { showJoinDialogue = true },
                        shape = MaterialTheme.shapes.medium,
                        content = { Text("Send message") },
                        enabled = true,
                        // Added some padding so it sits nicely in the sidebar
                        modifier = Modifier.padding(16.dp).fillMaxWidth()
                    )
                }

                // 2. Iterate directly over the chats list
                // (Make sure to import androidx.compose.foundation.lazy.items)
                items(chats.size) { chatNum ->
                    val chat = chats.getOrNull(chatNum) ?: return@items
                    Text(
                        text = chat.prettyName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openChatState.value = chat } // Update state on click
                            .padding(16.dp)
                    )
                }
            }
        }

        // RIGHT PANE: Chat View (Messages)
        // weight(1f) tells this box to fill all remaining horizontal space
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            val openChat = openChatState.value
            if (openChat == null) {
                // Progressive disclosure: show a placeholder until a chat is selected
                Text("Select a chat to start messaging", modifier = Modifier.padding(24.dp))
            } else {
                openChat.Display()
            }
        }
    }


    if (showJoinDialogue) Dialog(onDismissRequest = { println("dismissed") }) {
        Surface(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Enter your name")
                OutlinedTextField(
                    value = "sendBoxContent",
                    onValueChange = {
//                        sendBoxContent = it
                    },
                    modifier = Modifier
                        .weight(1f)
//                                .fillMaxWidth()
                        .padding(16.dp),
                    placeholder = { Text("Type a message...") }
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = { showJoinDialogue = false }) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { }) { Text("OK") }
                }
            }
        }
    }


    // runner to start account
    LaunchedEffect(Unit) {
        accounts.clear()
        accounts.add(
            Account(
                state
            )
        )
    }

}