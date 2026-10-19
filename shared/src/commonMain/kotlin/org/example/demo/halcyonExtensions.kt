package org.example.demo

import tigase.halcyon.core.requests.RequestBuilder
import tigase.halcyon.core.requests.modifyMessage
import tigase.halcyon.core.xmpp.FullJID
import tigase.halcyon.core.xmpp.JID
import tigase.halcyon.core.xmpp.stanzas.Message

fun RequestBuilder<Unit, Message>.addOOB(url: String, description: String?): RequestBuilder<Unit, Message> {
    return this.modifyMessage {
        val preBodyLen = body?.length ?: 0

        body = (body ?: "") + url

        val postBodyLen = body!!.length

        "x" {
            attributes["xmlns"] = "jabber:x:oob"
            "url" {
                +url
            }
            if (description != null) {
                "desc" {
                    +description
                }
            }
        }

        "fallback" {
            attributes["xmlns"] = "urn:xmpp:fallback:0"
            attributes["for"] = "jabber:x:oob"

            "body" {
                attributes["start"] = preBodyLen.toString()
                attributes["end"] = postBodyLen.toString()
            }
        }
    }
}


fun RequestBuilder<Unit, Message>.setReply(id: String, jid: String, quotedContent: String?): RequestBuilder<Unit, Message> {
    val fallbackTXT = quotedContent?.split("\n")?.joinToString("\n> ", prefix = "> ", postfix = "\n")

    return this.modifyMessage {
        "reply" {
            attributes["xmlns"] = "urn:xmpp:reply:0"
            attributes["to"] = jid
            attributes["id"] = id
        }

        if (fallbackTXT != null) {
            body = fallbackTXT + body

            "fallback" {
                attributes["xmlns"] = "urn:xmpp:fallback:0"
                attributes["for"] = "urn:xmpp:reply:0"

                "body" {
                    attributes["start"] = "0"
                    attributes["end"] = fallbackTXT.length.toString()
                }
            }
        }
    }
}
