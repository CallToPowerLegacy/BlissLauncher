package foundation.e.blisslauncher.features.accessibility;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

/**
 * Utility methods for accessibility, implemented as singleton
 */
public class AccessibilityUtils {

    private static String TAG = "AccessibilityUtils";
    private static AccessibilityUtils instance;

    private AccessibilityUtils() {
        // Nothing to see here...
    }

    /**
     * Returns the singleton instance
     *
     * @return the singleton instance
     */
    public static synchronized AccessibilityUtils getInstance() {
        if (instance == null) {
            instance = new AccessibilityUtils();
        }

        return instance;
    }

    /**
     * Checks whether the given Accessibility manager is activated.
     * <p>
     * Source: https://stackoverflow.com/questions/18094982/detect-if-my-accessibility-service-is-enabled
     *
     * @param packageName The package name
     * @param mContext    The view context
     * @return true if the given Accessibility manager is activated, false else
     */
    public boolean isAccessibilitySettingsOn(String packageName, Context mContext) {
        int accessibilityEnabled = 0;
        final String service = packageName + "/" + LockAccessibilityService.class.getCanonicalName();
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    mContext.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
            Log.d(TAG, "accessibilityEnabled = " + accessibilityEnabled);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "Error finding setting, default accessibility not found: "
                    + e.getMessage());
        }

        if (accessibilityEnabled == 1) {
            Log.d(TAG, "Accessibility is enabled");
            String settingValue = Settings.Secure.getString(
                    mContext.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String accessibilityService = mStringColonSplitter.next();

                    Log.v(TAG, "accessibilityService :: " + accessibilityService + " " + service);
                    if (accessibilityService.equalsIgnoreCase(service)) {
                        Log.d(TAG, "Accessibility manager is switched on");
                        return true;
                    }
                }
                Log.d(TAG, "Accessibility manager is not switched on");
            }
        } else {
            Log.d(TAG, "Accessibility is disabled");
        }

        return false;
    }

}
