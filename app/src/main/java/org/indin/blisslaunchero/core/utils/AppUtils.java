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
package org.indin.blisslaunchero.core.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Process;
import android.os.UserManager;
import android.support.annotation.Nullable;

import org.indin.blisslaunchero.BlissLauncher;
import org.indin.blisslaunchero.features.launcher.AppProvider;
import org.indin.blisslaunchero.core.IconsHandler;
import org.indin.blisslaunchero.core.database.model.ApplicationItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AppUtils {

    private static final String TAG = "AppUtils";
    private static volatile LauncherApps sLauncherApps;

    /**
     * Uses the PackageManager to find all launchable apps.
     */
    @SuppressLint("CheckResult")
    public static Map<String, ApplicationItem> loadAll(Context context) {

        List<ApplicationItem> launchableApps = new ArrayList<>();
        List<String> defaultPinnedAppsPackages = new ArrayList<>();

        UserManager manager = (UserManager) context.getSystemService(Context.USER_SERVICE);

        if (sLauncherApps == null) {
            sLauncherApps = (LauncherApps) context.getSystemService(
                    Context.LAUNCHER_APPS_SERVICE);
        }
        IconsHandler iconsHandler = BlissLauncher.getApplication(context).getIconsHandler();

        // Handle multi-profile support introduced in Android 5 (#542)
        for (android.os.UserHandle profile : manager.getUserProfiles()) {
            UserHandle user = new UserHandle(manager.getSerialNumberForUser(profile), profile);
            launchableApps = sLauncherApps.getActivityList(null, profile).parallelStream()
                    .filter(activityInfo -> {
                        ApplicationInfo appInfo = activityInfo.getApplicationInfo();
                        return !appInfo.packageName.equalsIgnoreCase(AppProvider.MICROG_PACKAGE)
                                && !appInfo.packageName.equalsIgnoreCase(AppProvider.MUPDF_PACKAGE);
                    })
                    .map(activityInfo -> {
                        ApplicationItem applicationItem = new ApplicationItem(context, activityInfo,
                                profile);
                        ApplicationInfo appInfo = activityInfo.getApplicationInfo();
                        applicationItem.icon = iconsHandler.getDrawableIconForPackage(
                                activityInfo.getComponentName(), user);
                        String componentName = activityInfo.getComponentName().toString();
                        applicationItem.appType = iconsHandler.isClock(componentName)
                                ? ApplicationItem.TYPE_CLOCK : (iconsHandler.isCalendar(
                                componentName)
                                ? ApplicationItem.TYPE_CALENDAR : ApplicationItem.TYPE_DEFAULT);
                        applicationItem.title = activityInfo.getLabel().toString();
                        applicationItem.container = Constants.CONTAINER_DESKTOP;
                        if (appInfo.packageName.equalsIgnoreCase("com.generalmagic.magicearth")) {
                            applicationItem.title = "Maps";
                        }
                        applicationItem.packageName = appInfo.packageName;
                        return applicationItem;
                    })
                    .collect(Collectors.toList());
        }

        Map<String, ApplicationItem> appArrayMap = new LinkedHashMap<>();
        for (ApplicationItem applicationItem : launchableApps) {
            appArrayMap.put(applicationItem.id, applicationItem);
        }
        return appArrayMap;
    }

    @Nullable
    public static String getPackageNameForIntent(Intent intent, PackageManager pm) {
        List<ResolveInfo> activities = pm.queryIntentActivities(intent,
                0);
        if (activities.size() == 0) return null;
        ActivityInfo activity = activities.get(0).activityInfo;
        return activity.applicationInfo.packageName;
    }

    public static ApplicationItem createAppItem(Context context, String packageName) {
        if (packageName.equalsIgnoreCase(AppProvider.MICROG_PACKAGE)
                || packageName.equalsIgnoreCase(AppProvider.MUPDF_PACKAGE)) {
            return null;
        }
        if (sLauncherApps == null) {
            sLauncherApps = (LauncherApps) context.getSystemService(
                    Context.LAUNCHER_APPS_SERVICE);
        }

        IconsHandler iconsHandler = BlissLauncher.getApplication(context).getIconsHandler();

        List<LauncherActivityInfo> launcherActivityInfos = sLauncherApps.getActivityList(
                packageName,
                Process.myUserHandle());
        if (launcherActivityInfos == null || launcherActivityInfos.size() == 0) {
            return null;
        }
        LauncherActivityInfo launcherActivityInfo = sLauncherApps.getActivityList(packageName,
                Process.myUserHandle()).get(0);
        if (launcherActivityInfo != null) {
            ApplicationItem applicationItem = new ApplicationItem(context, launcherActivityInfo,
                    Process.myUserHandle());
            ApplicationInfo appInfo = launcherActivityInfo.getApplicationInfo();
            applicationItem.icon = iconsHandler.getDrawableIconForPackage(
                    launcherActivityInfo.getComponentName(), new UserHandle());
            String componentName = launcherActivityInfo.getComponentName().toString();
            applicationItem.appType = iconsHandler.isClock(componentName)
                    ? ApplicationItem.TYPE_CLOCK : (iconsHandler.isCalendar(
                    componentName)
                    ? ApplicationItem.TYPE_CALENDAR : ApplicationItem.TYPE_DEFAULT);
            applicationItem.title = launcherActivityInfo.getLabel().toString();
            applicationItem.container = Constants.CONTAINER_DESKTOP;
            if (appInfo.packageName.equalsIgnoreCase("com.generalmagic.magicearth")) {
                applicationItem.title = "Maps";
            }
            applicationItem.packageName = appInfo.packageName;
            return applicationItem;
        }

        return null;
    }
}