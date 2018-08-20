/*
 * Copyright 2018 /e/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.indin.blisslaunchero.features.launcher;

import org.indin.blisslaunchero.BlissLauncher;
import org.indin.blisslaunchero.framework.DeviceProfile;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;

public class LauncherViewPager extends ViewPager {

    private Context mContext;

    public LauncherViewPager(@NonNull Context context) {
        super(context);
        this.mContext = context;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        DeviceProfile deviceProfile = BlissLauncher.getApplication(mContext).getDeviceProfile();
        setMeasuredDimension(deviceProfile.getAvailableWidthPx(), deviceProfile.getWorkspaceHeight());
    }
}
