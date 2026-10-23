# Changelog

All notable changes to the BlueMez Offline Bluetooth Messenger will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.1.0] - 2026-10-22

### Added
- **4-Tab Bottom Navigation**: Redesigned primary navigation into **Chats**, **Contacts**, **Transfers**, and **Settings** tabs with persistent state and quick tab switching.
- **Dedicated Contacts Screen**: Comprehensive screen organizing saved BlueMez friends, actively discovered nearby devices, and bonded Bluetooth devices with direct connect/chat actions.
- **Safe Conversation Management**:
  - Top bar overflow menu in `ChatScreen` with options to **Clear Messages**, **Delete Conversation**, and **Remove Contact**.
  - Deletion confirmation dialogs explaining irreversible actions and allowing optional purging of local media.
  - Safe long-press confirmation dialog on conversation items in `HomeScreen`, replacing previous accidental one-tap delete.
- **Appearance & Theme Controls**:
  - Interactive theme selection in **Settings** supporting **Follow System**, **Light Mode**, and **Dark Mode**.
  - Preference persistence via Jetpack DataStore (`theme_mode`).
  - Refined neutral theme palette with modern slate/gray backgrounds (`#F9FAFB` light, `#111827` dark), high-contrast surfaces, and accessible semantic status indicators.
- **Structured Media Storage**:
  - Reorganized app-specific media directory into `files/bluemez/media/{images,videos,other}`, `files/bluemez/transfers/{incoming,outgoing}`, and `files/bluemez/thumbnails`.
  - Storage calculation and one-tap cache wipe in **Settings**.
  - Media exclusion from Android cloud backup in `backup_rules.xml` and `data_extraction_rules.xml` to protect local privacy.
- **CI/CD & Documentation**:
  - GitHub Actions CI workflow (`.github/workflows/android-ci.yml`) for pull requests and main branch validation.
  - GitHub Actions Release workflow (`.github/workflows/android-release.yml`) for automated release builds on `v*` tags with SHA-256 checksums.
  - Comprehensive documentation: `docs/TESTING.md`, `docs/RELEASING.md`, and updated `README.md`.

### Fixed
- **Critical Keyboard & Composer Overlap Bug**:
  - Fixed soft keyboard pushing composer and send button off-screen.
  - Implemented WhatsApp-style pinned composer with `WindowInsets.statusBars` in `Scaffold` and `Modifier.imePadding().navigationBarsPadding()`.
  - Added reactive scroll tracking (`LaunchedEffect(isImeVisible)`) to keep conversation history centered and visible while typing.
- **Edge-to-Edge Navigation**: Fixed navigation bar inset padding to ensure bottom navigation bars and floating buttons render above system navigation gestures.

---

## [1.0.0] - 2026-10-22

### Added
- Native offline Bluetooth Classic RFCOMM transport over standard UUID `6e400001-b5a3-f393-e0a9-e50e24dcca9e`.
- Device discovery, Bluetooth hosting server, and automated connection management.
- Real-time text messaging with instant delivery ACKs.
- Chunked media transfer engine for photos and videos with SHA-256 validation.
- Offline outgoing message queuing with automatic delivery on reconnect.
- Room database for conversation, message, friend, and transfer persistence.
- Zero-cloud, 100% offline security architecture with zero network permissions.
