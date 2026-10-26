package tw.familyquest.tv;

import java.text.Normalizer;
import java.util.Locale;

/** Pure rules; every time value is in milliseconds. */
public final class Rules {
    public static final long MAX_BANK = 120 * 60_000L;
    private Rules() {}
    public static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC).trim().toLowerCase(Locale.ROOT)
                .replace('\u2019', '\'').replace('\u2018', '\'')
                .replace('\u2011', '-').replace('\u2010', '-').replaceAll("\\s+", " ");
    }
    public static boolean correct(String expected, String actual) {
        String want = normalize(expected), got = normalize(actual);
        // Common clock abbreviations accept optional periods.
        if (want.equals("a.m.") || want.equals("p.m.")) return want.replace(".", "").equals(got.replace(".", ""));
        return !got.isEmpty() && want.equals(got);
    }
    public static long remaining(long endElapsed, long endWall, int sessionBoot,
                                 long nowElapsed, long nowWall, int currentBoot) {
        if (sessionBoot != currentBoot || endElapsed <= 0 || endWall <= 0) return 0;
        return Math.max(0, Math.min(endElapsed - nowElapsed, endWall - nowWall));
    }
    public static long addReward(long bank, int minutes) {
        if (minutes < 1 || minutes > 60) throw new IllegalArgumentException("Minutes must be 1–60");
        return Math.min(MAX_BANK, Math.max(0, bank) + minutes * 60_000L);
    }
    public static void validate(int questions, int minutes, String level) {
        if (questions < 1 || questions > 50 || minutes < 1 || minutes > 60
                || !(level.equals("beginner") || level.equals("intermediate") || level.equals("advanced")))
            throw new IllegalArgumentException("題數須為 1–50，獎勵須為 1–60 分鐘，請選擇有效級別。");
    }
    public static int openingMinutes(String opening) {
        if (opening == null || !opening.matches("(?:[01][0-9]|2[0-3]):[0-5][0-9]"))
            throw new IllegalArgumentException("開放時間請使用 00:00–23:59。");
        return Integer.parseInt(opening.substring(0,2)) * 60 + Integer.parseInt(opening.substring(3));
    }
    public static boolean isOpen(String opening, int minuteOfDay) {
        return minuteOfDay >= openingMinutes(opening) && minuteOfDay < 1440;
    }
}
