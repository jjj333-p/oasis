package org.example.demo

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.unit.dp
import tigase.halcyon.core.xmpp.stanzas.Message

const val ReplyNS = "urn:xmpp:reply:0"

data class fallbackRange(
    val forNS: String,
    val start: Int,
    val end: Int
)


class OasisMessage(
    val displayFrom: String,
    val underlyingMessage: Message,
    val chat: Chat
) {

    val thisMessage = this
    var repliedMessage: OasisMessage? = null

    val cleanBody: String

    init {
        val replyElement = underlyingMessage.getChildrenNS("reply", ReplyNS)
        val replyJID = replyElement?.attributes?.getValue("to")
        val replyID = replyElement?.attributes?.getValue("id")

        val supportedFallbacks = mutableListOf<String>()

        if (replyJID != null && replyID != null) {
            supportedFallbacks.add(ReplyNS)
            repliedMessage = chat.getMessageByID(replyID)
        }

        val bodyChars = underlyingMessage.body?.toCharArray()

        cleanBody = //underlyingMessage.body ?: ""
            if (bodyChars != null) {
                val fallbacks = underlyingMessage.getChildrenNS("urn:xmpp:fallback:0").filter {
                    val rightName = it.name == "fallback"
                    if (!rightName) return@filter false

                    val forNS = it.attributes["for"] ?: return@filter false
                    if (!supportedFallbacks.contains(forNS)) return@filter false

                    val forBody = it.getFirstChild("body") ?: return@filter false

                    val end = forBody.attributes["end"]?.toIntOrNull() ?: return@filter false
                    val start = forBody.attributes["start"]?.toIntOrNull() ?: return@filter false

                    return@filter end > start
                }.map {
                    val forBody = it.getFirstChild("body")!!

                    fallbackRange(
                        it.attributes["for"]!!,
                        forBody.attributes["start"]!!.toInt(),
                        forBody.attributes["end"]!!.toInt()
                    )
                }
                if (fallbacks.isNotEmpty()) {
                    bodyChars.filterIndexed { index, ch ->
                        val remove = fallbacks.any {
                            it.start <= index && it.end > index
                        }
                        println("char=$ch; remove=$remove")
                        !remove
                    }.toCharArray().concatToString()
                } else {
                    underlyingMessage.body!!
                }
            } else {
                ""
            }

    }

    @Composable
    fun Display(nested: Boolean) {
        println("Displaying, nested = $nested")

        val replyToDisplay = thisMessage.repliedMessage

        // the message ui
        Column(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Text(
                text = displayFrom,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).weight(1f)
            )

            if (!nested && replyToDisplay != null) {
                Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    VerticalDivider(
                        color = Red,
                        thickness = 2.dp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp)
                    )
                    replyToDisplay.Display(true)
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
//                text = "${displayFrom}: ${underlyingMessage.body}",
                    text = cleanBody,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).weight(1f)
                )
                if (!nested)
                    Button(
                        onClick = {
                            chat.replyingTo.value = thisMessage
                            println(chat.replyingTo.value)
                        },
                        shape = MaterialTheme.shapes.medium,
                        content = { Text("Reply") },
                        enabled = true
                    )
            }
        }

    }
}