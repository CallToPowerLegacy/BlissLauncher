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
package org.indin.blisslaunchero.framework.customviews;

import org.indin.blisslaunchero.BlissLauncher;
import org.indin.blisslaunchero.features.notification.DotRenderer;
import org.indin.blisslaunchero.framework.DeviceProfile;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Property;
import android.widget.FrameLayout;

/**
 * Created by falcon on 20/3/18.
 */

public class BlissFrameLayout extends FrameLayout {

    private static final String TAG = "BlissFrameLayout";
    private final Context mContext;

    private boolean hasBadge = false;
    private Rect mTempIconBounds = new Rect();
    private DeviceProfile mDeviceProfile;

    private float mBadgeScale;

    private static final Property<BlissFrameLayout, Float> BADGE_SCALE_PROPERTY
            = new Property<BlissFrameLayout, Float>(Float.TYPE, "badgeScale") {
        @Override
        public Float get(BlissFrameLayout bubbleTextView) {
            return bubbleTextView.mBadgeScale;
        }

        @Override
        public void set(BlissFrameLayout bubbleTextView, Float value) {
            bubbleTextView.mBadgeScale = value;
            bubbleTextView.invalidate();
        }
    };
    private int mIconSize;
    private boolean mWithText;
    private DotRenderer mDotRenderer;

    public BlissFrameLayout(Context context) {
        this(context, null, 0);
    }

    public BlissFrameLayout(Context context,
            AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BlissFrameLayout(Context context, AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        init();

    }

    private void init() {
        setWillNotDraw(false);
        mDeviceProfile = BlissLauncher.getApplication(mContext).getDeviceProfile();
        mDotRenderer = new DotRenderer(mContext, mDeviceProfile.iconSizePx);
        mIconSize = mDeviceProfile.iconSizePx;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        drawBadgeIfNecessary(canvas);
    }

    private void drawBadgeIfNecessary(Canvas canvas) {
        if (hasBadge) {
            getIconBounds(mTempIconBounds);
            mDotRenderer.drawDot(canvas, mTempIconBounds);
        }
    }

    public void setWithText(boolean withText){
        this.mWithText = withText;
    }

    private void getIconBounds(Rect outBounds) {
        int cellHeightPx;
        if (mWithText) {
            cellHeightPx = mDeviceProfile.cellHeightWithoutPaddingPx;
        } else {
            cellHeightPx = mDeviceProfile.hotseatCellHeightWithoutPaddingPx;
        }
        int top = (getHeight() - cellHeightPx) / 2;
        int left = (getWidth() - mIconSize) / 2;
        int right = left + mIconSize;
        int bottom = top + mIconSize;
        outBounds.set(left, top, right, bottom);
    }

    public void applyBadge(boolean isBadge, boolean withText) {
        mWithText = withText;
        boolean wasBadged = hasBadge;
        hasBadge = isBadge;
        boolean isBadged = hasBadge;
        float newBadgeScale = isBadge ? 1f : 0;

        if ((wasBadged ^ isBadged)) {
            if (isShown()) {
                ObjectAnimator.ofFloat(this, BADGE_SCALE_PROPERTY, newBadgeScale).start();
            } else {
                mBadgeScale = newBadgeScale;
                invalidate();
            }
        }
    }
}
