# Instructions for coding and setup assistants

Read README.md, docs/privacy.md, and the relevant format/test document before changes. This repository contains an independent Android TV companion app. Do not apply its network permissions or services to cleanyourtv's launcher.

## TV setup

- Target the user's verified TV with `adb -s <serial>` on every command. Do not operate another connected device.
- Build from this source, run checks, then install with `install -r`. Preserve existing data and signing key. Never clear data or uninstall to resolve a signature mismatch without explicit authorization for data loss.
- No root, flashing, factory/test activities, package disabling, Home takeover, app-store verification bypass, or lowered minimum SDK. Stop if the TV refuses the sideload or is below API 26.
- The parent enters the PIN locally. Do not log/store it, hardcode defaults, or publish it. Enable restrictions only after the parent chooses apps and enables the accessibility service.
- Do not overwrite real earned credit for tests. Use an isolated emulator and fixture app for destructive time-accounting tests; record and preserve real-device settings.
- Real remote presses are required to verify vendor remote keys. Injected keys do not prove them.
- Close ADB after setup: debugging off, authorizations revoked, reboot, verify ports closed. The HTTPS dashboard is separate and optional.

## Implementation

- Keep the app native Java with no runtime library dependencies. UI is currently Traditional Chinese. Use the bundled font and procedural artwork, not private family images or official game assets.
- Question bank schema: docs/wordlists.md. Maintain 3 level IDs, configurable labels, accepted answers, hints, enabled flags, strict build validation, and tests. Never infer reuse rights from a public download link.
- Preserve time credit and preferences across app upgrades. Authentication, local-network restrictions, and timer boundaries need meaningful regression tests.
- Run `./gradlew --no-daemon testReleaseUnitTest lintRelease assembleRelease` (Windows: `./gradlew.bat`). Check TV focus, continuous questions, wrong-answer feedback, duplicate submissions, pause/resume, and parent access using docs/testing.md.
- Keep work focused. No network telemetry, cloud account, remote ADB listener, or hidden service. Do not claim kiosk-grade enforcement or universal TV compatibility.

## Sharing

- Do not commit APKs, signing keys, ADB keys, local.properties, private wordlists/photos, screenshots of family content, IPs, SSIDs, emails, or device identifiers. Use `<TV_SERIAL>` and `<tv-ip>` placeholders.
- Preserve MIT, upstream attribution, Google Fonts OFL, Gradle Apache notices, and CC0 question-bank notice. Source code licensing does not relicense imported material.
- Before requested publication, inspect the exact staged file list and scan for secrets/private assets. A repository setup request alone does not authorize publishing it. Prefer an independent code/security review for substantial changes.
