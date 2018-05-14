package org.indin.blisslaunchero.framework.customviews;

import android.content.Context;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.widget.AutoCompleteTextView;

public class BlissAutoCompleteTextView extends AutoCompleteTextView{
    public BlissAutoCompleteTextView(Context context) {
        super(context);
    }

    public BlissAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BlissAutoCompleteTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BlissAutoCompleteTextView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public boolean onDragEvent(DragEvent event) {
        // Without this drag/drop apps won't work on API <24.
        // EditTexts seem to interfere with drag/drop.
        return false;
    }
}
