# Planning Doc

I like to sometimes ramble to an LLM as a rubber duck, and then get it to summarize into notes for me to come back to later. Look away if you dislike LLM slop. 

----

## WebXDC & KMP UI Architecture

* **Avoid embedded Chromium:** It inflates binary size and memory.
* **Desktop:** Run a headless local HTTP server (Ktor) to serve WebXDC files, then open them in a system webview or default browser.
* **Mobile (iOS/Android):** Use native request interception (`WKURLSchemeHandler` for iOS, `WebViewAssetLoader` for Android) within an embedded webview. Do not use local servers on mobile due to strict lifecycle freezing.
* **WebXDC Security (Swiss Cheese Model):**
1. Embed the WebXDC app inside an `<iframe sandbox="allow-scripts allow-same-origin">` injected into your native webview wrapper.
2. Enforce a strict Content Security Policy (`connect-src 'none'`) in the HTTP/Interception response headers.
3. Have your native Kotlin interceptor explicitly block any network requests not destined for your local app domain.



## Background Wake & Push Notifications

* **XEP-0357 (Push Tickles):** Never send actual stanza data via APNs/FCM (avoids 4KB limits, out-of-order delivery, and breaking OMEMO ratchets). Send an empty "tickle" to wake the device.
* **iOS Wake Lifecycle (Do Not Lurk):** Upon wake -> Connect via TLS 1.3 -> Resume Stream (XEP-0198) -> Parse Delta -> Ack Server -> Fire Local OS Notification -> Close Socket explicitly. Do this in < 2 seconds to avoid OS battery penalties.
* **The Signal Fallback:** If XEP-0198 resumption fails or times out during a background wake, do *not* run a heavy MAM query. Immediately fire a generic local notification ("You may have new messages") and let the app sleep. Execute the heavy MAM sync only when the user foregrounds the app.

## Database & Sync Optimization

* **SQLite Transactions:** Never write individual messages directly. Wrap all MAM and history inserts in explicit transactions (e.g., batching 50–100 messages into a single commit) to avoid massive `fsync()` delays.
* **SQLite Config:** Always ensure SQLDelight/SQLite is running with `PRAGMA journal_mode=WAL;` and `PRAGMA synchronous=NORMAL;`.
* **Media Handling:** Do not download images (XEP-0363) in the background. Save the URL to the DB and fetch it on foreground. If the server throws a 404 because retention expired, render a "Media Expired" tombstone UI.

## Desktop Lifecycle & MUC UX

* **MUC Opt-In:** Prompt users on MUC join for notification preferences (All, Mentions, Muted). Proactively avoid sending presence to muted MUCs to drastically cut background XML chatter.
* **Desktop Sleep Hooks:** Intercept OS sleep events to send `<presence type='unavailable'/>` and cleanly close the TCP socket to prevent "ghost" online states.
* *macOS:* `NSWorkspace.willSleepNotification`
* *Windows:* `WM_POWERBROADCAST` (`PBT_APMSUSPEND`)
* *Linux:* `logind` D-Bus `PrepareForSleep` signal.


* **Desktop App Quit:** Minimize to tray for Windows/Linux, but for macOS explicit quits (`Cmd+Q`), implement APNs/UnifiedPush to wake the background service on demand.
