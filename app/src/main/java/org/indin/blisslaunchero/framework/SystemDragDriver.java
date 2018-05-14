package org.indin.blisslaunchero.framework;

import android.view.DragEvent;
import android.view.View;

/**
 * Created by falcon on 16/4/18.
 */

public class SystemDragDriver implements View.OnDragListener{

    protected final EventListener mEventListener;
    private float mLastY = 0;
    private float mLastX = 0;

    public SystemDragDriver(
            EventListener eventListener) {
        mEventListener = eventListener;
    }

    @Override
    public boolean onDrag(View v, DragEvent event) {
        final int action = event.getAction();

        switch (action) {
            case DragEvent.ACTION_DRAG_STARTED:
                mLastX = event.getX();
                mLastY = event.getY();
                return true;

            case DragEvent.ACTION_DRAG_ENTERED:
                return true;

            case DragEvent.ACTION_DRAG_LOCATION:
                mLastX = event.getX();
                mLastY = event.getY();
                mEventListener.onDriverDragMove(event.getX(), event.getY());
                return true;

            case DragEvent.ACTION_DROP:
                mLastX = event.getX();
                mLastY = event.getY();
                mEventListener.onDriverDragMove(event.getX(), event.getY());
                mEventListener.onDriverDragEnd(mLastX, mLastY);
                return true;
            case DragEvent.ACTION_DRAG_EXITED:
                mEventListener.onDriverDragExitWindow();
                return true;

            case DragEvent.ACTION_DRAG_ENDED:
                mEventListener.onDriverDragCancel();
                return true;

            default:
                return false;
        }
    }

    public interface EventListener {
        void onDriverDragMove(float x, float y);

        void onDriverDragExitWindow();

        void onDriverDragEnd(float x, float y);

        void onDriverDragCancel();
    }

}
