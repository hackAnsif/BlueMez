# BlueMez Release Process

This document outlines the steps to build, sign, and publish production releases of BlueMez.

---

## 1. Version Bumping

Before cutting a release, update the version identifiers in `app/build.gradle.kts`:

```kotlin
defaultConfig {
    versionCode = 2          // Increment integer for each release
    versionName = "1.1.0"    // Semantic versioning (MAJOR.MINOR.PATCH)
}
```

Update `CHANGELOG.md` with release notes detailing new features, fixes, and architectural enhancements.

---

## 2. Release Signing Configuration

BlueMez supports reproducible release builds via Gradle signing configs.

### Environment Variables
Configure these variables in your CI/CD runner (e.g. GitHub Repository Secrets):
- `KEYSTORE_PATH`: Path to the `.jks` or `.keystore` file.
- `STORE_PASSWORD`: Keystore store password.
- `KEY_ALIAS`: Signing key alias (defaults to `upload`).
- `KEY_PASSWORD`: Key password.

### GitHub Actions Secrets
In your GitHub repository under **Settings > Secrets and variables > Actions**, add:
- `KEYSTORE_BASE64`: Base64-encoded contents of your release `.keystore` file (`base64 -w 0 upload.keystore`).
- `STORE_PASSWORD`: Password for the keystore.
- `KEY_PASSWORD`: Password for the key.

---

## 3. Automated Tag-Based Releases

The GitHub Actions workflow `.github/workflows/android-release.yml` triggers on Git tags:

```bash
# 1. Ensure all changes are committed on main
git checkout main
git pull origin main

# 2. Tag the release
git tag -a v1.1.0 -m "Release v1.1.0: Keyboard layout fix, 4-tab bottom navigation, safe conversation management, refined neutral themes"

# 3. Push the tag to trigger release workflow
git push origin v1.1.0
```

The workflow will:
1. Restore or generate required debug and release keystores automatically (using `KEYSTORE_BASE64` secret if provided, or self-signing a release key so CI builds never fail).
2. Run JVM and Robolectric unit tests (`gradle :app:testDebugUnitTest`).
3. Build the optimized release APK (`gradle :app:assembleRelease`).
4. Compute SHA-256 checksums for release artifacts.
5. Upload the release APK and checksums as GitHub Actions run artifacts (`bluemez-release-apk`).
6. Publish a GitHub Release with the APK and checksum file attached when triggered by a version tag or manual dispatch with a tag.

### Manual Workflow Dispatch (Web UI)
You can also trigger a release build directly from GitHub:
1. Navigate to **Actions** > **Android Release**.
2. Click **Run workflow**.
3. Optionally enter a tag name (e.g. `v1.1.0`).
4. Download the built APK from Artifacts or GitHub Releases!

---

## 4. Manual Local Release Build

To build and sign an APK locally:

```bash
# If using custom keystore
export KEYSTORE_PATH="/path/to/my-release-key.jks"
export STORE_PASSWORD="your_store_password"
export KEY_PASSWORD="your_key_password"

gradle :app:assembleRelease

# Release outputs are generated at:
# app/build/outputs/apk/release/app-release.apk
```
