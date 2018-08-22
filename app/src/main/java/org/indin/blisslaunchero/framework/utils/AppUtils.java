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
package org.indin.blisslaunchero.framework.utils;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import org.indin.blisslaunchero.BlissLauncher;
import org.indin.blisslaunchero.R;
import org.indin.blisslaunchero.features.launcher.AllAppsList;
import org.indin.blisslaunchero.features.launcher.AppProvider;
import org.indin.blisslaunchero.framework.IconsHandler;
import org.indin.blisslaunchero.framework.database.model.AppItem;

import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;

public class AppUtils {

    private static final String TAG = "AppUtils";

    /**
     * Uses the PackageManager to find all launchable apps.
     */
    public static AllAppsList loadAll(Context context) {

        List<AppItem> launchableApps = new ArrayList<>();
        List<String> defaultPinnedAppsPackages = new ArrayList<>();

        UserManager manager = (UserManager) context.getSystemService(Context.USER_SERVICE);

        LauncherApps launcher = (LauncherApps) context.getSystemService(
                Context.LAUNCHER_APPS_SERVICE);

        IconsHandler iconsHandler = BlissLauncher.getApplication(context).getIconsHandler();

        // Handle multi-profile support introduced in Android 5 (#542)
        for (android.os.UserHandle profile : manager.getUserProfiles()) {
            UserHandle user = new UserHandle(manager.getSerialNumberForUser(profile), profile);
            for (LauncherActivityInfo activityInfo : launcher.getActivityList(null, profile)) {
                ApplicationInfo appInfo = activityInfo.getApplicationInfo();

                if (appInfo.packageName.equalsIgnoreCase(AppProvider.MICROG_PACKAGE)
                        || appInfo.packageName.equalsIgnoreCase(AppProvider.MUPDF_PACKAGE)) {
                    continue;
                }

                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setComponent(activityInfo.getComponentName());
                intent.setFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

                Drawable appIcon = iconsHandler.getDrawableIconForPackage(
                        activityInfo.getComponentName(), user);
                Log.i(TAG, "loadAll: here");
                boolean isSystemApp = false;

                if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                    isSystemApp = true;
                }

                String labelName = activityInfo.getLabel().toString();

                if (appInfo.packageName.equalsIgnoreCase("com.generalmagic.magicearth")) {
                    labelName = "Maps";
                }
                AppItem launchableApp = new AppItem(labelName,
                        appInfo.packageName,
                        appIcon,
                        intent,
                        activityInfo.getComponentName().toString(),
                        isSystemApp,
                        iconsHandler.isClock(activityInfo.getComponentName().toString()),
                        iconsHandler.isCalendar(activityInfo.getComponentName().toString()));
                launchableApps.add(launchableApp);
            }
        }

        Collections.sort(launchableApps, (app1, app2) -> {
            Collator collator = Collator.getInstance();
            return collator.compare(app1.getLabel().toString(), app2.getLabel().toString());
        });

        LinkedHashMap<String, AppItem> appArrayMap = new LinkedHashMap<>();
        for (AppItem appItem : launchableApps) {
            appArrayMap.put(appItem.getPackageName(), appItem);
        }

        PackageManager pm = context.getPackageManager();
        Intent[] intents = {
                new Intent(Intent.ACTION_DIAL),
                new Intent(Intent.ACTION_VIEW, Uri.parse("sms:")),
                new Intent(Intent.ACTION_VIEW, Uri.parse("http:")),
                new Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        };
        for (Intent intent : intents) {
            String packageName = getPackageNameForIntent(intent, pm);
            for (AppItem app : launchableApps) {
                if (app.getPackageName().equals(packageName)) {
                    app.setPinnedApp(true);
                    defaultPinnedAppsPackages.add(packageName);
                    break;
                }
            }
        }
        AllAppsList allAppsList = new AllAppsList(appArrayMap, defaultPinnedAppsPackages);
        return allAppsList;
    }

    @Nullable
    private static String getPackageNameForIntent(Intent intent, PackageManager pm) {
        List<ResolveInfo> activities = pm.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        if (activities.size() == 0) return null;
        return activities.get(0).activityInfo.packageName;
    }

    public static void startActivityWithAnimation(Context context, Intent intent) {
        Bundle bundle = ActivityOptions.makeCustomAnimation(
                context, R.anim.enter, R.anim.leave).toBundle();
        context.startActivity(intent, bundle);
    }

    /**
     * Create an AppItem object given just a package name
     */
    public static AppItem createAppItem(Context context, String packageName) {
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
            Intent intent = packageManager.getLaunchIntentForPackage(packageName);

            if (appInfo.packageName.equalsIgnoreCase(AppProvider.MICROG_PACKAGE)
                    || appInfo.packageName.equalsIgnoreCase(AppProvider.MUPDF_PACKAGE)) {
                return null;
            }

            if (intent != null) {
                ResolveInfo resolveInfo = packageManager.resolveActivity(intent, 0);
                if (resolveInfo != null) {
                    ComponentName componentName = intent.getComponent();

                    ActivityInfo activityInfo = resolveInfo.activityInfo;
                    IconsHandler iconsHandler = BlissLauncher.getApplication(
                            context).getIconsHandler();
                    Drawable appIcon = iconsHandler.getDrawableIconForPackage(componentName,
                            new UserHandle());
                    boolean isSystemApp = false;

                    if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                        isSystemApp = true;
                    }

                    String labelName = activityInfo.loadLabel(packageManager).toString();

                    if (appInfo.packageName.equalsIgnoreCase("com.generalmagic.magicearth")) {
                        labelName = "Maps";
                    }
                    AppItem launchableApp = new AppItem(
                            labelName,
                            appInfo.packageName,
                            appIcon,
                            intent,
                            componentName.toString(),
                            isSystemApp,
                            iconsHandler.isClock(componentName.toString()),
                            iconsHandler.isCalendar(componentName.toString()));
                    return launchableApp;
                }
            }

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }
}