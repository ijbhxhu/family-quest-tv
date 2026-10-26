# Verification checklist

Run all automated checks after changes:

```sh
./gradlew --no-daemon testReleaseUnitTest lintRelease assembleRelease
```

On Windows use `./gradlew.bat`. Unit tests cover answer normalization, credits and session boundaries, opening hours, question-bank validation and shuffle behavior, HTTP framing limits, and origin checks. They do not replace device testing.

## Isolated emulator / test TV

Use a separate emulator and a harmless fixture app for tests that change credit or rules. Never spend or reset a real child's saved minutes to make a test convenient. Install using the existing signing key and `install -r`.

1. Fresh install: no PIN, no selected apps, restrictions off, server off. Set the parent PIN locally. Verify wrong PIN is rejected and repeated failures rate-limit.
2. Quiz: wrong answer → visible correct spelling → automatic next question without credit. Five correct answers at default rules → exactly ten minutes. Repeated Enter/OK during feedback must not award twice.
3. Pause during feedback, wait longer than the transition, resume: advance once. Home/background/resume must preserve earned credit. Check all keyboard edges using Up/Down/Left/Right and a real D-pad.
4. Test all three levels, phrases with spaces, apostrophes, hyphens, periods, custom hints, aliases, and disabled entries. A one-word level must still work.
5. Parent dialogs: background the app while a rule/PIN-change dialog is open, then return. Require login again and ensure the old dialog cannot save. Leaving parent management idle for five minutes must also close its dialogs.
6. Enable restrictions on a fixture. No credit: launching it returns to the quiz. Active credit: app remains open. At expiry: return to quiz, including after a volume overlay. Test opening-time boundaries before and after the configured time.
7. Phone HTTPS: unauthenticated state/actions rejected; correct login accepted; wrong Host/Origin rejected; invalid rewards/configuration rejected; logout and PIN changes revoke tokens. Confirm phone layout on Android and iPhone, including the local-certificate flow.
8. Upgrade: record bank, progress, rules, chosen apps, enabled state, and certificate fingerprint; install the new APK; verify they remain. Do not copy PIN/hash/private keys into logs.
9. Reboot: unused bank remains, running session ends, Home and the real remote still work. Confirm the optional parent listener and accessibility service status; vendor firmware may affect restart behavior.
10. Close debugging and revoke ADB authorization after real-device work. Verify the TV is online while checking the debug port is closed; a sleeping/offline TV is not sufficient evidence.

## Public-content build

Build once with an external custom wordlist/wallpaper, then build with no content properties. Inspect the APK ZIP: default `assets/wordlists.json` must have 60 original sample rows and `assets/wallpaper.jpg` must be absent. This catches stale private assets. Inspect `git ls-files` before publication for keys, photos, local paths, identifiers, and APKs.

Document the actual device, Android version, firmware, and tests performed. Do not describe an emulator check as a physical-remote test, claim compatibility with untested TVs, or call a review a guarantee of perfection.
