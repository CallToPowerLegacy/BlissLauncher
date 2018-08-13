/*
 * Copyright 2018 /e/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.indin.blisslaunchero.framework.broadcast;

import org.greenrobot.eventbus.EventBus;
import org.indin.blisslaunchero.BlissLauncher;
import org.indin.blisslaunchero.features.launcher.AppProvider;
import org.indin.blisslaunchero.framework.events.AppAddEvent;
import org.indin.blisslaunchero.framework.events.AppChangeEvent;
import org.indin.blisslaunchero.framework.events.AppRemoveEvent;
import org.indin.blisslaunchero.framework.utils.UserHandle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

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
            AppAddEvent appAddEvent = new AppAddEvent();
            appAddEvent.packageName = packageName;
            EventBus.getDefault().post(appAddEvent);
        }

        if ("android.intent.action.PACKAGE_CHANGED".equalsIgnoreCase(action)) {
            Log.i(TAG, "handleEvent: changed " + packageName);
            Intent launchIntent = ctx.getPackageManager().getLaunchIntentForPackage(packageName);
            if (launchIntent == null) {
                return;
            }
            BlissLauncher.getApplication(ctx).getIconsHandler().resetIconDrawableForPackage(
                    launchIntent.getComponent(), user);
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

        BlissLauncher.getApplication(ctx).resetIconsHandler();

        // Reload application list
        final AppProvider provider = BlissLauncher.getApplication(ctx).getAppProvider();
        if (provider != null) {
            provider.reload();
        }
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String packageName = intent.getData().getSchemeSpecificPart();

        if (packageName.equalsIgnoreCase(ctx.getPackageName())) {
            // When running KISS locally, sending a new version of the APK immediately triggers a
            // "package removed" for fr.neamar.kiss,
            // There is no need to handle this event.
            // Discarding it makes startup time much faster locally as apps don't have to be
            // loaded twice.
            return;
        }
        handleEvent(ctx,
                intent.getAction(),
                packageName, new UserHandle(),
                intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
        );

    }
}
