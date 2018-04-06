package org.indin.blisslaunchero.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.indin.blisslaunchero.data.model.AppItem;
import org.indin.blisslaunchero.R;
import org.indin.blisslaunchero.ui.LauncherActivity;
import org.indin.blisslaunchero.widgets.AdaptiveIconUtils;
import org.indin.blisslaunchero.widgets.PathParser;

public class GraphicsUtil {

    private static final String TAG = "BLISS_GRAPHICS";

    public GraphicsUtil() {
    }
    
    /**
     * Takes 1 or more drawables and merges them to form a single Drawable.
     * However, if more than 4 drawables are provided, only the first 4 are used.
     */
    public Drawable generateFolderIcon(Context context, Drawable... sources) {
        int width = sources[0].getIntrinsicWidth();
        int height = width; // Square icons

        Log.i(TAG, "generateFolderIcon: "+width+"*"+height);
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
    public static Drawable scaleImage(Context context, Drawable image, float scaleFactor) {
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

    public static Drawable maskImage(Context context, Drawable image) {
        if ((image == null) || !(image instanceof BitmapDrawable)) {
            return image;
        }
        double scale = 1;
        Drawable maskDrawable;
        if (IconPackUtil.getIconMask() != null) {
            maskDrawable = IconPackUtil.getIconMask();
        } else {
            maskDrawable = ContextCompat.getDrawable(context, R.drawable.iconmask);
        }
        Bitmap orig_mask = ((BitmapDrawable) maskDrawable).getBitmap();
        Bitmap mask = Bitmap.createScaledBitmap(orig_mask,
                image.getIntrinsicWidth(),
                image.getIntrinsicHeight(), true);
        Log.i(TAG, "maskImage: " + mask.getHeight() + "*" + mask.getWidth());
        Bitmap original = ((BitmapDrawable) image).getBitmap();
        Bitmap bmp = original.copy(Bitmap.Config.RGB_565, true);


        Bitmap result = Bitmap.createBitmap(mask.getWidth(), mask.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        Paint paint = new Paint(
                Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
        paint.setColor(0XFF000000);
        RectF rectf = new RectF(0, 0, mask.getWidth(), mask.getHeight());
        int rx = (int) ConverterUtil.dp2Px(10, context);
        canvas.drawRoundRect(rectf, rx, rx, paint);


        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        /*Shader shader = new BitmapShader(original, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        paint.setShader(shader);*/
        canvas.drawBitmap(original, 0, 0, paint);

        return new BitmapDrawable(context.getResources(), result);
    }
    
    public Bitmap addBackground(Bitmap bitmap, boolean isFolder){
        if (bitmap.getWidth() > LauncherActivity.appIconWidth) {
            bitmap = Bitmap.createScaledBitmap(bitmap, LauncherActivity.appIconWidth,
                    (LauncherActivity.appIconWidth * bitmap.getHeight() / bitmap.getWidth()),
                    true);
        }
        if (!ImageUtils.hasTransparency(bitmap)) {
            return bitmap;
        }

        int width = LauncherActivity.appIconWidth;
        int height = width;
        Bitmap mergedBitmap = Bitmap.createBitmap(width, height, Bitmap
                .Config.ARGB_8888);
        Canvas canvas = new Canvas(mergedBitmap);
        canvas.drawColor(isFolder ? Color.WHITE
                : ImageUtils.getDominantColor(bitmap));

        Paint paint = new Paint(
                Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(bitmap, (width - bitmap.getWidth()) / 2,
                (height - bitmap.getHeight()) / 2, paint);
        return mergedBitmap;
    }

    public Bitmap addBackground(Drawable appIcon, boolean isFolder) {
        Bitmap bitmap = ((BitmapDrawable) appIcon).getBitmap();
        return addBackground(bitmap, isFolder);
    }

    public BitmapDrawable convertToRoundedCorner(Context context, Bitmap src) {
        return new BitmapDrawable(context.getResources(),
                BitmapUtils.getCroppedBitmap(src, this.getRoundedCornerPath(src)));
    }

    private Path getRoundedCornerPath(Bitmap src) {
        return resizePath(PathParser.createPathFromPathData(AdaptiveIconUtils.getMaskPath()),
                src.getWidth(), src.getHeight());
    }

    private Path resizePath(Path path, int width, int height) {
        RectF bounds = new RectF(0, 0, width, height);
        Path resizedPath = new Path(path);
        RectF src = new RectF();
        resizedPath.computeBounds(src, true);

        Matrix resizeMatrix = new Matrix();
        resizeMatrix.setRectToRect(src, bounds, Matrix.ScaleToFit.CENTER);
        resizedPath.transform(resizeMatrix);

        return resizedPath;
    }
}