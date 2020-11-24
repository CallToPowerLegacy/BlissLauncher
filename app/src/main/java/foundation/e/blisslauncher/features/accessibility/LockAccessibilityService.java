package foundation.e.blisslauncher.features.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import androidx.room.util.StringUtil;

public class LockAccessibilityService extends AccessibilityService {

    public static final String CUSTOM_DOUBLE_TAP_EVENT = "DOUBLE_TAPPED";

    private static final String TAG = "LockAccessibilityService";

    @Override
    protected void onServiceConnected() {
        Log.d(TAG, "onServiceConnected");
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
        info.notificationTimeout = 100;
        info.packageNames = null;
        setServiceInfo(info);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.d(TAG, "onAccessibilityEvent");
        boolean containsCustomDoubleTapEvent =
                event.getText().stream()
                        .filter(cs -> cs.equals(CUSTOM_DOUBLE_TAP_EVENT))
                        .findAny()
                        .isPresent();
        if (containsCustomDoubleTapEvent) {
            lockScreen();
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "onInterrupt");
    }

    public void lockScreen() {
        Log.d(TAG, "lockScreen");
        performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
    }

}
