package org.indin.blisslaunchero.features.launcher;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.widget.NestedScrollView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class CustomScrollVIew extends NestedScrollView {

    private static final String TAG = "CustomScrollVIew";

    private View nestedScrollTarget;
    private boolean nestedScrollTargetIsBeingDragged = false;
    private boolean nestedScrollTargetWasUnableToScroll = false;
    private boolean skipsTouchInterception = false;

    public CustomScrollVIew(Context context) {
        super(context);
    }

    public CustomScrollVIew(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomScrollVIew(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        Log.d(TAG, "dispatchTouchEvent() called with: ev = [" + ev + "]");
        boolean tempSkipsInterception = nestedScrollTarget != null;
        if(tempSkipsInterception){
            skipsTouchInterception = true;
        }

        boolean handled = super.dispatchTouchEvent(ev);
        if(tempSkipsInterception){
            skipsTouchInterception = false;

            if(!handled || nestedScrollTargetWasUnableToScroll){
                handled = super.dispatchTouchEvent(ev);
            }
        }
        return handled;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        Log.d(TAG, "onInterceptTouchEvent() called with: ev = [" + ev + "]");
        return !skipsTouchInterception && super.onInterceptTouchEvent(ev);
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed,
            int dyUnconsumed) {
        Log.d(TAG, "onNestedScroll() called with: target = [" + target + "], dxConsumed = ["
                + dxConsumed + "], dyConsumed = [" + dyConsumed + "], dxUnconsumed = ["
                + dxUnconsumed + "], dyUnconsumed = [" + dyUnconsumed + "]");
        if (target == nestedScrollTarget && !nestedScrollTargetIsBeingDragged) {
            if (dyConsumed != 0) {
                // The descendent was actually scrolled, so we won't bother it any longer.
                // It will receive all future events until it finished scrolling.
                nestedScrollTargetIsBeingDragged = true;
                nestedScrollTargetWasUnableToScroll = false;
            }
            else if (dyUnconsumed != 0) {
                // The descendent tried scrolling in response to touch movements but was not able to do so.
                // We remember that in order to allow RecyclerView to take over scrolling.
                nestedScrollTargetWasUnableToScroll = true;
                target.getParent().requestDisallowInterceptTouchEvent(false);
            }
        }
    }

    @Override
    public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes) {
        Log.d(TAG,
                "onNestedScrollAccepted() called with: child = [" + child + "], target = [" + target
                        + "], axes = [" + axes + "]");
        if((axes & View.SCROLL_AXIS_VERTICAL) != 0){
            nestedScrollTarget = target;
            nestedScrollTargetIsBeingDragged = false;
            nestedScrollTargetWasUnableToScroll = false;
        }

        super.onNestedScrollAccepted(child, target, axes);
    }

    @Override
    public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes,
            int type) {
        Log.d(TAG, "onStartNestedScroll() called with: child = [" + child + "], target = [" + target
                + "], axes = [" + axes + "], type = [" + type + "]");
        return (axes & View.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onStopNestedScroll(View target) {
        Log.d(TAG, "onStopNestedScroll() called with: target = [" + target + "]");
        nestedScrollTarget = null;
        nestedScrollTargetIsBeingDragged = false;
        nestedScrollTargetWasUnableToScroll = false;
    }
}
