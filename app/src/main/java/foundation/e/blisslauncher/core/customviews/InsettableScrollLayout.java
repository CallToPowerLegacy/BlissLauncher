package foundation.e.blisslauncher.core.customviews;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class InsettableScrollLayout extends ScrollView {

    public InsettableScrollLayout(@NonNull Context context) {
        super(context);
    }

    public InsettableScrollLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public InsettableScrollLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
