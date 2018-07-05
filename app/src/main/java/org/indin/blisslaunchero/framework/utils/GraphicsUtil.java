package org.indin.blisslaunchero.framework.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import org.indin.blisslaunchero.BlissLauncher;
import org.indin.blisslaunchero.framework.database.model.AppItem;
import org.indin.blisslaunchero.framework.DeviceProfile;

import java.util.HashMap;
import java.util.Map;

public class GraphicsUtil {

    private static final String TAG = "BLISS_GRAPHICS";
    private final DeviceProfile deviceProfile;
    private Context mContext;
    private String mPackageName;
    private int appIconWidth;

    public GraphicsUtil(Context context) {
        this.mContext = context;
        this.deviceProfile = BlissLauncher.getApplication(context).getDeviceProfile();
        this.appIconWidth = deviceProfile.iconSizePx;
    }

    /**
     * Takes 1 or more drawables and merges them to form a single Drawable.
     * However, if more than 4 drawables are provided, only the first 4 are used.
     */
    public Drawable generateFolderIcon(Context context, Drawable... sources) {
        int width = sources[0].getIntrinsicWidth();
        int height = width; // Square icons

        Log.i(TAG, "generateFolderIcon: " + width + "*" + height);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        int xOrigin = bitmap.getWidth() / 15;
        int yOrigin = bitmap.getHeight() / 15;
        int x = xOrigin;
        int y = yOrigin;
        int xIncrement = bitmap.getWidth() / 15;
        int yIncrement = bitmap.getHeight() / 15;
        int count = 0;
        int total = 0;
        for (Drawable d : sources) {
            d.setBounds(x, y, (int) (x + width / 2.5f), (int) (y + height / 2.5f));
            d.draw(canvas);
            x += (int) (width / 2.5f + xIncrement);
            count++;
            total++;
            if (count == 2) {
                count = 0;
                y += (int) (height / 2.5f + yIncrement);
                x = xOrigin;
            }
            if (total > 3) {
                break;
            }
        }

        Drawable convertedBitmap = convertToRoundedCorner(context, addBackground(bitmap, true));
        return convertedBitmap;
    }

    /**
     * A utility method that simplifies calls to the generateFolderIcon() method that
     * expects an array of Drawables.
     */
    public Drawable generateFolderIcon(Context context, AppItem app) {
        Drawable[] drawables = new Drawable[app.getSubApps().size()];
        for (int i = 0; i < app.getSubApps().size(); i++) {
            drawables[i] = app.getSubApps().get(i).getIcon();
        }
        return generateFolderIcon(context, drawables);
    }

    /**
     * Scales icons to match the icon pack
     */
    public Drawable scaleImage(Context context, Drawable image, float scaleFactor) {
        Log.i(TAG, "scaleImage: " + image.getIntrinsicWidth() + "*" + image.getIntrinsicHeight());
        if ((image == null) || !(image instanceof BitmapDrawable)) {
            return image;
        }
        Bitmap b = ((BitmapDrawable) image).getBitmap();
        int sizeX = Math.round(image.getIntrinsicWidth() * scaleFactor);
        Log.i(TAG, "sizeX: " + sizeX);
        int sizeY = Math.round(image.getIntrinsicHeight() * scaleFactor);
        Log.i(TAG, "sizeY: " + sizeY);
        Bitmap bitmapResized = Bitmap.createScaledBitmap(b, sizeX, sizeY, false);
        image = new BitmapDrawable(context.getResources(), bitmapResized);
        Log.i(TAG, "scaleImage2: " + image.getIntrinsicWidth() + "*" + image.getIntrinsicHeight());
        return image;
    }

    public Bitmap addBackground(Bitmap bitmap, boolean isFolder) {

        if (!hasTransparency(bitmap)) {
            bitmap = Bitmap.createScaledBitmap(bitmap, appIconWidth,
                    (appIconWidth * bitmap.getHeight() / bitmap.getWidth()),
                    true);
            return bitmap;
        }

        if (bitmap.getWidth() > appIconWidth) {
            bitmap = Bitmap.createScaledBitmap(bitmap, appIconWidth,
                    (appIconWidth * bitmap.getHeight() / bitmap.getWidth()),
                    true);
        }

        int width = appIconWidth;
        int height = width;
        Bitmap mergedBitmap = Bitmap.createBitmap(width, height, Bitmap
                .Config.ARGB_8888);
        Canvas canvas = new Canvas(mergedBitmap);
        canvas.drawColor(isFolder ? Color.WHITE
                : getDominantColor(bitmap));

        Paint paint = new Paint(
                Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(bitmap, (width - bitmap.getWidth()) / 2,
                (height - bitmap.getHeight()) / 2, paint);
        return mergedBitmap;
    }

    public Bitmap addBackground(Drawable appIcon, boolean isFolder) {
        BitmapDrawable bitmapDrawable;
        if(appIcon instanceof BitmapDrawable){
            bitmapDrawable = ((BitmapDrawable) appIcon);
        }else{
            Bitmap bitmap = Bitmap.createBitmap(appIcon.getIntrinsicWidth(), appIcon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            appIcon.draw(canvas);
            bitmapDrawable = new BitmapDrawable(mContext.getResources(), bitmap);
        }

        bitmapDrawable.setAntiAlias(true);
        Bitmap bitmap = bitmapDrawable.getBitmap();
        return addBackground(bitmap, isFolder);
    }

    public BitmapDrawable convertToRoundedCorner(Context context, Bitmap src) {
        return new BitmapDrawable(context.getResources(),
                BitmapUtils.getCroppedBitmap(src, deviceProfile.path));
    }

    public boolean hasTransparency(Bitmap bitmap) {
        for (int y = 0; y < bitmap.getWidth(); y++) {
            for (int x = 0; x < bitmap.getHeight(); x++) {
                if (Color.alpha(bitmap.getPixel(x, y)) < 255) {
                    return true;
                }
            }
        }

        return false;
    }


    /**
     * Finds the color with the most occurrences inside of a bitmap.
     *
     * @param drawable to get the dominant color of
     * @return the dominant color
     */
    public int getDominantColor(Drawable drawable){
        return getDominantColor(((BitmapDrawable)drawable).getBitmap());
    }

    /**
     * Finds the color with the most occurrences inside of a bitmap.
     *
     * @param bitmap the bitmap to get the dominant color of
     * @return the dominant color
     */
    public int getDominantColor(Bitmap bitmap) {
        Map<Integer, Integer> colors = new HashMap<>();

        int count = 0;

        for (int x = 0; x < bitmap.getWidth(); x++) {
            for (int y = 0; y < bitmap.getHeight(); y++) {
                if (Color.alpha(bitmap.getPixel(x, y)) == 255) {
                    int color = bitmap.getPixel(x, y);
                    colors.put(color, (colors.containsKey(color) ? colors.get(color) : 0) + 1);
                } else if ((Color.alpha(bitmap.getPixel(x, y)) < 0xF9)) {
                    count++;
                }
            }
        }

        int color = Color.TRANSPARENT;
        int occurrences = 0;
        if (colors.keySet().size() > 1) {
            for (Integer key : colors.keySet()) {
                if (colors.get(key) > occurrences) {
                    occurrences = colors.get(key);
                    color = key;
                }
            }

            return color;
        } else {
            return Color.WHITE;
        }
    }

}