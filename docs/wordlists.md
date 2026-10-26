# Question-bank format

The app packages a UTF-8 JSON file at build time. Edit `config/wordlists.json`, or pass `-PfamilyQuestWordlist=/absolute/path/to/file.json` to Gradle. Installing the update with the same signing key preserves the PIN, rules, saved credit, and progress. Restart the app to load a changed bank. The parent dashboard selects levels and rules; it does not upload question files.

There are three stable IDs: `beginner`, `intermediate`, and `advanced`. Each must contain at least one enabled word. Their display names and all questions are configurable; there is no fixed 200-question requirement. The included 20 questions per level are original CC0 examples, not standardized difficulty bands.

```json
{
  "schemaVersion": 1,
  "title": "My vocabulary adventure",
  "license": "CC0-1.0",
  "levels": [
    {
      "id": "beginner",
      "name": "初階",
      "words": [
        {"id": "beginner-001", "english": "apple", "zh": "蘋果"}
      ]
    },
    {
      "id": "intermediate",
      "name": "中階",
      "words": [
        {"id": "intermediate-001", "english": "color", "zh": "顏色", "acceptedAnswers": ["colour"], "hint": "c 開頭"}
      ]
    },
    {
      "id": "advanced",
      "name": "高階",
      "words": [
        {"id": "advanced-001", "english": "take turns", "zh": "輪流", "enabled": true}
      ]
    }
  ]
}
```

| Field | Rule |
|---|---|
| `schemaVersion` | Integer `1` |
| `title` | 1–40 characters; shown on the home screen |
| level `id` | Exactly one of each stable ID above |
| level `name` | 1–20 characters; shown on TV and phone |
| word `id` | Globally unique, nonempty, up to 80 characters |
| `english` | 1–24 characters: A–Z, spaces, apostrophes, hyphens, periods |
| `zh` | 1–64 characters; the prompt shown on screen |
| `acceptedAnswers` | Optional array of alternative spellings, using the same limits as `english` |
| `hint` | Optional string, up to 100 characters. Omit for an automatic first-letter hint; `""` suppresses its content |
| `enabled` | Optional boolean; defaults to `true`. Disabled rows remain in the file but are not asked |
| `license`, `attribution`, `source`, other metadata | Optional documentation only; they do not grant rights or change gameplay |

The whole file must be at most 4 MB. Invalid content stops the Gradle build. Within a level, repeated normalized English spellings are asked once per shuffled round. A round boundary avoids repeating the immediately previous question when there is more than one unique word.

Answers ignore case, surrounding spaces, repeated whitespace, and common straight/curly apostrophe and hyphen differences. `A.M.` and `P.M.` accept omitted periods. Spaces inside phrases otherwise matter. Alternative answers count as correct but feedback displays the primary `english` spelling.

Keep answers short enough to read from the couch. Long prompts and hints should be visually tested on the intended TV. Spellings outside the supported English keyboard need a code change, not an undocumented workaround.

## Private content

Use an external file or the ignored `private/` directory for a personal school list. Do not copy third-party PDFs or wordlist compilations into the repository unless you have permission to redistribute them. Public access is not a license. Imported content keeps its own terms; the CC0 notice covers only the original bundled sample bank.

Optional artwork is supplied with `-PfamilyQuestWallpaper=/absolute/path/to/wallpaper.jpg`. Use a reasonably sized 16:9 JPG, ideally 1920×1080, whose rights you hold. This gets packaged into your local APK only. Do not commit it, its APK, or screenshots containing private material.

For PowerShell, quote each full property argument (including `-P`):

```powershell
./gradlew.bat assembleRelease '-PfamilyQuestWordlist=C:/My Content/wordlists.json' '-PfamilyQuestWallpaper=C:/My Content/wallpaper.jpg'
```
