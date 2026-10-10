# Motor Insurance SIM Calling Test — Phase 1

Android 16 (compile/target API 36), Java 17, AGP 8.10.1, Gradle 8.11.1. Minimum Android 8. No laptop installation is needed: GitHub Actions builds the debug APK.

## Scope
- Start queue initiates ordinary SIM calls, one attempt per unique Indian mobile number per India calendar day (Asia/Kolkata).
- Local, +91, 91, 0091 and leading-zero formats normalize to the same identity.
- Repeated input is deduplicated; previous same-day attempts are skipped.
- Failed/unanswered/cancelled attempts still consume the automatic attempt. No automatic redial.
- Every retry requires Call Again followed by Confirm; the number must have a previous attempt.
- All calls, including retries, wait at least 60 seconds after call end. An active call blocks dispatch.
- The queue runs only while this app is visible. Return from the phone app after each call; the queue resumes after the gap. App/process restart stops the in-memory queue.
- System phone-state events persist end time while the dialer is foreground. If events are missed, returning to the app starts a conservative fresh 60-second gap.
- If SIM selection is shown, choose the SIM in the system dialer. No default-dialer takeover.

This app does NOT capture/inject cellular audio, record calls, use AI APIs, or make AI speak over a normal SIM call. You speak yourself. Dashboard, Sheets, AI telephony and WhatsApp are later phases.

## Download and install
Open Actions → Android Phase-1 APK → latest successful run → Artifacts → MotorInsurance-Phase1-APK. Download/unzip and install app-debug.apk on the SIM-capable Samsung device. This is a debug-signed test build, not a Play Store release. Only enable APK installation for the trusted download source if your device policy permits.

Grant Call and Phone permissions with the app button. Enter only authorized test numbers on the device, one per line. Press Start queue; after each call, end it in the phone app and return. Stop queue cancels future dispatch, but does not end an active SIM call.

## Device acceptance checklist
1. Verify Android 16 installation, permissions granted/denied and real voice-capable SIM support.
2. Enter the same authorized number in different accepted formats: only one automatic attempt.
3. End a call and confirm the next number waits 60 seconds. Confirm no dispatch with app backgrounded.
4. Busy, rejected and unanswered calls must never retry automatically.
5. Same-day restart retains duplicate protection. Clear app data/reinstall removes local history.
6. Call Again → Cancel makes no call. Confirm before cooldown completion stays blocked. Confirm after the gap requests one retry.
7. Restart/kill during a call, return after end: no resumed queue and a conservative cooldown.
8. Check two SIMs, incoming calls, SIM selection cancellation and device reboot.
9. India midnight permits a new automatic attempt. Backward time changes are blocked; use automatic device date/time.

CI checks rule tests, Android lint and APK compilation. Real Samsung/SIM behavior is NOT proven by a cloud build; complete these checks on the device.

## Privacy and limitations
No real numbers, customers, credentials, API keys or secrets belong in this public repository. Test inputs remain in memory; only SHA-256 number/day markers and timing are persisted in private app storage, with backup disabled. Hashes are not encryption; device access should be protected. No network permission, contacts, call-log or microphone access. No number logging, analytics or export. Screenshots are blocked by the app.

Daily protection is per installation/device, not centrally enforced. Clearing storage, reinstalling or deliberately manipulating the device clock can defeat local history. Use automatic date/time. Queue continuation depends on returning to the visible app due to Android background activity restrictions. This is a controlled Phase-1 test, not unattended production dialing.

## Build
The cloud workflow installs SDK 36 and Gradle, then runs:
`gradle --no-daemon testDebugUnitTest lintDebug assembleDebug`
No secrets are required. APK and verification reports are saved as Actions artifacts. Local developers with Android SDK/JDK/Gradle already available can run the same command.
