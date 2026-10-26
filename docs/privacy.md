# Privacy, permissions, and practical limits

## Stored locally

Rules, selected app package names, correct-answer progress, unused credit, and session deadlines are stored in the app's private preferences. The app does not keep answer histories or capture the TV screen. Android backup and device-transfer backup are disabled.

The parent PIN is stored as a salted PBKDF2-HMAC-SHA256 hash with 120,000 iterations. No PIN is embedded in source. Five failed checks impose a one-minute delay, shared by TV and phone login. This limiter is held in memory and resets if the process restarts; it is not an anti-tampering boundary.

The local HTTPS private key stays in Android Keystore. A self-signed certificate is generated on first use and its SHA-256 fingerprint is shown on the TV. Verify that fingerprint before trusting the certificate. A changed IP requires opening the new address; a changed certificate should be checked again on the TV. The server does not obtain a public certificate or contact a certificate authority.

## Optional phone dashboard

It starts only after a parent enables it and uses a visible foreground-service notification. It accepts IPv4 private-network/loopback clients on port 18443, checks exact Host/Origin values, requires authentication for state and actions, limits request sizes, and expires login tokens after 30 minutes. Tokens stay in the browser tab's sessionStorage. Logging out, changing the PIN, or stopping the server revokes all current phone tokens.

The dashboard supports direct IP URLs on the same private network, not custom hostnames, internet access, VPN routing, or reverse proxies. It should never be port-forwarded. A private IP is not proof that every device on that network is trustworthy.

When enabled, the service attempts to resume after a reboot and when the app next opens. Vendor battery/background restrictions and newer Android foreground-service rules can affect automatic startup. Check the status on your actual TV. Stopping phone management keeps it stopped across reboots.

The app has no cloud service, ads, telemetry, external web fonts, or outbound content downloads. The dashboard HTML/CSS/JavaScript is bundled with the APK.

## Permissions

| Permission / capability | Purpose |
|---|---|
| `INTERNET` | Optional local HTTPS listener; belongs to this companion app, not the launcher |
| `ACCESS_NETWORK_STATE` | Local connectivity context |
| Foreground service / special use | Maintain the parent-enabled local listener with a notification |
| Notifications | Display listener status on supported Android versions |
| Boot completed | Attempt to resume the listener if the parent previously enabled it |
| Accessibility service | Observe foreground package changes and return to the quiz when time expires; optional phone Home/app-launch actions |

The accessibility configuration cannot retrieve window contents and does not request key filtering. It observes package names, not text or typed input. It does not intercept physical volume buttons. A half-second timer check runs only while a selected restricted app is foreground.

## Enforcement limits

The parent must explicitly select apps and enable the service. System settings, Home, and known TV-input/factory packages are excluded from selection, but vendor behavior still needs manual testing. This is an activity-level boundary, not a device-owner or kiosk implementation.

- Disabling accessibility, force-stopping, clearing data, or removing the app defeats enforcement. System settings intentionally remain usable.
- Picture-in-picture, background audio, casting, and vendor overlays may continue playback or obscure which app is foreground. These are not reliably blocked.
- Family Quest does not control HDMI sources, tuner content, other devices, or the whole household network.
- Foreground-service or accessibility behavior may differ by TV firmware. Recheck after an update or reboot.
- The schedule uses the TV's clock. Changing system time can affect the schedule. Session duration uses both elapsed time and wall time; rebooting ends the current session.
- Saved credit is capped at 120 minutes. Starting a session spends the whole bank; stopping early or rebooting does not refund that session. Clearly explain this rule to children.

Use operating-system or account-based parental controls when stronger restrictions are required. See README.md for safe ADB closure after setup.
