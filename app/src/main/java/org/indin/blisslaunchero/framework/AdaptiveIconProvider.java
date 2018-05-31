package org.indin.blisslaunchero.framework;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;

import org.indin.blisslaunchero.framework.customviews.AdaptiveIconDrawableCompat;
import org.indin.blisslaunchero.framework.util.ResourceUtils;
import org.xmlpull.v1.XmlPullParser;

/**
 * Created by falcon on 19/4/18.
 */

public class AdaptiveIconProvider {

    private static final String TAG = "AdaptiveIconProvider";

    private static final String ANDROID_SCHEMA = "http://schemas.android.com/apk/res/android";
    private static final String[] IC_DIRS = new String[]{"mipmap", "drawable"};
    private static final String[] IC_CONFIGS = new String[]{"-anydpi-v26", "-v26", ""};

    public Drawable load(Context context, String packageName) {
        if (context == null) {
            throw new IllegalStateException(
                    "Loader.with(Context) must be called before loading an icon.");
        }

        PackageManager packageManager = context.getPackageManager();
        Drawable background = null, foreground = null;

        try {
            Resources resources = packageManager.getResourcesForApplication(packageName);
            Resources.Theme theme = resources.newTheme();
            ResourceUtils.setFakeConfig(resources, 26); //Build.VERSION_CODES.O = 26

            AssetManager assetManager = resources.getAssets();

            XmlResourceParser manifestParser = null;
            String iconName = null;
            int iconId = 0;
            try {
                manifestParser = assetManager.openXmlResourceParser("AndroidManifest.xml");
                int eventType;

                String matcher = "application";
                while ((eventType = manifestParser.nextToken()) != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && manifestParser.getName().equals(
                            matcher)) {
                        iconId = manifestParser.getAttributeResourceValue(
                                ANDROID_SCHEMA, "icon",
                                0);

                        if (iconId != 0) {
                            iconName = resources.getResourceName(iconId);
                            if (iconName.contains("/")) {
                                iconName = iconName.split("/")[1];
                            }
                            break;
                        }
                        if (iconId == 0) {
                            matcher = "activity";
                        }
                    }
                }
                manifestParser.close();
            } catch (Exception e) {
            }

            XmlResourceParser parser = null;
            for (int dir = 0; dir < IC_DIRS.length && parser == null; dir++) {
                for (int config = 0; config < IC_CONFIGS.length && parser == null; config++) {
                    for (String name : iconName != null && !iconName.equals("ic_launcher")
                            ? new String[]{iconName, "ic_launcher"} : new String[]{"ic_launcher"}) {
                        try {
                            parser = assetManager.openXmlResourceParser(
                                    "res/" + IC_DIRS[dir] + IC_CONFIGS[config] + "/" + name
                                            + ".xml");
                        } catch (Exception e) {
                            continue;
                        }

                        if (parser != null) {
                            break;
                        }
                    }
                }
            }

            int backgroundRes = -1, foregroundRes = -1;
            if (parser != null) {
                int event;
                while ((event = parser.getEventType()) != XmlPullParser.END_DOCUMENT) {
                    Log.i(TAG, packageName + ":parserName: " + parser.getName()+" "+parser.getAttributeCount());
                    if (event == XmlPullParser.START_TAG) {
                        switch (parser.getName()) {
                            case "background":
                                try {
                                    backgroundRes = parser.getAttributeResourceValue(
                                            "http://schemas.android.com/apk/res/android",
                                            "drawable", 0);
                                } catch (Exception e) {
                                    try {
                                        backgroundRes = parser.getAttributeResourceValue(
                                                "http://schemas.android.com/apk/res/android",
                                                "mipmap", 0);
                                    } catch (Exception e1) {
                                    }
                                }
                                break;
                            case "foreground":
                                try {
                                    foregroundRes = parser.getAttributeResourceValue(
                                            "http://schemas.android.com/apk/res/android",
                                            "drawable", 0);
                                } catch (Exception e) {
                                    try {
                                        foregroundRes = parser.getAttributeResourceValue(
                                                "http://schemas.android.com/apk/res/android",
                                                "mipmap", 0);
                                    } catch (Exception e1) {
                                    }
                                }
                                break;
                        }
                    }
                    parser.next();
                }

                parser.close();
            }

            if (background == null && backgroundRes != 0) {
                try {
                    background = ResourcesCompat.getDrawable(resources, backgroundRes, theme);
                } catch (Resources.NotFoundException e) {
                    try {
                        background = ResourcesCompat.getDrawable(resources,
                                resources.getIdentifier("ic_launcher_background", "mipmap",
                                        packageName), theme);
                    } catch (Resources.NotFoundException e1) {
                        try {
                            background = ResourcesCompat.getDrawable(resources,
                                    resources.getIdentifier("ic_launcher_background", "drawable",
                                            packageName), theme);
                        } catch (Resources.NotFoundException e2) {
                        }
                    }
                }
            }

            if (foreground == null) {
                try {
                    foreground = ResourcesCompat.getDrawable(resources, foregroundRes, theme);
                } catch (Resources.NotFoundException e) {
                    try {
                        foreground = ResourcesCompat.getDrawable(resources,
                                resources.getIdentifier("ic_launcher_foreground", "mipmap",
                                        packageName), theme);
                    } catch (Resources.NotFoundException e1) {
                        try {
                            foreground = ResourcesCompat.getDrawable(resources,
                                    resources.getIdentifier("ic_launcher_foreground", "drawable",
                                            packageName), theme);
                        } catch (Resources.NotFoundException e2) {
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (foreground != null && background != null) {
            return new AdaptiveIconDrawableCompat(background, foreground);
        } else {
            return null;
        }
    }
}
