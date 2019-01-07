package foundation.e.blisslauncher.features.launcher;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridLayout;

import foundation.e.blisslauncher.BlissLauncher;
import foundation.e.blisslauncher.core.DeviceProfile;

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
        setMeasuredDimension(deviceProfile.getAvailableWidthPx(),
                deviceProfile.hotseatCellHeightPx);
        this.setPadding(deviceProfile.iconDrawablePaddingPx / 2, 0,
                deviceProfile.iconDrawablePaddingPx / 2, 0);
    }
}
