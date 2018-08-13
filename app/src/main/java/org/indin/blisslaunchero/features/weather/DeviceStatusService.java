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
package org.indin.blisslaunchero.features.weather;

import org.indin.blisslaunchero.framework.utils.Constants;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.util.Log;

public class DeviceStatusService extends Service {

    private static final String TAG = DeviceStatusService.class.getSimpleName();
    private static final boolean D = Constants.DEBUG;

    private BroadcastReceiver mDeviceStatusListenerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // Network connection has changed, make sure the weather update service knows about it
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                boolean hasConnection = !intent.getBooleanExtra(
                        ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);

                if (D) Log.d(TAG, "Got connectivity change, has connection: " + hasConnection);

                Intent i = new Intent(context, WeatherUpdateService.class);
                if (hasConnection) {
                    context.startService(i);
                } else {
                    context.stopService(i);
                }
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                if (D) Log.d(TAG, "onDisplayOff: Cancel pending update");
                WeatherUpdateService.cancelUpdates(context);
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                if (D) Log.d(TAG, "onDisplayOn: Reschedule update");
                WeatherUpdateService.scheduleNextUpdate(context, false);
            }
        }
    };

    @Override
    public void onCreate() {
        IntentFilter deviceStatusFilter = new IntentFilter();
        deviceStatusFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        deviceStatusFilter.addAction(Intent.ACTION_SCREEN_OFF);
        deviceStatusFilter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(mDeviceStatusListenerReceiver, deviceStatusFilter);
    }

    @Override
    public void onDestroy() {
        if (D) Log.d(TAG, "Stopping service");
        unregisterReceiver(mDeviceStatusListenerReceiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (D) Log.d(TAG, "Starting service");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
