package org.indin.blisslaunchero.framework;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.indin.blisslaunchero.framework.customviews.AdaptiveIconDrawableCompat;
import org.indin.blisslaunchero.framework.utils.GraphicsUtil;
import org.indin.blisslaunchero.framework.utils.UserHandle;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

/**
 * Inspired from http://stackoverflow.com/questions/31490630/how-to-load-icon-from-icon-pack
 */
public class IconsHandler {

    private static final String TAG = "IconsHandler";
    // map with available icons packs
    private final HashMap<String, String> iconsPacks = new HashMap<>();
    // map with available drawable for an icons pack
    private final Map<String, String> packagesDrawables = new HashMap<>();
    // graphics util to manipulate bitmaps.
    private final GraphicsUtil graphicsUtil;
    // instance of a resource object of an icon pack
    private Resources iconPackres;
    // package name of the icons pack
    private String iconsPackPackageName;
    // bitmap mask of an icons pack
    private Bitmap maskImage = null;
    // front image of an icons pack
    private Bitmap frontImage = null;
    // scale factor of an icons pack
    private float factor = 1.0f;
    private final PackageManager pm;
    private final Context ctx;

    public IconsHandler(Context ctx) {
        super();
        this.ctx = ctx;
        this.pm = ctx.getPackageManager();
        graphicsUtil = new GraphicsUtil(ctx);
        loadIconsPack("org.indin.blissiconpack");
    }


    private boolean iconPackExists(PackageManager packageManager) {
        try {
            packageManager.getPackageInfo("org.indin.blissiconpack",
                    PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }


    /**
     * Parse icons pack metadata
     *
     * @param packageName Android package ID of the package to parse
     */
    public void loadIconsPack(String packageName) {

        //clear icons pack
        if (iconPackExists(pm)) {
            iconsPackPackageName = packageName;
        } else {
            iconsPackPackageName = "default";
        }

        packagesDrawables.clear();
        cacheClear();

        // system icons, nothing to do
        if (iconsPackPackageName.equalsIgnoreCase("default")) {
            return;
        }

        XmlResourceParser xpp = null;

        try {
            // search appfilter.xml into icons pack apk resource folder
            iconPackres = pm.getResourcesForApplication(iconsPackPackageName);
            Context iconPackContext = ctx.createPackageContext(iconsPackPackageName, 0);
            InputStream stream = iconPackContext.getAssets().open("appfilter.xml");
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
            NodeList items = doc.getElementsByTagName("item");
            for (int i = 0; i < items.getLength(); i++) {
                Node item = items.item(i);
                String componentName = item.getAttributes().getNamedItem(
                        "component").getTextContent();
                String drawableName = item.getAttributes().getNamedItem(
                        "drawable").getTextContent();

                if (!packagesDrawables.containsKey(componentName)) {
                    packagesDrawables.put(componentName, drawableName);
                }
            }
            Log.i(TAG, "Cached " + packagesDrawables.size() + " icons");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Error parsing appfilter.xml " + e);
        }

    }


    private Drawable getDefaultAppDrawable(ComponentName componentName, UserHandle userHandle) {
        try {
            LauncherApps launcher = (LauncherApps) ctx.getSystemService(
                    Context.LAUNCHER_APPS_SERVICE);
            LauncherActivityInfo info = launcher.getActivityList(componentName.getPackageName(),
                    userHandle.getRealHandle()).get(0);
            return info.getBadgedIcon(0);
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "Unable to found component " + componentName.toString() + e);
            return null;
        }
    }

    public boolean isClock(String componentName) {
        return packagesDrawables.get(componentName) != null &&
                packagesDrawables.get(componentName).equals("clock");
    }

    public boolean isCalendar(String componentName) {
        return packagesDrawables.get(componentName) != null &&
                packagesDrawables.get(componentName).equals("calendar");
    }


    /**
     * Get or generate icon for an app
     */
    public Drawable getDrawableIconForPackage(ComponentName componentName, UserHandle userHandle) {
       /* // system icons, nothing to do
        if (iconsPackPackageName.equalsIgnoreCase("default")) {
            return this.getDefaultAppDrawable(componentName, userHandle);
        }*/
        String drawable = packagesDrawables.get(componentName.toString());
        if (drawable != null) { //there is a custom icon
            int id = iconPackres.getIdentifier(drawable, "drawable", iconsPackPackageName);
            if (id > 0) {
                //noinspection deprecation: Resources.getDrawable(int, Theme) requires SDK 21+
                try {
                    return iconPackres.getDrawable(id);
                } catch (Resources.NotFoundException e) {
                    // Unable to load icon, keep going.
                    e.printStackTrace();
                }
            }
        }

        // Search first in cache
        Drawable systemIcon = cacheGetDrawable(componentName.toString());
        if (systemIcon != null) {
            return systemIcon;
        }

        systemIcon = new AdaptiveIconProvider().load(ctx, componentName.getPackageName());
        if(systemIcon != null){
            Log.i(TAG, "getDrawableIconForPackage: herrrre");
            cacheStoreDrawable(componentName.toString(), systemIcon);
            return systemIcon;
        }

        systemIcon = this.getDefaultAppDrawable(componentName, userHandle);
        if (Utilities.ATLEAST_OREO
                && systemIcon instanceof AdaptiveIconDrawable) {
            systemIcon = new AdaptiveIconDrawableCompat(
                    ((AdaptiveIconDrawable) systemIcon).getBackground(),
                    ((AdaptiveIconDrawable) systemIcon).getForeground());
        } else {
            systemIcon = graphicsUtil.convertToRoundedCorner(ctx,
                    graphicsUtil.addBackground(systemIcon, false));
        }
        cacheStoreDrawable(componentName.toString(), systemIcon);
        return systemIcon;
    }

    private boolean isDrawableInCache(String key) {
        File drawableFile = cacheGetFileName(key);
        return drawableFile.isFile();
    }

    private void cacheStoreDrawable(String key, Drawable drawable) {
        Bitmap bitmap = getBitmapFromDrawable(drawable);
        File drawableFile = cacheGetFileName(key);
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(drawableFile);
            bitmap.compress(CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            Log.e(TAG, "Unable to store drawable in cache " + e);
        }
    }

    private Bitmap getBitmapFromDrawable(Drawable drawable) {
        final Bitmap bmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bmp);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bmp;
    }

    private Drawable cacheGetDrawable(String key) {

        if (!isDrawableInCache(key)) {
            return null;
        }

        FileInputStream fis;
        try {
            fis = new FileInputStream(cacheGetFileName(key));
            BitmapDrawable drawable =
                    new BitmapDrawable(this.ctx.getResources(), BitmapFactory.decodeStream(fis));
            fis.close();
            return drawable;
        } catch (Exception e) {
            Log.e(TAG, "Unable to get drawable from cache " + e);
        }

        return null;
    }

    /**
     * create path for icons cache like this
     * {cacheDir}/icons/{icons_pack_package_name}_{key_hash}.png
     */
    private File cacheGetFileName(String key) {
        return new File(getIconsCacheDir() + iconsPackPackageName + "_" + key.hashCode() + ".png");
    }


    /**
     * returns icons cache directory.
     */
    private File getIconsCacheDir() {
        return new File(this.ctx.getCacheDir().getPath() + "/icons/");
    }

    /**
     * Clear cache
     */
    private void cacheClear() {
        File cacheDir = this.getIconsCacheDir();

        if (!cacheDir.isDirectory()) {
            return;
        }

        for (File item : cacheDir.listFiles()) {
            if (!item.delete()) {
                Log.w(TAG, "Failed to delete file: " + item.getAbsolutePath());
            }
        }
    }

    public void resetIconDrawableForPackage(ComponentName componentName, UserHandle user) {
        if(!packagesDrawables.containsKey(componentName.toString())){
            Drawable icon = new AdaptiveIconProvider().load(ctx, componentName.getPackageName());
            if(icon != null){
                cacheStoreDrawable(componentName.toString(), icon);
                return;
            }

            icon = this.getDefaultAppDrawable(componentName, user);
            if (Utilities.ATLEAST_OREO
                    && icon instanceof AdaptiveIconDrawable) {
                icon = new AdaptiveIconDrawableCompat(
                        ((AdaptiveIconDrawable) icon).getBackground(),
                        ((AdaptiveIconDrawable) icon).getForeground());
            } else {
                icon = graphicsUtil.convertToRoundedCorner(ctx,
                        graphicsUtil.addBackground(icon, false));
            }
            cacheStoreDrawable(componentName.toString(), icon);
        }
    }
}
