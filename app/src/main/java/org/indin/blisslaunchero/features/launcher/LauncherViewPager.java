package org.indin.blisslaunchero.features.launcher;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;

import org.indin.blisslaunchero.framework.DeviceProfile;

public class LauncherViewPager extends ViewPager {

    private Context mContext;

    public LauncherViewPager(@NonNull Context context) {
        super(context);
        this.mContext = context;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        DeviceProfile deviceProfile = LauncherActivity.getLauncher(mContext).getDeviceProfile();
        setMeasuredDimension(deviceProfile.getAvailableWidthPx(), deviceProfile.getWorkspaceHeight());
    }
}
