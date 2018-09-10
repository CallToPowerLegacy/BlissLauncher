package org.indin.blisslaunchero.features.launcher;

import org.indin.blisslaunchero.BlissLauncher;
import org.indin.blisslaunchero.framework.DeviceProfile;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class PageIndicatorLinearLayout extends LinearLayout {
    private Context mContext;

    public PageIndicatorLinearLayout(Context context) {
        this(context, null);
    }

    public PageIndicatorLinearLayout(Context context,
            @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PageIndicatorLinearLayout(Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        DeviceProfile deviceProfile = BlissLauncher.getApplication(mContext).getDeviceProfile();
        setMeasuredDimension(deviceProfile.getAvailableWidthPx(),
                deviceProfile.getPageIndicatorHeight());
    }
}
