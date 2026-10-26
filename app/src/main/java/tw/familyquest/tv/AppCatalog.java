package tw.familyquest.tv;

import android.content.*;
import android.content.pm.*;
import java.util.*;

public final class AppCatalog {
    public static final class App {
        public final String pkg, label;
        App(String pkg, String label) { this.pkg = pkg; this.label = label; }
    }
    public static List<App> list(Context context) {
        PackageManager pm = context.getPackageManager();
        Set<String> excluded = new HashSet<>(Arrays.asList(context.getPackageName(), "com.android.tv.settings", "com.android.settings",
                "com.google.android.tv.settings", "com.mediatek.tv.tvinput", "com.mediatek.wwtv.tvcenter", "com.android.vending"));
        for (String action : new String[]{Intent.ACTION_MAIN, "android.settings.SETTINGS"}) {
            Intent intent = new Intent(action); if (Intent.ACTION_MAIN.equals(action)) intent.addCategory(Intent.CATEGORY_HOME);
            for (ResolveInfo r : pm.queryIntentActivities(intent, 0)) excluded.add(r.activityInfo.packageName);
        }
        List<App> result = new ArrayList<>(); Set<String> seen = new HashSet<>();
        for (ResolveInfo r : pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0)) {
            String pkg = r.activityInfo.packageName;
            if (!excluded.contains(pkg) && !pkg.startsWith("com.cvte.") && !pkg.contains("factory") && !pkg.contains("tvinput")
                    && !pkg.contains("settings") && seen.add(pkg)) result.add(new App(pkg, r.loadLabel(pm).toString()));
        }
        result.sort(Comparator.comparing(a -> a.label)); return result;
    }
    public static Set<String> packages(Context c) { Set<String> out = new HashSet<>(); for (App a : list(c)) out.add(a.pkg); return out; }
    public static void launch(Context c, String pkg) {
        if (!packages(c).contains(pkg)) throw new IllegalArgumentException("App 不可開啟。");
        FamilyState state = FamilyState.get(c);
        if (state.enabled() && state.restricted().contains(pkg) && (!state.isOpen() || state.remaining() <= 0))
            throw new IllegalArgumentException("每天 " + state.startTime() + " 後完成單字，並按開始計時。");
        Intent intent = c.getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
        if (intent == null) throw new IllegalArgumentException("找不到 App。");
        c.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
}
