package org.indin.blisslaunchero.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.indin.blisslaunchero.R;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class IconPackUtil {
    public static String TAG = "BLISS_LAUNCHER_ICONS";

    public static boolean iconPackPresent = false;
    private static Drawable iconBackground1 = null;
    private static Drawable iconBackground2 = null;
    private static Drawable wallpaper = null;
    private static Drawable iconMask = null;

    public static Drawable folderBackground;

    private static String ICON_PACK_PACKAGE = "org.indin.blissiconpack";

    private static Resources iconPackResources;

    private static Map<String, Integer> iconMapper;
    private static Context mContext;

    public static boolean iconPackExists(PackageManager packageManager) {
        try {
            packageManager.getPackageInfo(ICON_PACK_PACKAGE,
                    PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }

    public static boolean isClock(String componentName) {
        return iconMapper.get(componentName) != null &&
                iconMapper.get(componentName) == Constants.DEFAULT_CLOCK_ID;
    }

    public static Drawable getIconFromIconPack(Context context, String componentName) {
        if (iconMapper == null) {
            return null;
        }
        if (iconMapper.containsKey(componentName)) {
            return iconPackResources.getDrawable(iconMapper.get(componentName), null);
        }
        return null;
    }

    public static void cacheIconsFromIconPack(Context context) {
        mContext = context;
        if (iconMapper != null) {
            return;
        }
        if (!iconPackExists(context.getPackageManager())) {
            return;
        }

        iconPackPresent = true;
        iconMapper = new HashMap<>();
        try {
            Context iconPackContext = context.createPackageContext(ICON_PACK_PACKAGE, 0);
            iconPackResources = context.getPackageManager()
                    .getResourcesForApplication(ICON_PACK_PACKAGE);

            InputStream stream = iconPackContext.getAssets().open("appfilter.xml");
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
            NodeList items = doc.getElementsByTagName("item");
            for (int i = 0; i < items.getLength(); i++) {
                Node item = items.item(i);
                String componentName = item.getAttributes().getNamedItem(
                        "component").getTextContent();
                String drawableName = item.getAttributes().getNamedItem(
                        "drawable").getTextContent();
                int drawableId = drawableName.equals("clock") ? Constants.DEFAULT_CLOCK_ID
                        : iconPackResources.getIdentifier(drawableName, "drawable",
                                ICON_PACK_PACKAGE);
                if (drawableId > 0) {
                    iconMapper.put(componentName, drawableId);
                }
            }

            iconBackground1 = iconPackResources.getDrawable(
                    iconPackResources.getIdentifier("iconback_d", "drawable", ICON_PACK_PACKAGE),
                    null);
            if (iconBackground1 == null) {
                iconBackground1 = ContextCompat.getDrawable(context, R.drawable.iconback_d);
            }
            iconBackground2 = iconPackResources.getDrawable(
                    iconPackResources.getIdentifier("iconback_d", "drawable", ICON_PACK_PACKAGE),
                    null);
            if (iconBackground2 == null) {
                iconBackground2 = ContextCompat.getDrawable(context, R.drawable.iconback_d);
            }
            folderBackground = iconPackResources.getDrawable(
                    iconPackResources.getIdentifier("iconback_d", "drawable", ICON_PACK_PACKAGE),
                    null);
            wallpaper = iconPackResources.getDrawable(
                    iconPackResources.getIdentifier("wall1", "drawable", ICON_PACK_PACKAGE),
                    null);
            iconMask = iconPackResources.getDrawable(
                    iconPackResources.getIdentifier("iconmask", "drawable", ICON_PACK_PACKAGE),
                    null);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not load icons from icon pack");
        } catch (IOException e) {
            Log.e(TAG, "Could not create parser");
        } catch (SAXException e) {
            Log.e(TAG, "Could not parse file");
        } catch (ParserConfigurationException e) {
            Log.e(TAG, "Could not create parser");
        }
        Log.i(TAG, "Cached " + iconMapper.size() + " icons");
    }

    /**
     * Allows for use of two different app icon backgrounds based on the
     * first character of the app label
     */
    public static Drawable getIconBackground(char firstCharacter) {
        if (Character.toLowerCase(firstCharacter) % 2 == 0) {
            return iconBackground1;
        } else {
            return iconBackground2;
        }
    }

    public static Bitmap getWallpaper() {
        return ((BitmapDrawable) wallpaper).getBitmap();
    }

    public static Drawable getIconMask() {
        return iconMask;
    }
}
