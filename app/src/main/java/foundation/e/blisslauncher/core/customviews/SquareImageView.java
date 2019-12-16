package foundation.e.blisslauncher.core.customviews;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Created by falcon on 16/2/18.
 */

public class SquareImageView extends android.support.v7.widget.AppCompatImageView {

    public SquareImageView(Context context) {
        this(context, null);
    }

    public SquareImageView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        int size = width < height ? width : height;
        setMeasuredDimension(size, size);
    }
}