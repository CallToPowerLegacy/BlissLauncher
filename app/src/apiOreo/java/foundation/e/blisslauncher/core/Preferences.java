package foundation.e.blisslauncher.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

import foundation.e.blisslauncher.core.utils.Constants;

public class Preferences {

    private static final String DAY_FORECAST_CONDITION_CODE = "condition_code";
    private static final String DAY_FORECAST_LOW = "low";
    private static final String DAY_FORECAST_HIGH = "high";

    private static final String NOTIFICATION_ACCESS = "notification_access";

    private static final String ENABLE_LOCATION = "enable_location";

    private static final String ACTION_USAGE = "foundation.e.blisslauncher.ACTION_USAGE";

    private static final String CURRENT_MIGRATION_VERSION = "current_migration_version";

    private Preferences() {
    }

    public static void setUserCreationTime(Context context, String key) {
        getPrefs(context).edit().putLong(key, System.currentTimeMillis()).apply();
    }

    public static boolean shouldOpenUsageAccess(Context context) {
        return getPrefs(context).getBoolean(ACTION_USAGE, true);
    }

    public static void setNotOpenUsageAccess(Context context) {
        getPrefs(context).edit().putBoolean(ACTION_USAGE, false).apply();
    }

    public static boolean shouldAskForNotificationAccess(Context context) {
        return getPrefs(context).getBoolean(NOTIFICATION_ACCESS, true);
    }

    public static void setNotToAskForNotificationAccess(Context context) {
        getPrefs(context).edit().putBoolean(NOTIFICATION_ACCESS, false).apply();
    }

    public static int getCurrentMigrationVersion(Context context){
        return getPrefs(context).getInt(CURRENT_MIGRATION_VERSION, 0);
    }

    public static void setCurrentMigrationVersion(Context context, int version){
        getPrefs(context).edit().putInt(CURRENT_MIGRATION_VERSION, version).apply();
    }

    public static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void setEnableLocation(Context context) {
        getPrefs(context).edit().putBoolean(ENABLE_LOCATION, true).apply();
    }

    public static boolean getEnableLocation(
            Context context) {
        return getPrefs(context).getBoolean(ENABLE_LOCATION, false);
    }
}
