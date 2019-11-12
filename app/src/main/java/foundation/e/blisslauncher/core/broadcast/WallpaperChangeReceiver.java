package foundation.e.blisslauncher.core.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import foundation.e.blisslauncher.core.blur.BlurWallpaperProvider;

public class WallpaperChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        BlurWallpaperProvider.getInstance(context).clear();
    }

    public static WallpaperChangeReceiver register(Context context) {
        IntentFilter timeIntentFilter = new IntentFilter(Intent.ACTION_WALLPAPER_CHANGED);
        WallpaperChangeReceiver receiver = new WallpaperChangeReceiver();
        context.registerReceiver(receiver, timeIntentFilter);
        return receiver;
    }

    public static void unregister(Context context, WallpaperChangeReceiver receiver) {
        context.unregisterReceiver(receiver);
    }
}
