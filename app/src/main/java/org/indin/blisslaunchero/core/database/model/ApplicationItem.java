package org.indin.blisslaunchero.core.database.model;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.os.UserHandle;

import org.indin.blisslaunchero.core.UserManagerCompat;
import org.indin.blisslaunchero.core.utils.Constants;

public class ApplicationItem extends LauncherItem {

    public static final int FLAG_SYSTEM_UNKNOWN = 0;
    public static final int FLAG_SYSTEM_YES = 1 << 0;
    public static final int FLAG_SYSTEM_NO = 1 << 1;

    /**
     * Package name of the application.
     */
    public String packageName;

    /**
     * Intent used to start the application.
     */
    public Intent intent;

    public ComponentName componentName;

    /**
     * Indicates if the app is a system app or not.
     */
    public int isSystemApp;

    public static final int TYPE_CLOCK = 745;
    public static final int TYPE_CALENDAR = 746;
    public static final int TYPE_DEFAULT = 111;

    /**
     * Indicates the type of app item ie. Clock or Calendar (in case of none, It will be )
     */
    public int appType;

    public ApplicationItem(){
        itemType = Constants.ITEM_TYPE_APPLICATION;
    }

    @Override
    public Intent getIntent() {
        return intent;
    }

    /**
     * Must not hold the Context.
     */
    public ApplicationItem(Context context, LauncherActivityInfo info, UserHandle user) {
        this(info, user, UserManagerCompat.getInstance(context).isQuietModeEnabled(user));
    }

    public ApplicationItem(LauncherActivityInfo info, UserHandle user, boolean quietModeEnabled) {
        itemType = Constants.ITEM_TYPE_APPLICATION;
        this.componentName = info.getComponentName();
        this.id = this.componentName.flattenToString();
        this.container = LauncherItem.NO_ID;
        this.user = user;

        intent = makeLaunchIntent(info);

        isSystemApp = (info.getApplicationInfo().flags & ApplicationInfo.FLAG_SYSTEM) == 0
                ? FLAG_SYSTEM_NO : FLAG_SYSTEM_YES;
    }

    public static Intent makeLaunchIntent(LauncherActivityInfo info) {
        return makeLaunchIntent(info.getComponentName());
    }

    public static Intent makeLaunchIntent(ComponentName cn) {
        return new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(cn)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
    }
}
