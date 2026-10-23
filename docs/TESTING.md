# BlueMez Testing Guide

This guide details how to verify the functionality, offline integrity, and reliability of the BlueMez offline Bluetooth messenger.

---

## 1. Automated Testing

BlueMez includes unit tests and Robolectric JVM integration tests for protocol parsing, transfer validation, database operations, and user preferences.

### Running Automated Tests
Run tests directly via Gradle in bash:
```bash
gradle :app:testDebugUnitTest
```

### Key Test Suites
- **`ExampleRobolectricTest.kt`**: Validates Room database persistence, conversations, messages, queued offline message dispatching, and contact management.
- **`ProtocolAndTransferUnitTest.kt`**: Tests serialization/deserialization of framing packets (`HEADER`, `TEXT`, `FILE_START`, `FILE_CHUNK`, `FILE_ACK`), chunk calculation, checksum verification, and state transitions.

---

## 2. Hardware Testing (Two Physical Android Devices)

Because BlueMez uses native Android Bluetooth RFCOMM hardware APIs (`BluetoothAdapter`, `BluetoothServerSocket`, `BluetoothSocket`), physical verification requires two Android devices running Android 8.0 (API 26) or higher.

### Prerequisites
1. Install the BlueMez APK on **Phone A** and **Phone B**.
2. Keep Wi-Fi and Cellular Data turned **OFF** or enable Airplane mode (then toggle Bluetooth ON) to verify 100% offline functionality.
3. Grant nearby devices / Bluetooth and notification permissions when prompted.

---

### Step-by-Step Test Checklist

#### A. Discovery & Pairing
1. On **Phone A**, navigate to the **Contacts** tab and tap **"Start Discovery"** or ensure hosting is enabled.
2. On **Phone B**, tap **"Start Discovery"**.
3. Confirm Phone A appears under **Discovered Devices** on Phone B with its real Bluetooth name and MAC address.
4. Tap **Connect** on Phone B. Accept the system Bluetooth pairing prompt if the devices are not yet bonded.
5. Verify both devices show the green **"Connected"** status badge.

#### B. WhatsApp-Style Keyboard & Composer Verification
1. Open the active conversation on Phone A.
2. Tap inside the message input box to trigger the soft keyboard.
3. **Verify:**
   - The message input bar and Send button stay visible directly above the keyboard (no overlap or clipping).
   - The conversation message list scrolls smoothly to keep the latest messages in view.
   - Enter multiple lines of text (3–4 lines); verify the input box expands gracefully while keeping the Send button accessible.
   - Dismiss the keyboard and ensure layout transitions back seamlessly without flickering or white gaps.

#### C. Real-Time Text Messaging
1. Send a text message from Phone A to Phone B.
2. **Verify on Phone A:** Message bubble shows a single checkmark (sent) and updates to double checkmarks (delivered) upon receiving transport ACK.
3. **Verify on Phone B:** Message appears immediately with accurate timestamp and audio/vibration notification.

#### D. Offline Queuing & Auto-Reconnection
1. Disable Bluetooth on Phone B.
2. On Phone A, observe status change to **"Disconnected"**.
3. Type and send a message on Phone A.
4. **Verify:** Message is saved with a pending/queued status indicator (clock icon) and is persisted in Room SQLite.
5. Re-enable Bluetooth on Phone B and reconnect.
6. **Verify:** The queued message on Phone A is automatically transmitted and marked delivered once the connection is re-established.

#### E. Media Transfer (Photo & Video)
1. Tap the attachment paperclip icon in the composer on Phone A.
2. Select an image using the native Android Photo Picker.
3. Tap Send.
4. **Verify on both phones:** Progress indicator shows transfer percentage and throughput.
5. When finished, open the image on Phone B. Verify it is saved in Phone B's private `bluemez/media/images/` directory and renders sharply.
6. Repeat with a short MP4 video (<20MB).

#### F. Safe Conversation & Contact Management
1. In the chat screen, tap the 3-dot overflow menu in the top app bar:
   - **"Clear Messages"**: Confirms deletion of messages and attachments while keeping the contact and thread.
   - **"Delete Conversation"**: Deletes the chat history with an option to remove associated media files.
   - **"Remove Contact"**: Removes custom contact status while giving the choice to preserve or remove message history.
2. Long-press a conversation row on the **Chats** tab: Confirm the delete dialog warns the user before taking action.

#### G. Theme & Appearance
1. Navigate to the **Settings** tab.
2. Under **Appearance**, switch between **Follow System**, **Light Mode**, and **Dark Mode**.
3. Verify the theme switches immediately without app restart, respecting the refined neutral palette (slate/gray neutral background with high contrast readability).
4. Restart the app; confirm the chosen theme persists from DataStore.
