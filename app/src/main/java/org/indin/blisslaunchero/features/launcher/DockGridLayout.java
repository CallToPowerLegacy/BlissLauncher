package org.indin.blisslaunchero.features.launcher;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridLayout;

import org.indin.blisslaunchero.BlissLauncher;
import org.indin.blisslaunchero.framework.DeviceProfile;

public class DockGridLayout extends GridLayout {

    private Context mContext;

    public DockGridLayout(Context context) {
        this(context, null);
    }

    public DockGridLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DockGridLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        DeviceProfile deviceProfile = BlissLauncher.getApplication(mContext).getDeviceProfile();
        setMeasuredDimension(deviceProfile.getAvailableWidthPx(), deviceProfile.hotseatCellHeightPx);
    }
}
