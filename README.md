# ASTRA

**Project codename:** ASTRA
**Owner:** Spider
**Package:** `com.spider.astra`
**Platform:** Android (tablet-first, responsive)
**Primary device:** Redmi Pad Pro 5G — Snapdragon 7s Gen 2, 8GB RAM, Android 16

> "An intelligent companion that lives inside your device."

ASTRA is a native Android personal AI assistant inspired by JARVIS/FRIDAY/EDITH —
not a generic chatbot. Its core identity is a **floating AI bubble** that stays
above other apps while ASTRA works in the background.

---

## Milestone status (Phase 0 → Phase 1)

This repository currently contains **Phase 1 — Application Foundation**:

- Valid Android Gradle project (Kotlin, Gradle 8.14, AGP 8.5.0, JDK 21)
- Jetpack Compose dashboard (dark futuristic theme)
- Animated central ASTRA orb with per-state visuals (IDLE/LISTENING/THINKING/
  PLANNING/EXECUTING/WAITING_FOR_PERMISSION/COMPLETED/ERROR)
- Text command input with send
- Mock conversation (AssistantRepository + MockAssistantRepository) — **no real
  backend is claimed to be connected**
- DataStore-backed user preferences
- Navigation host (Dashboard / Chat / Settings stub)
- Overlay-permission settings entry point (Phase 2 bubble permission)

**Not yet built:** the floating overlay bubble (Phase 2), voice (Phase 4), and the
Python-backend integration (Phase 5) are planned future milestones.

---

## Build toolchain blocker (read first)

This environment (OpenCode Mobile on Android, Ubuntu 24.04 aarch64) had **no JDK,
no Gradle, and no Android SDK** installed. During Phase 0 I installed:

| Tool | Version | Source |
|------|---------|--------|
| OpenJDK JDK | 21.0.13 | `apt` (`openjdk-21-jdk-headless`) |
| Android SDK cmdline-tools | 11076708 | `dl.google.com` |
| SDK Platform | android-35 (API 35) | `sdkmanager` |
| SDK Build-Tools | 35.0.0 | `sdkmanager` |
| SDK Platform-Tools | 37.0.1 | `sdkmanager` |
| Gradle | 8.14 | download in progress (see below) |

**Gradle download is blocked/slow.** The official Gradle distribution hosts
(`services.gradle.org`, `downloads.gradle.org`) return ~30 bytes/s (effectively
blocked) from this network; `gradle.org` sustains only ~1–7 KB/s with frequent
stalls. The ~130 MB `gradle-8.14-bin.zip` therefore cannot be fetched reliably
within this session.

**To build, you need Gradle 8.14.** Options:

1. **Transfer the project** to a machine with internet + Android Studio (or a
   working `gradle` 8.x) and run `./gradlew :app:assembleDebug` (or
   `gradle :app:assembleDebug`).
2. **Retry the download** from this environment when connectivity to
   `gradle.org`/`services.gradle.org` improves:
   ```
   cd /opt/android
   wget -c -t 0 https://gradle.org/distributions/gradle-8.14-bin.zip
   ```
   (A background `wget` with infinite retries was started during development;
   check `/opt/android/gradle-8.14-bin.zip`.)

Once Gradle 8.14 is available:

```bash
export ANDROID_HOME=/opt/android
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-arm64
export PATH=/opt/android/gradle-8.14/bin:$PATH   # after extracting the zip
cd /root/projects/ASTRA

# Debug build
gradle :app:assembleDebug

# Install on a connected device (adb from $ANDROID_HOME/platform-tools)
$ANDROID_HOME/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The APK will appear at `app/build/outputs/apk/debug/app-debug.apk`.

## Automatic builds with GitHub Actions

A CI workflow lives at `.github/workflows/android-ci.yml`. On every push to `master` it:

1. Checks out the repo
2. Sets up JDK 21
3. Runs `./gradlew :app:assembleDebug`
4. Uploads the debug APK as a workflow artifact (30-day retention)

You can download the APK from the GitHub Actions run page → "Artifacts" → `astra-debug-apk`, then install on the Redmi Pad Pro with `adb install -r app-debug.apk`.

**Prerequisite before the first workflow run:** generate the Gradle wrapper on a machine that has Gradle 8.14:
```
gradle wrapper --gradle-version 8.14
```
This creates `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.jar` (the last two are required for `./gradlew` to work). Commit and push them, then GitHub Actions builds automatically.

---

## Project structure

```
ASTRA/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/
│       │   ├── values/colors.xml, themes.xml, strings.xml
│       │   ├── drawable/
│       │   └── layout/
│       └── java/com/spider/astra/
│           ├── MainActivity.kt
│           ├── navigation/AstraNavHost.kt
│           ├── ui/
│           │   ├── theme/AstraTheme.kt
│           │   ├── components/AstraOrb.kt
│           │   ├── dashboard/DashboardScreen.kt
│           │   ├── chat/ChatScreen.kt
│           │   └── settings/SettingsScreen.kt
│           ├── assistant/
│           │   ├── core/AssistantRepository.kt
│           │   ├── core/MockAssistantRepository.kt
│           │   ├── models/AssistantMessage.kt
│           │   ├── models/AssistantResponse.kt
│           │   └── state/AssistantTask.kt
│           ├── permissions/
│           ├── data/settings/AstraSettings.kt
│           └── integration/   (backend/android — future phases)
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
├── PROGRESS.md
└── README.md
```

---

## Architecture notes

- **MVVM-ish separation**: `MainActivity` only wires the repository, settings, and
  navigation host. Business logic lives in `assistant/core/`; UI lives in `ui/`.
- **Mock backend**: `MockAssistantRepository` simulates a
  thinking → planning → executing → completed cycle. It never fabricates device
  capabilities and never claims a real backend connection.
- **Permissions**: declared only `INTERNET` (for the future backend integration).
  The `SYSTEM_ALERT_WINDOW`/overlay permission is **not** declared in the manifest;
  it is requested at runtime via `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` in
  Phase 2.
- **No secrets**: no API keys, Gemini/Groq credentials, or tokens are stored in
  the APK.

---

## Backend integration (Phase 5, future)

The owner already has a Python ASTRA backend running in Termux
(`astra.py`, `hands.py`, `senses.py`, SQLite memory). That backend is **not**
accessed by this Phase 1 app and must not be overwritten.

The integration interface (`AssistantRepository` / `AssistantApi`) is designed so
a later implementation can connect to the Python backend through a **local,
authenticated HTTP API** (or another local IPC). Exact integration steps are to
be documented when the backend endpoint and auth mechanism are confirmed.

**Do not hardcode API keys.** Do not put Gemini/Groq credentials in the APK.

---

## Safety rules (observed)

- Permissions requested only when needed, with explanation.
- No silent screen capture, no silent messaging, no payments, no arbitrary
  shell execution without validation.
- Root-powered actions (future) will use a restricted allowlist + explicit
  confirmation for dangerous operations.
- Audit trail for sensitive actions.
- Honest error reporting: no fake success messages.
