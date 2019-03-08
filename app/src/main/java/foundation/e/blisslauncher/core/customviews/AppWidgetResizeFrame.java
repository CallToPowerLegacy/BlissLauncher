package foundation.e.blisslauncher.core.customviews;

import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;

import foundation.e.blisslauncher.BlissLauncher;
import foundation.e.blisslauncher.R;

public class AppWidgetResizeFrame extends FrameLayout {
    private RoundedWidgetView mRoundedWidgetView;

    private ImageView mTopHandle;
    private ImageView mBottomHandle;

    private boolean mTopBorderActive;
    private boolean mBottomBorderActive;
    private int mWidgetPaddingTop;
    private int mWidgetPaddingBottom;

    private int mBaselineWidth;
    private int mBaselineHeight;
    private int mBaselineX;
    private int mBaselineY;
    private int mResizeMode;

    private int mRunningHInc;
    private int mRunningVInc;
    private int mMinHeight;
    private int mDeltaX;
    private int mDeltaY;
    private int mDeltaXAddOn;
    private int mDeltaYAddOn;

    private int mBackgroundPadding;
    private int mTouchTargetWidth;

    private int mTopTouchRegionAdjustment = 0;
    private int mBottomTouchRegionAdjustment = 0;

    int[] mDirectionVector = new int[2];
    int[] mLastDirectionVector = new int[2];

    final int SNAP_DURATION = 150;
    final int BACKGROUND_PADDING = 24;
    final float DIMMED_HANDLE_ALPHA = 0f;
    final float RESIZE_THRESHOLD = 0.66f;

    private static Rect mTmpRect = new Rect();

    public static final int TOP = 1;
    public static final int BOTTOM = 3;

    private Context mContext;


    public AppWidgetResizeFrame(@NonNull Context context, RoundedWidgetView widgetView) {
        super(context);
        mRoundedWidgetView = widgetView;
        mContext = context;

        final AppWidgetProviderInfo info = widgetView.getAppWidgetInfo();
        Rect padding = AppWidgetHostView.getDefaultPaddingForWidget(context, info.provider, null);
        // We want to account for the extra amount of padding that we are adding to the widget
        // to ensure that it gets the full amount of space that it has requested
        mMinHeight = info.minHeight + padding.top + padding.bottom;

        setBackgroundResource(R.drawable.widget_resize_frame);
        setPadding(0, 0, 0, 0);

        LayoutParams lp;
        mTopHandle = new ImageView(context);
        mTopHandle.setImageResource(R.drawable.widget_resize_handle_top);
        lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        addView(mTopHandle, lp);

        mBottomHandle = new ImageView(context);
        mBottomHandle.setImageResource(R.drawable.widget_resize_handle_bottom);
        lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        addView(mBottomHandle, lp);

        Rect p = AppWidgetHostView.getDefaultPaddingForWidget(context,
                widgetView.getAppWidgetInfo().provider, null);
        mWidgetPaddingTop = p.top;
        mWidgetPaddingBottom = p.bottom;

        if (mResizeMode == AppWidgetProviderInfo.RESIZE_HORIZONTAL) {
            mTopHandle.setVisibility(GONE);
            mBottomHandle.setVisibility(GONE);
        }

        final float density = context.getResources().getDisplayMetrics().density;
        mBackgroundPadding = (int) Math.ceil(density * BACKGROUND_PADDING);
        mTouchTargetWidth = 2 * mBackgroundPadding;
    }

    public boolean beginResizeIfPointInRegion(int x, int y) {
        boolean horizontalActive = (mResizeMode & AppWidgetProviderInfo.RESIZE_HORIZONTAL) != 0;
        boolean verticalActive = (mResizeMode & AppWidgetProviderInfo.RESIZE_VERTICAL) != 0;

        mTopBorderActive = (y < mTouchTargetWidth + mTopTouchRegionAdjustment) && verticalActive;
        mBottomBorderActive = (y > getHeight() - mTouchTargetWidth + mBottomTouchRegionAdjustment)
                && verticalActive;

        boolean anyBordersActive = mTopBorderActive || mBottomBorderActive;

        mBaselineWidth = getMeasuredWidth();
        mBaselineHeight = getMeasuredHeight();
        mBaselineX = getLeft();
        mBaselineY = getTop();

        if (anyBordersActive) {
            mTopHandle.setAlpha(mTopBorderActive ? 1.0f : DIMMED_HANDLE_ALPHA);
            mBottomHandle.setAlpha(mBottomBorderActive ? 1.0f : DIMMED_HANDLE_ALPHA);
        }
        return anyBordersActive;
    }

    /**
     * Here we bound the deltas such that the frame cannot be stretched beyond the extents
     * of the CellLayout, and such that the frame's borders can't cross.
     */
    public void updateDeltas(int deltaX, int deltaY) {
        if (mTopBorderActive) {
            mDeltaY = Math.max(-mBaselineY, deltaY);
            mDeltaY = Math.min(mBaselineHeight - 2 * mTouchTargetWidth, mDeltaY);
        } else if (mBottomBorderActive) {
            mDeltaY = Math.min(BlissLauncher.getApplication(
                    mContext).getDeviceProfile().availableHeightPx - (mBaselineY + mBaselineHeight),
                    deltaY);
            mDeltaY = Math.max(-mBaselineHeight + 2 * mTouchTargetWidth, mDeltaY);
        }
    }

    public void visualizeResizeForDelta(int deltaX, int deltaY) {
        visualizeResizeForDelta(deltaX, deltaY, false);
    }

    /**
     * Based on the deltas, we resize the frame, and, if needed, we resize the widget.
     */
    private void visualizeResizeForDelta(int deltaX, int deltaY, boolean onDismiss) {
        updateDeltas(deltaX, deltaY);
       /* DragLayer.LayoutParams lp = (DragLayer.LayoutParams) getLayoutParams();

        if (mTopBorderActive) {
            lp.y = mBaselineY + mDeltaY;
            lp.height = mBaselineHeight - mDeltaY;
        } else if (mBottomBorderActive) {
            lp.height = mBaselineHeight + mDeltaY;
        }

        resizeWidgetIfNeeded(onDismiss);*/
        requestLayout();
    }

    /**
     * This is the final step of the resize. Here we save the new widget size and position
     * to LauncherModel and animate the resize frame.
     */
    public void commitResize() {
        //resizeWidgetIfNeeded(true);
        requestLayout();
    }
}
