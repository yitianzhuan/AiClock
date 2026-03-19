# Repository Guidelines

## Project Structure & Module Organization
`AiClock` is a single-module Android app. Root Gradle files live in `/`, including [`build.gradle.kts`](/Users/coolwind/AiProject/AiClock/build.gradle.kts), [`settings.gradle.kts`](/Users/coolwind/AiProject/AiClock/settings.gradle.kts), and the wrapper scripts. App code is under [`app/src/main/java/com/aiclock/smartalarm`](/Users/coolwind/AiProject/AiClock/app/src/main/java/com/aiclock/smartalarm): `model/` for data classes, `data/` for persistence, `alarm/` for scheduling and receivers, and `ui/` for activities and adapters. Layouts and strings are in [`app/src/main/res`](/Users/coolwind/AiProject/AiClock/app/src/main/res). Product notes live in [`docs/PRD.md`](/Users/coolwind/AiProject/AiClock/docs/PRD.md).

## Build, Test, and Development Commands
Run all commands from the repository root.

- `./gradlew assembleDebug` builds the debug APK.
- `./gradlew installDebug` installs the debug build on a connected device or emulator.
- `./gradlew lint` runs Android lint checks.
- `./gradlew test` runs JVM unit tests if `app/src/test` is added.
- `./gradlew connectedAndroidTest` runs device tests if `app/src/androidTest` is added.

Before building, ensure `local.properties` points to a valid Android SDK.

## Coding Style & Naming Conventions
Use Kotlin with 4-space indentation and standard Android/Kotlin style. Keep classes and activities in `PascalCase`, methods and properties in `camelCase`, and resource files in `snake_case` such as `activity_main.xml` or `item_alarm.xml`. Match the existing package namespace `com.aiclock.smartalarm`. Prefer small, focused classes aligned with current folders: UI logic in `ui`, alarm orchestration in `alarm`, and storage logic in `data`.

## Testing Guidelines
There are currently no committed test sources. Add JVM tests under `app/src/test` and instrumentation tests under `app/src/androidTest`. Name test files after the target class, for example `AlarmSchedulerTest.kt`. For behavioral changes around scheduling, snooze, boot restore, or persistence, include at least one automated test or document why coverage is not practical.

## Commit & Pull Request Guidelines
Git history currently uses Conventional Commit style, for example `feat: initialize AiClock Android app with smart alarm logic`. Continue with prefixes like `feat:`, `fix:`, and `docs:`. Keep each commit scoped to one change. PRs should include a short summary, testing performed, related issue or requirement, and screenshots or recordings for UI changes.

## Security & Configuration Tips
Do not commit `local.properties`, signing configs, or machine-specific SDK paths. Exact alarm and notification behavior depend on runtime permissions, so verify changes on Android 12+ and Android 13+ devices when touching alarm or notification flows.
