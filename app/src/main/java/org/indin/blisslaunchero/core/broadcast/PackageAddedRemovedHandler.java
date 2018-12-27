package org.indin.blisslaunchero.core.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.indin.blisslaunchero.BlissLauncher;
import org.indin.blisslaunchero.features.launcher.AppProvider;
import org.indin.blisslaunchero.core.events.AppAddEvent;
import org.indin.blisslaunchero.core.events.AppChangeEvent;
import org.indin.blisslaunchero.core.events.AppRemoveEvent;
import org.indin.blisslaunchero.core.utils.UserHandle;

public class PackageAddedRemovedHandler extends BroadcastReceiver {

    private static final String TAG = "PackageAddedRemovedHand";

    public static void handleEvent(Context ctx, String action, String packageName, UserHandle user,
            boolean replacing) {
        Log.d(TAG, "handleEvent() called with: ctx = [" + ctx + "], action = [" + action
                + "], packageName = [" + packageName + "], user = [" + user + "], replacing = ["
                + replacing + "]");
        // Insert into history new packages (not updated ones)
        if ("android.intent.action.PACKAGE_ADDED".equals(action) && !replacing) {
            Log.i(TAG, "handleEvent: added " + packageName);

            Intent launchIntent = ctx.getPackageManager().getLaunchIntentForPackage(packageName);
            if (launchIntent == null) {//for some plugin app
                return;
            }

            BlissLauncher.getApplication(ctx).resetIconsHandler();

            AppAddEvent appAddEvent = new AppAddEvent();
            appAddEvent.packageName = packageName;
            EventBus.getDefault().post(appAddEvent);
        }

        if ("android.intent.action.PACKAGE_CHANGED".equalsIgnoreCase(action)) {
            Log.i(TAG, "handleEvent: changed " + packageName);
            Intent launchIntent = ctx.getPackageManager().getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                BlissLauncher.getApplication(ctx).getIconsHandler().resetIconDrawableForPackage(
                        launchIntent.getComponent(), user);
            }

            BlissLauncher.getApplication(ctx).resetIconsHandler();

            AppChangeEvent appChangeEvent = new AppChangeEvent();
            appChangeEvent.packageName = packageName;
            EventBus.getDefault().post(appChangeEvent);

        }
        if ("android.intent.action.PACKAGE_REMOVED".equals(action) && !replacing) {
            Log.i(TAG, "handleEvent: removed " + packageName);
            AppRemoveEvent appRemoveEvent = new AppRemoveEvent();
            appRemoveEvent.packageName = packageName;
            EventBus.getDefault().post(appRemoveEvent);
        }

        // Reload application list
        final AppProvider provider = BlissLauncher.getApplication(ctx).getAppProvider();
        if (provider != null) {
            provider.reload();
        }
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String packageName = intent.getData().getSchemeSpecificPart();

        Log.d(TAG, "onReceive() called with: ctx = [" + ctx + "], intent = [" + intent + "]");
        if (packageName.equalsIgnoreCase(ctx.getPackageName())) {
            return;
        }
        handleEvent(ctx,
                intent.getAction(),
                packageName, new UserHandle(),
                intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
        );

    }
}
