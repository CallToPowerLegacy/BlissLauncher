package org.indin.blisslaunchero.features.notification;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import org.indin.blisslaunchero.framework.util.ListUtil;

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
