package org.indin.blisslaunchero.framework.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

public class BitmapUtils {

    public static Bitmap getCroppedBitmap(Bitmap src, Path path){
        Bitmap output = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint();
        paint.setDither(true);
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setColor(0XFF000000);

        canvas.drawPath(path, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(src, 0, 0, paint);
        return output;
    }
}
