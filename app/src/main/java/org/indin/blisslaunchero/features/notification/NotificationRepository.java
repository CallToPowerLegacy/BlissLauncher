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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.jakewharton.rxrelay2.BehaviorRelay;

import android.service.notification.StatusBarNotification;
import android.util.Log;

/**
 * Created by Amit Kumar
 * Email : mr.doc10jl96@gmail.com
 */

public class NotificationRepository {

    private BehaviorRelay<Set<String>> notificationRelay;
    private static final String TAG = "NotificationRepository";

    private static NotificationRepository sInstance;

    private NotificationRepository() {
        notificationRelay = BehaviorRelay.createDefault(Collections.emptySet());
    }

    public static NotificationRepository getNotificationRepository() {
        if (sInstance == null) {
            sInstance = new NotificationRepository();
        }
        return sInstance;
    }

    public void updateNotification(List<StatusBarNotification> list) {
        Log.d(TAG, "updateNotification() called with: list = [" + list.size() + "]");
        Set<String> notificationSet = new HashSet<>();
        for (StatusBarNotification statusBarNotification : list) {
            notificationSet.add(statusBarNotification.getPackageName());
        }
        this.notificationRelay.accept(notificationSet);
    }
    public BehaviorRelay<Set<String>> getNotifications() {
        return this.notificationRelay;
    }
}
