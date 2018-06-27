package org.indin.blisslaunchero;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.indin.blisslaunchero.features.weather.DeviceStatusService;
import org.indin.blisslaunchero.features.weather.WeatherSourceListenerService;
import org.indin.blisslaunchero.features.weather.WeatherUpdateService;
import org.indin.blisslaunchero.features.weather.WeatherUtils;
import org.indin.blisslaunchero.framework.DeviceProfile;
import org.indin.blisslaunchero.framework.IconsHandler;
import org.indin.blisslaunchero.framework.Preferences;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

public class BlissLauncher extends Application {
    private IconsHandler iconsPackHandler;
    private DeviceProfile deviceProfile;

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

}
