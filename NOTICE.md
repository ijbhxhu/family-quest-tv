# Source and asset provenance

Family Quest TV is an independent companion app developed for a TV using [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv). The launcher itself is not bundled or modified here. Module-card integration follows the upstream contract in [docs/modules.md](https://github.com/Gathin23/cleanyourtv/blob/main/docs/modules.md).

The initial Android/Gradle project scaffolding and module-card conventions were adapted from cleanyourtv. The upstream checkout used during this work was commit `f592b6d2db0cfa0e28779660f25ff65386a5ff9b`, following v3.0.0. Its MIT copyright and license are retained in [licenses/cleanyourtv-MIT.txt](licenses/cleanyourtv-MIT.txt) and the root license. The upstream license's Outfit font note describes an upstream file; Outfit is not distributed in this project.

Quiz logic, time accounting, local parent management, and block-world drawing are maintained in this repository. This is not an official cleanyourtv or Roblox release, and neither project is responsible for this companion app.

The 60 sample word-prompt pairs in `config/wordlists.json` were composed for this project and dedicated under CC0 1.0. No third-party school PDF, extracted full school wordlist, private picture, screenshot, device identifier, or signing key is distributed.

Noto Sans TC is distributed unmodified from Google Fonts. Its copyright/OFL notice and exact source URLs, byte counts, and SHA-256 hashes are in `licenses/`. The full font supports custom Traditional Chinese prompts.

The Gradle wrapper scripts and wrapper JAR are Gradle-generated tooling, licensed under Apache 2.0. JUnit 4 (EPL 1.0), Hamcrest (BSD), and JSON-java (public domain) are test-only dependencies downloaded by Gradle; they are not embedded in the APK. Android framework classes are provided by the device.
