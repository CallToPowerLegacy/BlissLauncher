package foundation.e.blisslauncher.core.customviews;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.ViewGroup;

import foundation.e.blisslauncher.R;
import foundation.e.blisslauncher.features.widgets.CheckLongPressHelper;

public class RoundedWidgetView extends AppWidgetHostView {

    private final Path stencilPath = new Path();
    private float cornerRadius;
    private CheckLongPressHelper mLongPressHelper;
    private static final String TAG = "RoundedWidgetView";

    public RoundedWidgetView(Context context) {
        super(context);
        this.cornerRadius = context.getResources().getDimensionPixelSize(R.dimen.corner_radius);
        mLongPressHelper = new CheckLongPressHelper(this);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // compute the path
        stencilPath.reset();
        stencilPath.addRoundRect(0, 0, w, h, cornerRadius, cornerRadius, Path.Direction.CW);
        stencilPath.close();

    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int save = canvas.save();
        canvas.clipPath(stencilPath);
        super.dispatchDraw(canvas);
        canvas.restoreToCount(save);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        // Consume any touch events for ourselves after longpress is triggered
        if (mLongPressHelper.hasPerformedLongPress()) {
            mLongPressHelper.cancelLongPress();
            return true;
        }

        // Watch for longpress events at this level to make sure
        // users can always pick up this widget
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                mLongPressHelper.postCheckForLongPress();
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mLongPressHelper.cancelLongPress();
                break;
        }

        // Otherwise continue letting touch events fall through to children
        return false;
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        mLongPressHelper.cancelLongPress();
    }

    @Override
    public int getDescendantFocusability() {
        return ViewGroup.FOCUS_BLOCK_DESCENDANTS;
    }
}
