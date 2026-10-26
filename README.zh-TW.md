# Family Quest TV

用單字挑戰累積電視使用時間。適用 Android TV 的獨立 App，提供方塊世界介面、連續出題，以及 Android／iPhone 瀏覽器家長管理頁。

[English / 完整建置步驟](README.md) · [交給 AI 的操作提示](PROMPT.md) · [題庫設定](docs/wordlists.md)

## 功能

- 遙控器或英文鍵盤拼字；答對自動進下一題，答錯先顯示正確拼法，不必再按「下一題」。按返回可暫停。
- 預設答對 5 題存入 10 分鐘，每天 16:00 後開放。題數、分鐘、時間、級別與受限 App 都由家長調整。
- 初、中、高階題庫使用 JSON 設定，公開版附 60 題原創 CC0 範例。可設定中文、英文、提示、其他接受拼法與是否啟用；不是經學校認證的完整教材。
- 按「使用已存時間」才開始倒數；待機與離開 App 仍計時，重新開機會結束當次倒數，未使用的存款會保留。最多存 120 分鐘。
- 同一區網的手機可設定規則、給獎勵、結束倒數、回 Home、調音量與開啟 App。沒有雲端帳號、廣告或遙測。
- 初次安裝不啟用限制、不開網路伺服器，也沒有共用預設 PIN。家長需親自設定。

## 讓自己的 AI 協助

Clone 專案後，請 AI 讀取 `AGENTS.md`、`README.md` 與 `PROMPT.md`。先編譯、測試，再針對你指定的 TV serial 安裝。PIN 直接在電視設定，不用交給 AI。

需要 JDK 17、Android SDK 34 與 build-tools 34.0.0。Windows 執行：

```powershell
./gradlew.bat --no-daemon testReleaseUnitTest lintRelease assembleRelease
adb -s <TV_SERIAL> install -r app/build/outputs/apk/release/app-release.apk
```

安裝後開啟「家長管理」建立 PIN、選擇受限 App，再到系統設定開啟 Family Quest 無障礙服務。手機管理網址以電視畫面為準；使用自簽 HTTPS 憑證，比對電視顯示的 SHA-256 指紋後，才接受該網址的憑證。網路位址變更時需使用新網址。

設定完成後，關閉 USB／網路／無線偵錯，撤銷 ADB 授權，重開機並檢查偵錯連接埠已關閉。日常手機管理不需要 ADB。

## 可自訂的內容

直接改 `config/wordlists.json` 就能換教材；也可用 `-PfamilyQuestWordlist=<外部 JSON 路徑>` 保持私人題庫不進 repo。用 `-PfamilyQuestWallpaper=<外部 JPG 路徑>` 加入自己的背景；公開版沒有家人照片或私人圖片，預設背景由程式繪製。

安裝更新時使用相同的本機簽章金鑰與 `install -r`，即可保留 PIN、累積時間與家長設定。不同電腦產生的 debug key 可能不同；不要為了安裝失敗就清除資料。

## 限制與授權

這是需要家人配合的時間管理工具。停用無障礙、強制停止或清除 App 資料會讓限制失效；無法保證控制子母畫面、投放及背景音訊。請先閱讀 [隱私與限制](docs/privacy.md)。

程式採 MIT，保留 cleanyourtv 的來源與授權；公開範例題庫採 CC0，Noto Sans TC 採 SIL OFL 1.1。第三方私人教材與圖片沒有包含在公開版，也不會自動套用這些授權。與 Roblox 無官方關聯。
