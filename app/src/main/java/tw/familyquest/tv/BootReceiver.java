package tw.familyquest.tv;
import android.content.*;
public final class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        FamilyState state = FamilyState.get(c);
        if (state.hasPin() && state.serverEnabled()) c.startForegroundService(new Intent(c, ParentServerService.class));
    }
}
