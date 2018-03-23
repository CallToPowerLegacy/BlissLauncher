package org.indin.blisslaunchero.notification;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

/**
 * Created by falcon on 20/3/18.
 */

public class DotRenderer {

    private static final String TAG = "DotRenderer";
    private static final float SIZE_PERCENTAGE = 0.25f;

    private final int mSize;
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG |
            Paint.FILTER_BITMAP_FLAG);

    public DotRenderer(int iconSizePx){
     this.mSize = (int) (SIZE_PERCENTAGE *iconSizePx);
    }

    public void drawDot(Canvas canvas, Rect iconBounds){
        Log.d(TAG, "drawDot() called with: canvas = [" + canvas + "], iconBounds = [" + iconBounds
                + "]");
        int badgeCenterX = (int) (iconBounds.left + mSize*0.25);
        int badgeCenterY = (int) (iconBounds.top + mSize*0.25);

        mPaint.setColor(Color.parseColor("#FF0800"));
        canvas.drawCircle(badgeCenterX, badgeCenterY, mSize/2, mPaint);
    }
}
