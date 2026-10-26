package tw.familyquest.tv;

import android.accessibilityservice.AccessibilityService;
import android.content.*;
import android.os.*;
import android.view.accessibility.AccessibilityEvent;

public final class GateService extends AccessibilityService {
    public static volatile GateService instance;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String foreground = "";
    private long lastGate;
    private final Runnable tick = new Runnable() {
        @Override public void run() { enforce(); schedule(); }
    };
    private final BroadcastReceiver screen = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            // Retain the last package: some TVs resume it without a new window event.
            if (Intent.ACTION_SCREEN_OFF.equals(i.getAction())) { handler.removeCallbacks(tick); }
            else recheck();
        }
    };
    @Override protected void onServiceConnected() {
        instance = this;
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF); filter.addAction(Intent.ACTION_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(screen, filter, Context.RECEIVER_NOT_EXPORTED); else registerReceiver(screen, filter);
        recheck();
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || event.getPackageName() == null) return;
        String pkg = event.getPackageName().toString();
        // Volume overlays, notifications and the keyboard must not stop the countdown check.
        if (pkg.equals("com.android.systemui") || pkg.contains("inputmethod")) return;
        foreground = pkg; enforce(); schedule();
    }
    public void recheck() { handler.post(() -> { enforce(); schedule(); }); }
    private void schedule() {
        handler.removeCallbacks(tick);
        FamilyState s = FamilyState.get(this);
        if (s.enabled() && s.restricted().contains(foreground)) handler.postDelayed(tick, 500);
    }
    private void enforce() {
        FamilyState s = FamilyState.get(this);
        PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
        if (!power.isInteractive() || !s.enabled() || !s.restricted().contains(foreground) || (s.isOpen() && s.remaining() > 0)) return;
        if (SystemClock.elapsedRealtime() - lastGate < 1500) return;
        lastGate = SystemClock.elapsedRealtime();
        // Opening the quiz pauses the restricted activity. A simultaneous HOME global action
        // races with this launch on some firmware and can leave the launcher on top of the quiz.
        startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }
    @Override public void onInterrupt() { }
    @Override public void onDestroy() {
        if (instance == this) instance = null;
        handler.removeCallbacksAndMessages(null);
        try { unregisterReceiver(screen); } catch (IllegalArgumentException ignored) { }
        super.onDestroy();
    }
}
