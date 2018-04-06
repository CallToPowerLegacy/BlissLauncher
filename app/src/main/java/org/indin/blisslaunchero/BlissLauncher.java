package org.indin.blisslaunchero;

import android.app.Application;

import com.bugsnag.android.Bugsnag;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

public class BlissLauncher extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("Roboto-Regular.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build());
        Bugsnag.init(this);
    }
}
