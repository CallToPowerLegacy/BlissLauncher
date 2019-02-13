package foundation.e.blisslauncher.core.customviews;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.view.MotionEvent;

import foundation.e.blisslauncher.R;

public class RoundedWidgetView extends AppWidgetHostView {

    private final Path stencilPath = new Path();
    private float cornerRadius = 0;

    private static final String TAG = "RoundedWidgetView";

    public RoundedWidgetView(Context context) {
        super(context);
        this.cornerRadius = context.getResources().getDimensionPixelSize(R.dimen.corner_radius);
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
        /*getParent().requestDisallowInterceptTouchEvent(true);
        int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_UP:
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
        }*/

        return super.onInterceptTouchEvent(ev);
    }
}
