package org.indin.blisslaunchero.features.notification;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import org.indin.blisslaunchero.framework.utils.ListUtil;

/**
 * Created by falcon on 14/3/18.
 */

public class NotificationService extends NotificationListenerService {

    NotificationRepository mNotificationRepository;

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
        mNotificationRepository.updateNotification(ListUtil.asSafeList(getActiveNotifications()));
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        mNotificationRepository.updateNotification(ListUtil.asSafeList(getActiveNotifications()));
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        mNotificationRepository.updateNotification(ListUtil.asSafeList(getActiveNotifications()));
    }
}
