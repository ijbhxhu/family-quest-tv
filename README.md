# Family Quest TV

Turn English spelling practice into earned TV time. A small, native Android TV app with a block-world interface, continuous questions, and a local parent dashboard for Android and iPhone browsers.

[繁體中文](README.zh-TW.md) · [AI setup prompt](PROMPT.md) · [Question bank format](docs/wordlists.md) · [Privacy and limitations](docs/privacy.md)

## What it does

- Answer with the TV remote or an English keyboard. Correct answers advance automatically; mistakes show the spelling before the next question.
- Earn a configurable number of minutes after a configurable number of correct answers. Defaults: **5 answers → 10 minutes**, available after **16:00** in the TV's local time.
- Save up to 120 minutes. Starting a session consumes saved minutes; the timer continues while another app is open or the TV is asleep. Rebooting ends the current session but preserves unused saved minutes.
- Parents select which apps require time. App restrictions and the network dashboard are **off until explicitly configured**. There is no default PIN.
- Manage rules, rewards, selected apps, volume, and Home from a browser on the same private network. No cloud account, analytics, ads, or third-party web assets.
- Use as a standalone app or a module card in [cleanyourtv](https://github.com/Gathin23/cleanyourtv). It does not replace Home or modify the launcher.

The interface is currently Traditional Chinese, with English answers. The included **60 original sample prompts** cover three example levels; they are a starting set, not a certified school curriculum. Replace them with your own JSON without editing Java.

**This is a cooperative family time tool, not a tamper-proof parental-control system.** Disabling its accessibility service, force-stopping it, or clearing app data bypasses restrictions. Picture-in-picture, casting, and background audio are not reliably controlled. See [limitations](docs/privacy.md).

## Build from source

Requirements: JDK 17, Android SDK Platform 34, Build Tools 34.0.0, and Android Platform Tools. Android Studio can install these. Set `JAVA_HOME` and `ANDROID_HOME` for your machine, or configure the SDK in your local `local.properties`. Minimum device version: Android 8.0 / API 26.

```sh
git clone https://github.com/ijbhxhu/family-quest-tv.git
cd family-quest-tv
./gradlew --no-daemon testReleaseUnitTest lintRelease assembleRelease
```

On Windows, use `./gradlew.bat` in PowerShell. The first build needs internet to download Gradle and dependencies. The app itself runs offline; only the optional parent dashboard uses your local network.

Output: `app/build/outputs/apk/release/app-release.apk`. Builds use your **local Android debug signing key** for personal sideloading; no signing key or prebuilt APK is distributed. Keep the same local key for future updates. Do not clear app data to work around a signing mismatch: recover the original key or knowingly migrate first. For distributing your own builds, configure your own release signing outside version control.

The full Noto Sans TC font is bundled for custom Chinese prompts, so this independent app is several megabytes. The upstream launcher's APK size limit applies to the launcher, which this project does not modify.

## Install and set up

1. Enable ADB temporarily using your TV vendor's procedure. Pair/authorize your own computer. Identify the TV with `adb devices -l`; use its explicit serial for every command.
2. Install your locally built APK: `adb -s <TV_SERIAL> install -r app/build/outputs/apk/release/app-release.apk`.
3. Open **Family Quest** from the TV's Apps screen. In **家長管理**, create a private parent PIN. Do not put it in source code, screenshots, or AI chat logs.
4. Open **設定題數、分鐘與受限 App** to choose rules and apps. Enable the **Family Quest** accessibility service in Android settings, then enable **App 使用限制**. Leave Home, system settings, TV inputs, the keyboard, and remote services alone.
5. Optional: choose **啟動手機管理頁**. On the same private network, open the exact `https://<tv-ip>:18443` address displayed on the TV. The certificate is generated on your TV; verify its displayed SHA-256 fingerprint before accepting that specific certificate warning. Never disable browser security globally or expose this port to the internet.
6. Test real remote buttons, a zero-credit block, a timed session, and a reboot. After setup, disable USB/network/wireless debugging, revoke ADB authorizations, reboot, and verify the debug port is closed. The parent dashboard does not need ADB.

If using cleanyourtv, follow its [TV safety procedure](https://github.com/Gathin23/cleanyourtv/blob/main/PROMPT.md) separately. No debloating, launcher takeover, root, or factory-menu access is needed for this app.

## Customize

Edit [`config/wordlists.json`](config/wordlists.json), or build with a file outside the repository:

```sh
./gradlew assembleRelease -PfamilyQuestWordlist=/absolute/path/to/your-wordlists.json
```

An optional private JPG wallpaper can be supplied with `-PfamilyQuestWallpaper=/absolute/path/to/wallpaper.jpg`. With no wallpaper property, the background is drawn entirely in code. **No family photos or private artwork are included.** The content-copy task removes stale custom assets when returning to a default build. See [the format and examples](docs/wordlists.md).

In PowerShell, quote each entire Gradle property argument, including the `-P` prefix:

```powershell
./gradlew.bat assembleRelease '-PfamilyQuestWordlist=C:/My Content/wordlists.json' '-PfamilyQuestWallpaper=C:/My Content/wallpaper.jpg'
```

## Verification and contributions

Run `testReleaseUnitTest lintRelease assembleRelease` after changes. Review [the manual test plan](docs/testing.md) for TV navigation, credit accounting, parent authentication, and real-device checks. Review [AGENTS.md](AGENTS.md) before using an AI coding assistant.

Personal-device validation covered Sharp AQUOS-TVW21A, Android TV 14, firmware UKNV.260302.001, for the initial time restrictions, dashboard, and Home integration. The redesigned continuous quiz is also exercised on an Android 15 emulator. This does not establish compatibility with every TV or vendor's background-service policies.

Please keep changes focused, include reproduction steps for bugs, and exclude personal information from logs and screenshots. Do not publish security-sensitive details in an issue; see [SECURITY.md](SECURITY.md).

## Licenses and credits

- Code: [MIT](LICENSE), with upstream notices preserved. Built alongside [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv); see [NOTICE.md](NOTICE.md).
- Original sample question bank: [CC0 1.0](config/LICENSE). Private third-party school wordlists are not included or relicensed.
- [Google Fonts Noto Sans TC](https://fonts.google.com/noto/specimen/Noto+Sans+TC): [SIL Open Font License 1.1](licenses/NotoSansTC-OFL.txt), bundled unmodified with source and checksums.
- No official Roblox characters, logos, assets, or affiliation. The default block-world artwork is rendered by app code.
