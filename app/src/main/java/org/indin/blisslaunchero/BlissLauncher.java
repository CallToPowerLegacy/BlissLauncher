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
package org.indin.blisslaunchero;

import org.indin.blisslaunchero.features.launcher.AppProvider;
import org.indin.blisslaunchero.features.weather.DeviceStatusService;
import org.indin.blisslaunchero.features.weather.WeatherSourceListenerService;
import org.indin.blisslaunchero.features.weather.WeatherUpdateService;
import org.indin.blisslaunchero.features.weather.WeatherUtils;
import org.indin.blisslaunchero.framework.DeviceProfile;
import org.indin.blisslaunchero.framework.IconsHandler;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

public class BlissLauncher extends Application {
    private IconsHandler iconsPackHandler;
    private DeviceProfile deviceProfile;

    private AppProvider mAppProvider;

    private static final String TAG = "BlissLauncher";
    @Override
    public void onCreate() {
        super.onCreate();

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("Roboto-Regular.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build());

        if (WeatherUtils.isWeatherServiceAvailable(this)) {
            Log.i(TAG, "onCreate: weather avail ");
            startService(new Intent(this, WeatherSourceListenerService.class));
            startService(new Intent(this, DeviceStatusService.class));
            WeatherUpdateService.scheduleNextUpdate(this, true);
        }

    }

    public static BlissLauncher getApplication(Context context) {
        return (BlissLauncher) context.getApplicationContext();
    }

    public DeviceProfile getDeviceProfile(){
        if(deviceProfile == null){
            deviceProfile = new DeviceProfile(this);
        }
        return deviceProfile;
    }

    public IconsHandler getIconsHandler() {
        if (iconsPackHandler == null) {
            iconsPackHandler = new IconsHandler(this);
        }

        return iconsPackHandler;
    }

    public void resetIconsHandler(){
        iconsPackHandler = new IconsHandler(this);
    }

    public void initAppProvider(){
        connectAppProvider();
    }

    private void connectAppProvider() {
        Intent intent = new Intent(this, AppProvider.class);
        startService(intent);

        bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                AppProvider.LocalBinder localBinder = (AppProvider.LocalBinder) service;
                mAppProvider = localBinder.getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        }, Context.BIND_AUTO_CREATE);
    }

    public AppProvider getAppProvider(){
        if(mAppProvider == null){
            connectAppProvider();
        }
        return mAppProvider;
    }

}
