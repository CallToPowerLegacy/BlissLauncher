package org.indin.blisslaunchero.framework.util;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.text.SpannableString;
import android.util.Log;

import org.indin.blisslaunchero.R;
import org.indin.blisslaunchero.data.model.AppItem;
import org.indin.blisslaunchero.features.launcher.AllAppsList;
import org.indin.blisslaunchero.framework.AdaptiveIconProvider;
import org.indin.blisslaunchero.framework.Utilities;
import org.indin.blisslaunchero.framework.customviews.AdaptiveIconDrawableCompat;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppUtil {

    private static final String TAG = "AppUtil";

    /**
     * Uses the PackageManager to find all launchable apps.
     */
    public static AllAppsList loadAllApps(Context context) {
        List<AppItem> launchableApps = new ArrayList<>();
        PackageManager packageManager = context.getPackageManager();

        List<ApplicationInfo> apps = packageManager.getInstalledApplications(0);
        // Handle multi-profile support introduced in Android 5 (#542)

        for (ApplicationInfo appInfo : apps) {
            String packageName = appInfo.packageName;

            Intent intent = packageManager.getLaunchIntentForPackage(packageName);
            if (intent != null) {
                ActivityInfo activityInfo = intent.resolveActivityInfo(packageManager, 0);

                String componentName = intent.getComponent().toString();
                Log.i(TAG, "loadLaunchableApps: " + componentName);
                boolean iconFromIconPack = true;
                Drawable appIcon = null;
                boolean isClock = false;
                boolean isCalendar = false;
                boolean isAdaptive = false;

                // Load icon from icon pack if present
                if (IconPackUtil.iconPackPresent) {
                    isClock = IconPackUtil.isClock(componentName);
                    isCalendar = IconPackUtil.isCalendar(componentName);
                    appIcon = IconPackUtil.getIconFromIconPack(context, componentName);
                }
                if (appIcon == null) {
                    isAdaptive = true;
                    appIcon = new AdaptiveIconProvider().load(context, packageName);
                    if (appIcon == null) {
                        Log.i(TAG, "appIconNull:  "+packageName);
                        Drawable iconDrawable = appInfo.loadIcon(packageManager);
                        /*GraphicsUtil graphicsUtil = new GraphicsUtil(context);
                        appIcon = graphicsUtil.convertToRoundedCorner(context,
                                graphicsUtil.addBackground(iconDrawable, false));*/
                        if (Utilities.ATLEAST_OREO
                                && iconDrawable instanceof AdaptiveIconDrawable) {
                            appIcon = new AdaptiveIconDrawableCompat(
                                    ((AdaptiveIconDrawable) iconDrawable).getBackground(),
                                    ((AdaptiveIconDrawable) iconDrawable).getForeground());
                        } else {
                            GraphicsUtil graphicsUtil = new GraphicsUtil(context);
                            appIcon = graphicsUtil.convertToRoundedCorner(context,
                                    graphicsUtil.addBackground(iconDrawable, false));
                        }

                    }
                }

                boolean isSystemApp = false;

                if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                    isSystemApp = true;
                }

                AppItem launchableApp = new AppItem(activityInfo.loadLabel(packageManager).toString(),
                        packageName,
                        appIcon,
                        intent,
                        componentName,
                        iconFromIconPack,
                        isSystemApp,
                        isClock,
                        isCalendar,
                        isAdaptive);
                launchableApps.add(launchableApp);
            }
        }

        Collections.sort(launchableApps, (app1, app2) -> {
            Collator collator = Collator.getInstance();
            return collator.compare(app1.getLabel(), app2.getLabel());
        });
        Log.i(TAG, "loadLaunchableApps: " + launchableApps.size());
        AllAppsList allAppsList;
        List<AppItem> pinnedApps = getPinnedApps(context, launchableApps);
        launchableApps.removeAll(pinnedApps);
        allAppsList = new AllAppsList(launchableApps, pinnedApps);
        return allAppsList;
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
                boolean isAdaptive = false;

                // Load icon from icon pack if present
                if (IconPackUtil.iconPackPresent) {
                    isClock = IconPackUtil.isClock(componentName);
                    isCalendar = IconPackUtil.isCalendar(componentName);
                    appIcon = IconPackUtil.getIconFromIconPack(context, componentName);
                }
                if (appIcon == null) {
                    isAdaptive = true;
                    appIcon = new AdaptiveIconProvider().load(context, packageName);
                    if (appIcon == null) {
                        Drawable iconDrawable = appInfo.loadIcon(packageManager);
                        /*GraphicsUtil graphicsUtil = new GraphicsUtil(context);
                        appIcon = graphicsUtil.convertToRoundedCorner(context,
                                graphicsUtil.addBackground(iconDrawable, false));*/
                        if (Utilities.ATLEAST_OREO
                                && iconDrawable instanceof AdaptiveIconDrawable) {
                            appIcon = new AdaptiveIconDrawableCompat(
                                    ((AdaptiveIconDrawable) iconDrawable).getBackground(),
                                    ((AdaptiveIconDrawable) iconDrawable).getForeground());
                        } else {
                            GraphicsUtil graphicsUtil = new GraphicsUtil(context);
                            appIcon = graphicsUtil.convertToRoundedCorner(context,
                                    graphicsUtil.addBackground(iconDrawable, false));
                        }
                    }
                }

                return new AppItem(appInfo.loadLabel(packageManager).toString(),
                        packageName,
                        appIcon,
                        intent,
                        componentName,
                        iconFromIconPack,
                        (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0,
                        isClock,
                        isCalendar,
                        isAdaptive);
            } else {
                return null;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}