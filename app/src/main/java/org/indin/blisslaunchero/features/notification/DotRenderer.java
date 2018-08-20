/*
 * Copyright 2018 /e/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.indin.blisslaunchero.features.notification;

import org.indin.blisslaunchero.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

/**
 * Created by falcon on 20/3/18.
 */

public class DotRenderer {

    private static final String TAG = "DotRenderer";
    private static final float SIZE_PERCENTAGE = 0.3375f;

    private final Context mContext;
    private final int mSize;
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG |
            Paint.FILTER_BITMAP_FLAG);

    public DotRenderer(Context context, int iconSizePx) {
        mContext = context;
        this.mSize = (int) (SIZE_PERCENTAGE * iconSizePx);
    }

    public void drawDot(Canvas canvas, Rect iconBounds) {
        Bitmap myBitmap = BitmapFactory.decodeResource(
                mContext.getResources(),
                R.drawable.notification_icon_72);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(myBitmap, mSize,mSize, true);

        canvas.drawBitmap(scaledBitmap, iconBounds.left - scaledBitmap.getWidth() / 2,
                iconBounds.top - scaledBitmap.getHeight() / 2, mPaint);
        //canvas.drawCircle(badgeCenterX, badgeCenterY, mSize/2, mPaint);
    }
}
