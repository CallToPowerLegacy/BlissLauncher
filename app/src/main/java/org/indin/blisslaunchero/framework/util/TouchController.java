package org.indin.blisslaunchero.framework.util;

import android.view.MotionEvent;

public interface TouchController {

    /**
     * Called when the draglayer receives touch event.
     */
    boolean onControllerTouchEvent(MotionEvent ev);

    /**
     * Called when the draglayer receives a intercept touch event.
     */
    boolean onControllerInterceptTouchEvent(MotionEvent ev);
}