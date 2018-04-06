package org.indin.blisslaunchero.notification;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import org.indin.blisslaunchero.R;

/**
 * Created by falcon on 20/3/18.
 */

public class DotRenderer {

    private static final String TAG = "DotRenderer";
    private static final float SIZE_PERCENTAGE = 0.35f;

    private final Context mContext;
    private final int mSize;
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG |
            Paint.FILTER_BITMAP_FLAG);

    public DotRenderer(Context context, int iconSizePx) {
        mContext = context;
        this.mSize = (int) (SIZE_PERCENTAGE * iconSizePx);
    }

    public void drawDot(Canvas canvas, Rect iconBounds) {
        Log.d(TAG, "drawDot() called with: canvas = [" + canvas + "], iconBounds = [" + iconBounds
                + "]");
        int badgeCenterX = (int) (iconBounds.left + mSize * 0.25);
        int badgeCenterY = (int) (iconBounds.top + mSize * 0.25);

        Bitmap myBitmap = BitmapFactory.decodeResource(
                mContext.getResources(),
                R.drawable.notification_icon_72);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(myBitmap, mSize,mSize, true);

        canvas.drawBitmap(scaledBitmap, iconBounds.left - scaledBitmap.getWidth() / 3,
                iconBounds.top - scaledBitmap.getHeight() / 3, mPaint);
        //canvas.drawCircle(badgeCenterX, badgeCenterY, mSize/2, mPaint);
    }
}
