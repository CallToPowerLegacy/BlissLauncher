package org.indin.blisslaunchero.framework;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;

import org.indin.blisslaunchero.BuildConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Utilities {

    private static final String TAG = "Utilities";

    public static final String AUTHORITY = (BuildConfig.APPLICATION_ID + ".settings").intern();

    /**
     * Use hard coded values to compile with android source.
     */
    public static final boolean ATLEAST_OREO =
            Build.VERSION.SDK_INT >= 26;

    public static final boolean ATLEAST_NOUGAT_MR1 =
            Build.VERSION.SDK_INT >= 25;

    public static final boolean ATLEAST_NOUGAT =
            Build.VERSION.SDK_INT >= 24;

    public static final boolean ATLEAST_MARSHMALLOW =
            Build.VERSION.SDK_INT >= 23;

    public static final boolean ATLEAST_LOLLIPOP_MR1 =
            Build.VERSION.SDK_INT >=22;

    /**
     * Compresses the bitmap to a byte array for serialization.
     */
    public static byte[] flattenBitmap(Bitmap bitmap) {
        // Try go guesstimate how much space the icon will take when serialized
        // to avoid unnecessary allocations/copies during the write.
        int size = bitmap.getWidth() * bitmap.getHeight() * 4;
        ByteArrayOutputStream out = new ByteArrayOutputStream(size);
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            return out.toByteArray();
        } catch (IOException e) {
            Log.w(TAG, "Could not write bitmap");
            return null;
        }
    }

    public static float dpiFromPx(int size, DisplayMetrics metrics){
        float densityRatio = (float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT;
        return (size / densityRatio);
    }
    public static int pxFromDp(float size, DisplayMetrics metrics) {
        return (int) Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                size, metrics));
    }

    public static int pxFromSp(float size, DisplayMetrics metrics) {
        return (int) Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                size, metrics));
    }

    /**
     * Calculates the height of a given string at a specific text size.
     */
    public static int calculateTextHeight(float textSizePx) {
        Paint p = new Paint();
        p.setTextSize(textSizePx);
        Paint.FontMetrics fm = p.getFontMetrics();
        return (int) Math.ceil(fm.bottom - fm.top);
    }

}
