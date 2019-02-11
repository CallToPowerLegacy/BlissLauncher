package foundation.e.blisslauncher.features.notification;

import android.content.Intent;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import foundation.e.blisslauncher.core.utils.ListUtil;

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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
}
