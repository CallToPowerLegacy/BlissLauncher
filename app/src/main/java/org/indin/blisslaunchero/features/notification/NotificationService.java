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
package org.indin.blisslaunchero.features.notification;

import org.indin.blisslaunchero.framework.utils.ListUtil;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

/**
 * Created by falcon on 14/3/18.
 */

public class NotificationService extends NotificationListenerService {

    NotificationRepository mNotificationRepository;

    private static final String TAG = "NotificationService";

    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationRepository = NotificationRepository.getNotificationRepository();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onListenerConnected() {
        Log.d(TAG, "onListenerConnected() called");
        mNotificationRepository.updateNotification(ListUtil.asSafeList(getActiveNotifications()));
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.d(TAG, "onNotificationPosted() called with: sbn = [" + sbn + "]");
        mNotificationRepository.updateNotification(ListUtil.asSafeList(getActiveNotifications()));
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.d(TAG, "onNotificationRemoved() called with: sbn = [" + sbn + "]");
        mNotificationRepository.updateNotification(ListUtil.asSafeList(getActiveNotifications()));
    }
}
