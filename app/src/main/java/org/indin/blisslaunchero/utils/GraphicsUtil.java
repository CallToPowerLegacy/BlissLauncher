package org.indin.blisslaunchero.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.indin.blisslaunchero.model.AppItem;
import org.indin.blisslaunchero.R;

public class GraphicsUtil {

    private static final String TAG = "BLISS_GRAPHICS";

    public static Bitmap blur(Context context, Bitmap image) {
        int width = Math.round(image.getWidth() * 0.2f);
        int height = Math.round(image.getHeight() * 0.2f);

        Bitmap input = Bitmap.createScaledBitmap(image, width, height, false);
        Bitmap output = Bitmap.createBitmap(input);

        RenderScript rs = RenderScript.create(context);
        ScriptIntrinsicBlur theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        Allocation allocInput = Allocation.createFromBitmap(rs, input);
        Allocation allocOutput = Allocation.createFromBitmap(rs, output);
        theIntrinsic.setRadius(7);
        theIntrinsic.setInput(allocInput);
        theIntrinsic.forEach(allocOutput);
        allocOutput.copyTo(output);

        return output;
    }

    /**
     * Takes 1 or more drawables and merges them to form a single Drawable.
     * However, if more than 4 drawables are provided, only the first 4 are used.
     */
    public static Drawable generateFolderIcon(Context context, Drawable... sources) {
        for (Drawable d : sources) {
            if (!(d instanceof BitmapDrawable)) {
                Log.d(TAG, "Unknown type of icon found");
                return context.getResources().getDrawable(R.mipmap.ic_folder, null);
            }
        }
        int width = sources[0].getIntrinsicWidth();
        int height = width; // Square icons
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        int xOrigin = bitmap.getWidth() / 20;
        int yOrigin = bitmap.getHeight() / 20;
        int x = xOrigin;
        int y = yOrigin;
        int xIncrement = bitmap.getWidth() / 10;
        int yIncrement = bitmap.getHeight() / 10;
        int count = 0;
        int total = 0;
        for (Drawable d : sources) {
            BitmapDrawable bd = (BitmapDrawable) d;
            bd.setBounds(x, y, (int) (x + width / 2.5f), (int) (y + height / 2.5f));
            bd.draw(canvas);
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

        Drawable output = new BitmapDrawable(context.getResources(), bitmap);
        if (IconPackUtil.iconPackPresent) {
            output = GraphicsUtil.scaleImage(context, output, 1f);
            output = GraphicsUtil.maskImage(context, output);
        }
        return output;
    }

    /**
     * A utility method that simplifies calls to the generateFolderIcon() method that
     * expects an array of Drawables.
     */
    public static Drawable generateFolderIcon(Context context, AppItem app) {
        Drawable[] drawables = new Drawable[app.getSubApps().size()];
        for (int i = 0; i < app.getSubApps().size(); i++) {
            drawables[i] = app.getSubApps().get(i).getIcon();
        }
        return generateFolderIcon(context, drawables);
    }

    /**
     * Scales icons to match the icon pack
     */
    public static Drawable scaleImage(Context context, Drawable image, float scaleFactor) {
        if ((image == null) || !(image instanceof BitmapDrawable)) {
            return image;
        }
        Bitmap b = ((BitmapDrawable) image).getBitmap();
        int sizeX = Math.round(image.getIntrinsicWidth() * scaleFactor);
        int sizeY = Math.round(image.getIntrinsicHeight() * scaleFactor);
        Bitmap bitmapResized = Bitmap.createScaledBitmap(b, sizeX, sizeY, false);
        image = new BitmapDrawable(context.getResources(), bitmapResized);
        return image;
    }

    public static Drawable maskImage(Context context, Drawable image) {
        if ((image == null) || !(image instanceof BitmapDrawable)) {
            return image;
        }
        double scale = 0.85;
        Drawable maskDrawable;
        if(IconPackUtil.getIconMask() != null){
            maskDrawable = IconPackUtil.getIconMask();
        }else
            maskDrawable = ContextCompat.getDrawable(context, R.drawable.iconmask);
        Bitmap mask = ((BitmapDrawable) maskDrawable).getBitmap();
        Bitmap original = Bitmap.createScaledBitmap(((BitmapDrawable) image).getBitmap(),
                (int) (mask.getWidth() * scale),
                (int) (mask.getHeight() * scale), true);


        Bitmap result = Bitmap.createBitmap(mask.getWidth(), mask.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        canvas.drawBitmap(original, (canvas.getWidth() - original.getWidth()) / 2,
                (canvas.getHeight() - original.getHeight()) / 2, null);
        canvas.drawBitmap(mask, 0, 0, paint);
        paint.setXfermode(null);
        return new BitmapDrawable(context.getResources(), result);
    }
}