package org.indin.blisslaunchero.framework.util;

import android.graphics.Bitmap;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.renderscript.Type;

public class RSBlurProcessor {

    private RenderScript rs;

    private static final boolean IS_BLUR_SUPPORTED = Build.VERSION.SDK_INT >= 17;
    private static final int MAX_RADIUS = 25;

    public RSBlurProcessor(RenderScript rs) {
        this.rs = rs;
    }

    public Bitmap blur(Bitmap bitmap, float radius, int repeat) {

        if (!IS_BLUR_SUPPORTED) {
            return null;
        }

        if (radius > MAX_RADIUS) {
            radius = MAX_RADIUS;
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Create allocation type
        Type bitmapType = new Type.Builder(rs, Element.RGBA_8888(rs))
                .setX(width)
                .setY(height)
                .setMipmaps(false) // We are using MipmapControl.MIPMAP_NONE
                .create();

        // Create allocation
        Allocation allocation = Allocation.createTyped(rs, bitmapType);

        // Create blur script
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        blurScript.setRadius(radius);

        // Copy data to allocation
        allocation.copyFrom(bitmap);

        // set blur script input
        blurScript.setInput(allocation);

        // invoke the script to blur
        blurScript.forEach(allocation);

        // Repeat the blur for extra effect
        for (int i=0; i<repeat; i++) {
            blurScript.forEach(allocation);
        }

        // copy data back to the bitmap
        allocation.copyTo(bitmap);

        // release memory
        allocation.destroy();
        blurScript.destroy();
        allocation = null;
        blurScript = null;

        return bitmap;
    }
}
