package foundation.e.blisslauncher.core.customviews;

import android.content.Context;
import android.util.AttributeSet;
import android.view.WindowInsets;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class InsettableScrollLayout extends ScrollView implements Insettable {

    public InsettableScrollLayout(@NonNull Context context) {
        super(context);
    }

    public InsettableScrollLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public InsettableScrollLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setInsets(WindowInsets insets) {
        int top = getPaddingTop();
        int left = getPaddingLeft();
        int right = getPaddingRight();
        int bottom = getPaddingBottom();
        setPadding(left, top, right, bottom + insets.getSystemWindowInsetBottom());
    }
}
