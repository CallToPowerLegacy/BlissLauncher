package org.indin.blisslaunchero.framework.customviews;

import android.graphics.Canvas;
import android.graphics.Point;
import android.view.View;

/**
 * Created by falcon on 15/2/18.
 */

public class BlissDragShadowBuilder extends View.DragShadowBuilder {

    private final int mX;
    private final int mY;
    private Point mScaleFactor;
    // Defines the constructor for myDragShadowBuilder
    public BlissDragShadowBuilder(View v, float x, float y) {

        // Stores the View parameter passed to myDragShadowBuilder.
        super(v);


        mX = (int) x;
        mY = (int) y;
    }

    // Defines a callback that sends the drag shadow dimensions and touch point back to the
    // system.
    @Override
    public void onProvideShadowMetrics (Point size, Point touch) {
        // Defines local variables
        int width;
        int height;

        // Sets the width of the shadow to half the width of the original View
        width = (int) (getView().getWidth() * 1);

        // Sets the height of the shadow to half the height of the original View
        height = (int) (getView().getHeight() * 1);

        // Sets the size parameter's width and height values. These get back to the system
        // through the size parameter.
        size.set(width, height);
        // Sets size parameter to member that will be used for scaling shadow image.
        mScaleFactor = size;

        // Sets the touch point's position to be in the middle of the drag shadow
        touch.set(mX, mY);
    }

    @Override
    public void onDrawShadow(Canvas canvas) {

        // Draws the ColorDrawable in the Canvas passed in from the system.
        canvas.scale(mScaleFactor.x/(float)getView().getWidth(), mScaleFactor.y/(float)getView().getHeight());
        getView().draw(canvas);
    }

}
