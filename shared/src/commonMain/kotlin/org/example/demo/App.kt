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