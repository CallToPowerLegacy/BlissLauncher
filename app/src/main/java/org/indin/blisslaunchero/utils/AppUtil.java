package org.indin.blisslaunchero.utils;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;

import org.indin.blisslaunchero.model.AppItem;
import org.indin.blisslaunchero.R;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppUtil {

    /**
     * Uses the PackageManager to find all launchable apps.
     */
    public static List<AppItem> loadLaunchableApps(Context context) {
        PackageManager packageManager = context.getPackageManager();

        List<ApplicationInfo> apps = packageManager.getInstalledApplications(0);
        List<AppItem> launchableApps = new ArrayList<>();
        for (ApplicationInfo appInfo : apps) {
            String packageName = appInfo.packageName;
            Intent intent = packageManager.getLaunchIntentForPackage(packageName);
            if (intent != null) {
                String componentName = intent.getComponent().toString();

                boolean iconFromIconPack = true;
                Drawable appIcon = null;
                boolean isClock = false;
                boolean isCalendar = false;

                // Load icon from icon pack if present
                if (IconPackUtil.iconPackPresent) {
                    isClock = IconPackUtil.isClock(componentName);
                    isCalendar = IconPackUtil.isCalendar(componentName);
                    appIcon = IconPackUtil.getIconFromIconPack(context, componentName);
                }
                if (appIcon == null) {
                    appIcon = appInfo.loadIcon(packageManager);
                    iconFromIconPack = false;
                    appIcon = GraphicsUtil.scaleImage(context, appIcon, 1f);
                    appIcon = GraphicsUtil.maskImage(context, appIcon);
                }

                boolean isSystemApp = false;

                if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                    isSystemApp = true;
                }

                AppItem launchableApp = new AppItem(
                        appInfo.loadLabel(packageManager),
                        packageName,
                        appIcon,
                        intent,
                        componentName,
                        iconFromIconPack,
                        isSystemApp,
                        isClock,
                        isCalendar);
                launchableApps.add(launchableApp);
            }
        }

        Collections.sort(launchableApps, new Comparator<AppItem>() {
            @Override
            public int compare(AppItem app1, AppItem app2) {
                Collator collator = Collator.getInstance();
                return collator.compare(app1.getLabel(), app2.getLabel());
            }
        });

        return launchableApps;
    }

    /**
     * Currently picks four apps for the dock (Phone, SMS, Browser, Camera)
     */
    public static List<AppItem> getPinnedApps(Context context, List<AppItem> launchableApps) {
        PackageManager pm = context.getPackageManager();
        Intent[] intents = {
                new Intent(Intent.ACTION_DIAL),
                new Intent(Intent.ACTION_VIEW, Uri.parse("sms:")),
                new Intent(Intent.ACTION_VIEW, Uri.parse("http:")),
                new Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        };

        List<AppItem> pinnedApps = new ArrayList<>();
        for (Intent intent : intents) {
            String packageName = getPackageNameForIntent(intent, pm);
            for (AppItem app : launchableApps) {
                if (app.getPackageName().equals(packageName)) {
                    pinnedApps.add(app);
                    break;
                }
            }
        }
        return pinnedApps;
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

            if (intent != null) {
                String componentName = intent.getComponent().toString();

                boolean iconFromIconPack = true;
                Drawable appIcon = null;
                boolean isClock = false;
                boolean isCalendar = false;
                // Load icon from icon pack if present
                if (IconPackUtil.iconPackPresent) {
                    isClock = IconPackUtil.isClock(componentName);
                    isCalendar = IconPackUtil.isCalendar(componentName);
                    appIcon = IconPackUtil.getIconFromIconPack(context, componentName);
                }
                if (appIcon == null) {
                    appIcon = appInfo.loadIcon(packageManager);
                    iconFromIconPack = false;
                    appIcon = GraphicsUtil.scaleImage(context, appIcon, 1f);
                    appIcon = GraphicsUtil.maskImage(context, appIcon);
                }

                return new AppItem(appInfo.loadLabel(packageManager),
                        packageName,
                        appIcon,
                        intent,
                        componentName,
                        iconFromIconPack,
                        (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0,
                        isClock,
                        isCalendar);
            } else {
                return null;
            }
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
}