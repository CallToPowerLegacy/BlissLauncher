package foundation.e.blisslauncher.features.shortcuts;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.LauncherApps;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

@TargetApi(Build.VERSION_CODES.O)
public class AddItemActivity extends AppCompatActivity {

    private static final String TAG = "AddItemActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LauncherApps launcherApps = (LauncherApps) getSystemService(Context.LAUNCHER_APPS_SERVICE);
        LauncherApps.PinItemRequest request = launcherApps.getPinItemRequest(getIntent());
        if (request == null) {
            finish();
            return;
        }

        if (request.getRequestType() == LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) {
            InstallShortcutReceiver.queueShortcut(
                    new ShortcutInfoCompat(request.getShortcutInfo()), this);
            request.accept();
            finish();
            return;
        }
    }
}
