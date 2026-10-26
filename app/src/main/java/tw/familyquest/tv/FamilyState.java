package tw.familyquest.tv;

import android.content.*;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Base64;
import java.security.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.*;

public final class FamilyState {
    private static FamilyState singleton;
    public final Context context;
    private final SharedPreferences prefs;
    private final SecureRandom random = new SecureRandom();
    private int failedPins;
    private long retryAfter;
    private final Map<String,Long> tokens = new HashMap<>();
    public final WordBank words;
    private FamilyState(Context context) {
        this.context = context.getApplicationContext();
        prefs = this.context.getSharedPreferences("family", Context.MODE_PRIVATE);
        try { words = new WordBank(this.context); } catch (Exception e) { throw new IllegalStateException("題庫無法載入", e); }
    }
    public static synchronized FamilyState get(Context c) {
        if (singleton == null) singleton = new FamilyState(c);
        return singleton;
    }
    public int questions() { return prefs.getInt("questions", 5); }
    public int minutes() { return prefs.getInt("minutes", 10); }
    public String level() { return prefs.getString("level", "beginner"); }
    public String startTime() { return prefs.getString("startTime", "16:00"); }
    public boolean isOpen() { Calendar now = Calendar.getInstance(); return Rules.isOpen(startTime(), now.get(Calendar.HOUR_OF_DAY)*60+now.get(Calendar.MINUTE)); }
    public int progress() { return prefs.getInt("progress", 0); }
    public long bank() { return prefs.getLong("bank", 0); }
    public boolean enabled() { return prefs.getBoolean("enabled", false); }
    public boolean serverEnabled() { return prefs.getBoolean("serverEnabled", false); }
    public void serverEnabled(boolean enabled) { prefs.edit().putBoolean("serverEnabled", enabled).commit(); }
    // Parents explicitly select apps during setup; existing choices survive updates.
    public Set<String> restricted() { return new HashSet<>(prefs.getStringSet("apps", Collections.emptySet())); }
    private int boot() { return Settings.Global.getInt(context.getContentResolver(), Settings.Global.BOOT_COUNT, -1); }
    public synchronized long remaining() {
        return Rules.remaining(prefs.getLong("endElapsed", 0), prefs.getLong("endWall", 0), prefs.getInt("boot", -2),
                SystemClock.elapsedRealtime(), System.currentTimeMillis(), boot());
    }
    public synchronized boolean earnCorrect() {
        if (!isOpen()) throw new IllegalArgumentException("每天 " + startTime() + " 後開放答題與使用時間。");
        int progress = progress() + 1;
        boolean earned = progress >= questions();
        prefs.edit().putInt("progress", earned ? 0 : progress)
                .putLong("bank", earned ? Rules.addReward(bank(), minutes()) : bank()).commit();
        return earned;
    }
    public synchronized void startSession() {
        if (!isOpen()) throw new IllegalArgumentException("每天 " + startTime() + " 後才能開始計時。");
        long credit = bank();
        if (credit <= 0) throw new IllegalArgumentException("先完成單字挑戰，才能開始計時。");
        if (remaining() > 0) throw new IllegalArgumentException("目前已有倒數中的時間，儲存分鐘可留到下次使用。");
        prefs.edit().putLong("endElapsed", SystemClock.elapsedRealtime() + credit)
                .putLong("endWall", System.currentTimeMillis() + credit).putInt("boot", boot()).putLong("bank", 0).commit();
    }
    public synchronized void stopSession() { prefs.edit().remove("endElapsed").remove("endWall").commit(); }
    public synchronized void bonus(int minutes) { prefs.edit().putLong("bank", Rules.addReward(bank(), minutes)).commit(); }
    public synchronized void configure(int questions, int minutes, String level, String startTime, Set<String> selected, boolean enabled) {
        Rules.validate(questions, minutes, level);
        Rules.openingMinutes(startTime);
        Set<String> allowed = AppCatalog.packages(context);
        if (!allowed.containsAll(selected)) throw new IllegalArgumentException("部分 App 不可限制或已不存在。");
        if (enabled && (!hasPin() || selected.isEmpty() || GateService.instance == null))
            throw new IllegalArgumentException("請先設定家長 PIN、選擇 App，並在電視開啟 Family Quest 無障礙服務。");
        SharedPreferences.Editor e = prefs.edit().putInt("questions", questions).putInt("minutes", minutes)
                .putString("level", level).putString("startTime", startTime).putStringSet("apps", new HashSet<>(selected)).putBoolean("enabled", enabled);
        if (questions != questions() || !level.equals(level())) e.putInt("progress", 0);
        e.commit();
        if (GateService.instance != null) GateService.instance.recheck();
    }
    public boolean hasPin() { return prefs.contains("pinHash"); }
    public synchronized void setPin(String pin) {
        if (!pin.matches("[0-9]{4,12}")) throw new IllegalArgumentException("PIN 請使用 4–12 位數字。");
        byte[] salt = new byte[16]; random.nextBytes(salt);
        prefs.edit().putString("pinSalt", Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString("pinHash", Base64.encodeToString(hash(pin, salt), Base64.NO_WRAP)).commit();
        failedPins = 0; retryAfter = 0; tokens.clear();
    }
    private byte[] hash(String pin, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, 120_000, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (GeneralSecurityException e) { throw new IllegalStateException(e); }
        finally { spec.clearPassword(); }
    }
    public synchronized boolean verifyPin(String pin) {
        if (SystemClock.elapsedRealtime() < retryAfter || !hasPin() || pin.length() > 12) return false;
        byte[] want = Base64.decode(prefs.getString("pinHash", ""), Base64.DEFAULT);
        byte[] salt = Base64.decode(prefs.getString("pinSalt", ""), Base64.DEFAULT);
        boolean ok = MessageDigest.isEqual(want, hash(pin, salt));
        if (ok) failedPins = 0;
        else if (++failedPins >= 5) { retryAfter = SystemClock.elapsedRealtime() + 60_000; failedPins = 0; }
        return ok;
    }
    public synchronized String login(String pin) {
        if (!verifyPin(pin)) throw new IllegalArgumentException("PIN 不正確，或嘗試過多；請稍後再試。");
        tokens.entrySet().removeIf(e -> e.getValue() <= SystemClock.elapsedRealtime());
        if (tokens.size() >= 4) tokens.clear();
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String token = Base64.encodeToString(bytes, Base64.NO_WRAP | Base64.URL_SAFE | Base64.NO_PADDING);
        tokens.put(token, SystemClock.elapsedRealtime() + 30 * 60_000L); return token;
    }
    public synchronized boolean authenticated(String token) { return tokens.getOrDefault(token, 0L) > SystemClock.elapsedRealtime(); }
    public synchronized void revokeTokens() { tokens.clear(); }
}
