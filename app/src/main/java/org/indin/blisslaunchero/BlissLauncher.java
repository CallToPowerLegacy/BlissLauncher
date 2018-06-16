package org.indin.blisslaunchero;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import org.indin.blisslaunchero.features.weather.WeatherUpdateService;
import org.indin.blisslaunchero.framework.DeviceProfile;
import org.indin.blisslaunchero.framework.IconsHandler;
import org.indin.blisslaunchero.framework.Preferences;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

public class BlissLauncher extends Application {
    private IconsHandler iconsPackHandler;
    private DeviceProfile deviceProfile;

    @Override
    public void onCreate() {
        super.onCreate();

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("Roboto-Regular.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build());


        Intent intent = new Intent(this, WeatherUpdateService.class);
        intent.setAction(WeatherUpdateService.ACTION_FORCE_UPDATE);
        startService(intent);

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

}
