# TagMind

Native Android NFC item reminder app built with Kotlin, Jetpack Compose, Navigation 3, Room, and AlarmManager. Android 8.0 (API 26) or later; NFC hardware is required for tag testing.

## Resume development

Open this folder in Android Studio. Its bundled JDK runs Gradle; the default Java on this machine is Java 8 and cannot run this build. The build also uses a Java 17 toolchain, which Gradle resolves automatically. The Android SDK path is in `local.properties`.

From PowerShell in this folder:

```powershell
.\dev.ps1
```

Build, run unit tests, install, and launch on a connected Android device:

```powershell
.\dev.ps1 -Install
```

Enable USB debugging and connect one device first. The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

Additional checks (after setting JAVA_HOME to a supported JDK):

```powershell
.\gradlew.bat :app:compileDebugAndroidTestKotlin :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

The second command requires a device or emulator. A physical NFC phone and tags are needed to verify scans, notification sound, alarm timing, and reboot recovery.

## Current checkpoint

- Implemented: dark home screen, tag registration with icons, Room persistence, reminder presets, item details and scan history, alarm notifications, and pending alarm restoration after reboot.
- Resumed fixes: known tags open item details and confirm active reminders; confirmation cancels their alarm and notification; stale cancelled alarms are ignored; notification taps while the app is open update navigation; NFC dispatch accepts tag extras, handles launch scans, and suppresses duplicate reads for two seconds; item detail ViewModels are keyed per item.
- Replaced deleted starter-code tests with repository registration, reminder replacement, and confirmation tests, plus an Android home-to-scan navigation test.
- No Git repository was present when work resumed.

## Remaining work

1. Add onboarding and runtime notification/exact-alarm permission guidance. Scheduling currently requires these permissions to be granted; callers still need to surface scheduling failures rather than leaving an unscheduled pending reminder.
2. Finish the reminder-fired landing screen with scan prompt and snooze; currently notification taps open item details.
3. Add repeat nudges and settings, including battery optimization guidance.
4. Implement Leaving Check; its route currently displays the home screen as a placeholder.
5. Add custom reminder duration, a one-minute test preset, and confirmation feedback.
6. Cancel previous scheduled alarms when replacing reminders, and add transaction protection for reminder/scan writes.
7. Verify on a physical NFC device, including closed-app alarms and reboot recovery.

The original product plan is `../nfc-item-reminder-plan.pdf`. The existing native Kotlin implementation is retained.

## Verification on 2026-10-15

- Debug APK built successfully.
- Three repository unit tests passed with no failures.
- Android lint passed after replacing Activity casts with `LocalActivity`; advisory warnings remain.
- Android UI test source compiled; execution requires a device or emulator.
- Restart script passed PowerShell syntax validation and resolved the local Android SDK correctly.
- No connected device or configured emulator was available, so installation, launch, and NFC behavior remain unverified.
