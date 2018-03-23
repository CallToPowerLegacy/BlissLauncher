package org.indin.blisslaunchero.widgets;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Property;
import android.widget.FrameLayout;

import org.indin.blisslaunchero.notification.DotRenderer;
import org.indin.blisslaunchero.ui.LauncherActivity;
import org.indin.blisslaunchero.utils.ConverterUtil;

/**
 * Created by falcon on 20/3/18.
 */

public class BlissFrameLayout extends FrameLayout {

    private static final String TAG = "BlissFrameLayout";
    private final Context mContext;

    private boolean hasBadge = false;
    private Rect mTempIconBounds = new Rect();

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
    private Paint mPaint;
    private boolean mWithText;

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
        mPaint = new Paint();
        mPaint.setTypeface(Typeface.SANS_SERIF);
        mPaint.setTextSize(ConverterUtil.spToPx(LauncherActivity.labelTextSizeSp, mContext));
        mIconSize = LauncherActivity.appIconWidth;
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
            DotRenderer dotRenderer = new DotRenderer(LauncherActivity.appIconWidth);
            dotRenderer.drawDot(canvas, mTempIconBounds);
        }
    }

    private void getIconBounds(Rect outBounds) {
        int cellHeightPx;
        if (mWithText) {
            Paint.FontMetrics fm = mPaint.getFontMetrics();
            cellHeightPx = mIconSize + (int) Math.ceil(fm.bottom - fm.top);
        } else {
            cellHeightPx = mIconSize;
        }
        int top = (getHeight() - cellHeightPx) / 2;
        int left = (getWidth() - mIconSize) / 2;
        int right = left + mIconSize;
        int bottom = top + mIconSize;
        Log.i(TAG, "top: " + top + " left: " + left + " right: " + right + " bottom: " + bottom);
        outBounds.set(left, top, right, bottom);
    }

    public void applyBadge(boolean isBadge, boolean withText) {
        Log.d(TAG, "applyBadge() called with: isBadge = [" + isBadge + "]");
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
