# BlueMez — Offline Bluetooth Messenger for Android

**BlueMez** is a 100% offline, peer-to-peer messaging application for Android devices. It allows two nearby phones to exchange real-time text messages, emojis, Unicode (including Malayalam), photos, and short videos over native **Bluetooth Classic RFCOMM**, completely free from internet access, mobile data, Wi-Fi, servers, or cloud dependencies.

---

## Key Architecture & Features

1. **Pure Offline Operation**:
   - Zero internet permission requested.
   - Zero telemetry, third-party analytics, accounts, or cloud database storage.
   - Private conversation database stored locally via **Room**.
   - Cloud backup explicitly disabled in backup rules to safeguard personal data.

2. **Bluetooth Classic RFCOMM Protocol**:
   - Custom service UUID: `6e400001-b5a3-f393-e0a9-e50e24dcca9e`.
   - Binary framing with 4-byte magic signature `BMEZ`, protocol versioning, frame typing, and length validation.
   - Stream reassembly with bounded memory buffers to prevent memory exhaustion.
   - Bi-directional keepalive ping/pong heartbeat.

3. **Message Reliability & Queuing**:
   - Outgoing messages created while disconnected are persisted in Room with state `PENDING`.
   - When a connection to that friend is restored, the queue is automatically flushed and transmitted.
   - Delivery acknowledgement (`MsgDeliveredAck`) transitions status from `SENDING` to `DELIVERED`.
   - Optional read receipts (`MsgReadReceipt`) transition status to `READ`.
   - Full support for Unicode, Malayalam scripts, emojis, and multiline text.

4. **Streaming Media Transfers**:
   - Streamed in 32 KB bounded chunks with sequence indexes and byte offsets.
   - Real-time progress percentage and byte transfer tracking.
   - Received file saved first to a `.tmp` file in private app storage.
   - Integrity verified using SHA-256 before finalizing the file.
   - Safe cancellation and temp file cleanup.
   - Secure filename sanitization against path-traversal attacks.
   - Export to user storage via Android Storage Access Framework (SAF).

5. **Friend Management**:
   - Assign and persist custom friend names (e.g. "Adil", "College Friend").
   - View recent conversations, unread counters, and last message previews.
   - Real-time connection indicators (Green = Connected, Amber = Reconnecting, Red = Disconnected).

---

## Two Physical Devices Testing Guide

Follow this checklist to test BlueMez on two physical Android phones:

### Prerequisites:
- 2 physical Android phones running Android 7.0 (API 24) or higher (Android 12/13/14+ supported).
- BlueMez installed on both devices.
- Bluetooth enabled in Android settings on both devices.

### Step-by-Step Two-Device Connection Procedure:
1. **Onboarding & Display Name**:
   - Launch BlueMez on Phone A and Phone B.
   - Enter a display name on each phone (e.g., "Phone A" and "Phone B").
   - Tap **Grant Bluetooth Permissions** and approve the system dialogs (`BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE`).
   - Tap **Get Started**.

2. **Starting Host / Receive Mode**:
   - On **Phone A**: Tap the **Host / Listen** button on the Home screen.
   - The status updates to **Hosting (Active)**. Phone A is now listening for incoming RFCOMM connections from BlueMez.

3. **Scanning & Connecting**:
   - On **Phone B**: Tap **Scan Nearby** (or the floating action button).
   - Phone B performs live Bluetooth discovery.
   - When Phone A appears in the list, tap **Connect** next to Phone A.
   - Phone B initiates the RFCOMM socket connection and performs the binary handshake.
   - Both devices immediately transition to **Connected**!

4. **Assigning a Custom Friend Name**:
   - In the Chat screen, tap the friend's name in the top bar.
   - Enter a personalized name (e.g. "Adil"). Tap **Save Name**.
   - The custom name is saved in your local Room database.

5. **Exchanging Real-Time Text, Malayalam & Emojis**:
   - Type a message with emojis and Malayalam text (e.g., `ഹലോ സുഹൃത്തേ! 🚀`).
   - Tap **Send**.
   - Notice the status icons:
     - Sending (spinner) -> Delivered (two checks).
   - When the other phone reads the message, it updates to blue checkmarks (Read receipt).

6. **Transferring Images & Videos**:
   - Tap the paperclip attachment icon on the chat bar.
   - Select a photo or short video from the Android Photo Picker.
   - The app computes the SHA-256 hash, sends a `TransferRequest`, and streams the file in 32 KB chunks.
   - Watch the live progress percentage on both devices.
   - When complete, the SHA-256 checksum is verified and the image/video appears in the chat bubble.
   - Tap the media bubble to open the **Media Viewer**, play videos, or tap **Export to Storage**.

7. **Testing Offline Queuing & Reconnection**:
   - Turn OFF Bluetooth or walk out of range.
   - The status changes to **Disconnected / Offline**.
   - Send a message while disconnected. It displays a clock icon (**Pending (Offline)**).
   - Turn Bluetooth back ON and tap **Host / Listen** / reconnect.
   - The queued message is automatically flushed and delivered!

---

## Running the Automated Test Suite

Run the unit tests via Gradle:
```bash
gradle :app:testDebugUnitTest
```

This verifies:
- Protocol binary frame serialization and deserialization (`FrameEncoder`, `FrameParser`).
- Stream reassembly with preceding garbage and partial packets (`FrameReader`).
- Oversized frame rejection and protection against denial of service.
- Filename sanitization against path traversal (`../../../`).
- SHA-256 integrity calculation.
- Room database operations, conversation tracking, and pending message retrieval (`ExampleRobolectricTest`).
