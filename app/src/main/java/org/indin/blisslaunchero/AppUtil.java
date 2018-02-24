package org.indin.blisslaunchero;

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

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppUtil {

    /**
     * Uses the PackageManager to find all launchable apps.
     */
    public static List<AppItem> loadLaunchableApps(Context context, int iconWidth) {
        PackageManager packageManager = context.getPackageManager();

        List<ApplicationInfo> apps = packageManager.getInstalledApplications(0);
        List<AppItem> launchableApps = new ArrayList<>();
        for (ApplicationInfo app : apps) {
            String packageName = app.packageName;
            Intent intent = packageManager.getLaunchIntentForPackage(packageName);
            if (intent != null) {
                String componentName = intent.getComponent().toString();

                boolean iconFromIconPack = true;
                Drawable appIcon = null;
                // Load icon from icon pack if present
                if (IconPackUtil.iconPackPresent) {
                    appIcon = IconPackUtil.getIconFromIconPack(context, componentName);
                }
                if (appIcon == null) {
                    appIcon = app.loadIcon(packageManager);
                    iconFromIconPack = false;
                    appIcon = GraphicsUtil.scaleImage(context, appIcon, 1f, iconWidth);
                    appIcon = GraphicsUtil.maskImage(context, appIcon);
                }

                AppItem launchableApp = new AppItem(
                        app.loadLabel(packageManager),
                        packageName,
                        appIcon,
                        intent,
                        componentName,
                        iconFromIconPack
                );
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
    public static AppItem createAppItem(Context context, String packageName, int iconWidth) {
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
            Intent intent = packageManager.getLaunchIntentForPackage(packageName);

            if (intent == null) {
                return null;
            }

            String componentName = intent.getComponent().toString();
            Drawable appIcon;
            boolean iconFromIconPack = true;
            if (IconPackUtil.iconPackPresent) {
                appIcon = IconPackUtil.getIconFromIconPack(context, componentName);
                if (appIcon == null) {
                    appIcon = appInfo.loadIcon(packageManager);
                    iconFromIconPack = false;
                    appIcon = GraphicsUtil.scaleImage(context, appIcon, 1f, iconWidth);
                    appIcon = GraphicsUtil.maskImage(context, appIcon);
                }
            } else {
                appIcon = appInfo.loadIcon(packageManager);
                iconFromIconPack = false;
            }

            AppItem appItem = new AppItem(appInfo.loadLabel(packageManager),
                    packageName,
                    appIcon,
                    intent,
                    componentName,
                    iconFromIconPack);
            return appItem;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
}