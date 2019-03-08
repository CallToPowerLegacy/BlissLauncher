package foundation.e.blisslauncher;

import android.app.Application;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import foundation.e.blisslauncher.core.DeviceProfile;
import foundation.e.blisslauncher.core.IconsHandler;
import foundation.e.blisslauncher.core.customviews.WidgetHost;
import foundation.e.blisslauncher.features.launcher.AppProvider;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

public class BlissLauncher extends Application {
    private IconsHandler iconsPackHandler;
    private DeviceProfile deviceProfile;

    private AppProvider mAppProvider;

    private static WidgetHost sAppWidgetHost;
    private static AppWidgetManager sAppWidgetManager;

    private static int sLongPressTimeout = 300;

    private static final String TAG = "BlissLauncher";

    @Override
    public void onCreate() {
        super.onCreate();

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("Roboto-Regular.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build());

        sAppWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        sAppWidgetHost = new WidgetHost(getApplicationContext(),
                R.id.APPWIDGET_HOST_ID);
        sAppWidgetHost.startListening();
    }

    public static BlissLauncher getApplication(Context context) {
        return (BlissLauncher) context.getApplicationContext();
    }

    public DeviceProfile getDeviceProfile() {
        if (deviceProfile == null) {
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

    public void resetIconsHandler() {
        iconsPackHandler = new IconsHandler(this);
    }

    public void initAppProvider() {
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

    public AppProvider getAppProvider() {
        if (mAppProvider == null) {
            connectAppProvider();
        }
        return mAppProvider;
    }

    public WidgetHost getAppWidgetHost() {
        return sAppWidgetHost;
    }

    public AppWidgetManager getAppWidgetManager() {
        return sAppWidgetManager;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        sAppWidgetHost.stopListening();
        sAppWidgetHost = null;
    }

    public static long getLongPressTimeout() {
        return sLongPressTimeout;
    }
}
