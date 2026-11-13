# ASTRA

**Package:** `com.spider.astra`  
**Platform:** Native Android (tablet-first)  
**Primary device:** Redmi Pad Pro 5G · Android 16 · 8 GB RAM

ASTRA is a personal Android assistant concept inspired by JARVIS / FRIDAY / EDITH. The planned experience is a persistent floating orb with conversational AI and carefully permissioned device actions.

## Current milestone: Phase 1 — Application Foundation

<details>
<summary>Phase 1 delivery status</summary>

- [x] Android Gradle project and CI build pipeline
- [x] Compose app theme and animated, state-aware orb
- [x] Dashboard with command input and working quick prompts
- [x] Reactive mock conversation and message history
- [x] Task lifecycle and recent-activity panel
- [x] Dashboard / Chat / Settings navigation
- [x] DataStore preferences for display name, theme mode and voice preference
- [x] Debug APK built and uploaded by GitHub Actions
- [ ] Install on the Redmi Pad Pro and complete on-device visual / interaction smoke test

**Important:** CI proves the source compiles and packages. It does not prove the APK has been installed or visually tested on the target tablet yet.

</details>

### What works in this build

- **Dashboard:** animated orb, current assistant state, command console, quick prompts and recent activity.
- **Chat:** reactive message history, send controls, busy state and clear mock-mode disclosure.
- **Settings:** saved display name, System / Dark / Light theme selection, and a saved voice preference flag.
- **Mock assistant:** simulates Thinking → Planning → Executing → Completed and records a task.

All assistant replies in Phase 1 come from `MockAssistantRepository`. No real AI provider is connected and no device action is executed. The voice preference is stored only; microphone and speech features are not implemented yet.

## Build configuration

| Component | Version |
|---|---|
| Android Gradle Plugin | 8.5.0 |
| Gradle wrapper distribution | 8.7 |
| Kotlin | 1.9.24 |
| Compose Compiler | 1.5.14 |
| Compose BOM | 2024.06.00 |
| compileSdk / targetSdk | 35 / 35 |
| minSdk | 24 |
| JDK in CI | 21 |

The CI workflow generates the missing Gradle wrapper JAR on the GitHub-hosted runner, validates it, builds the debug APK and uploads the artifact.

## Download the debug APK

1. Open the [latest Android CI run](https://github.com/spider-man4u/Astra/actions/workflows/android-ci.yml).
2. Open the newest successful run on `master`.
3. In **Artifacts**, download `astra-debug-apk`.
4. Extract the ZIP and install `app-debug.apk` on the tablet.

Artifacts are retained for 30 days.

## Local build

Requirements: JDK 17 or 21, Android SDK platform 35, Build Tools 35.0.0, and Gradle 8.7.

This repository currently generates `gradle-wrapper.jar` in CI rather than committing the binary. On a machine with Gradle 8.7 installed, run:

```bash
gradle :wrapper --gradle-version 8.7 --distribution-type bin
./gradlew :app:assembleDebug
```

APK output:

```
app/build/outputs/apk/debug/app-debug.apk
```

## Planned phases

| Phase | Scope |
|---|---|
| 2 | Floating bubble, overlay permission flow and foreground service |
| 3 | Background task handling and safe action execution |
| 4 | Speech recognition and text-to-speech |
| 5 | Secure connection to the existing Python backend in Termux |
| 6 | Device integrations, permission gates and allowlisted actions |

The existing Termux Python backend is separate from this Android project and must not be overwritten during app development.

## Security boundaries

- No provider API keys or credentials are embedded in the APK.
- No arbitrary shell execution, silent screen capture, messaging or payments.
- Sensitive future actions must use an allowlist and explicit confirmation.
- The UI must report mock / offline state honestly rather than fabricate success.
