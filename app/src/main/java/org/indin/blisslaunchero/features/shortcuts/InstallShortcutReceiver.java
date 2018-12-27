package org.indin.blisslaunchero.features.shortcuts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.os.Process;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.indin.blisslaunchero.BlissLauncher;
import org.indin.blisslaunchero.core.IconsHandler;
import org.indin.blisslaunchero.core.Utilities;
import org.indin.blisslaunchero.core.database.model.ShortcutItem;
import org.indin.blisslaunchero.core.events.ShortcutAddEvent;
import org.indin.blisslaunchero.core.utils.Constants;

import java.util.UUID;

public class InstallShortcutReceiver extends BroadcastReceiver {
    private static final String TAG = "InstallShortcutReceiver";

    private static final String ACTION_INSTALL_SHORTCUT =
            "com.android.launcher.action.INSTALL_SHORTCUT";

    private static final String LAUNCH_INTENT_KEY = "intent.launch";
    private static final String DEEPSHORTCUT_TYPE_KEY = "isDeepShortcut";
    private static final String APP_SHORTCUT_TYPE_KEY = "isAppShortcut";
    private static final String USER_HANDLE_KEY = "userHandle";
    private static final String NAME_KEY = "name";
    private static final String ICON_KEY = "icon";
    private static final String ICON_RESOURCE_NAME_KEY = "iconResource";
    private static final String ICON_RESOURCE_PACKAGE_NAME_KEY = "iconResourcePackage";

    // The set of shortcuts that are pending install
    private static final String APPS_PENDING_INSTALL = "apps_to_install";

    public static final int NEW_SHORTCUT_BOUNCE_DURATION = 450;
    public static final int NEW_SHORTCUT_STAGGER_DELAY = 85;

    private static final Object sLock = new Object();

    @Override
    public void onReceive(Context context, Intent data) {
        if (!ACTION_INSTALL_SHORTCUT.equals(data.getAction())) {
            return;
        }
        ShortcutItem info = createShortcutInfo(data, context);
        EventBus.getDefault().post(new ShortcutAddEvent(info));
    }

    private void queuePendingShortcutInfo(ShortcutItem info, Context context) {

    }

    public static void queueShortcut(ShortcutInfoCompat info, Context context) {
        ShortcutItem shortcutItem = new ShortcutItem();
        shortcutItem.id = info.getId();
        shortcutItem.user = info.getUserHandle();
        shortcutItem.packageName = info.getPackage();
        shortcutItem.title = info.getShortLabel().toString();
        shortcutItem.container = Constants.CONTAINER_DESKTOP;
        Drawable icon = DeepShortcutManager.getInstance(context).getShortcutIconDrawable(info,
                context.getResources().getDisplayMetrics().densityDpi);
        shortcutItem.icon = BlissLauncher.getApplication(context).getIconsHandler().convertIcon(
                icon);
        shortcutItem.launchIntent = info.makeIntent();
        EventBus.getDefault().post(new ShortcutAddEvent(shortcutItem));
    }

    private static ShortcutItem createShortcutInfo(Intent data, Context context) {

        Intent intent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
        String name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        Parcelable bitmap = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);

        if (intent == null) {
            // If the intent is null, we can't construct a valid ShortcutInfo, so we return null
            Log.e(TAG, "Can't construct ShorcutInfo with null intent");
            return null;
        }

        final ShortcutItem item = new ShortcutItem();

        // Only support intents for current user for now. Intents sent from other
        // users wouldn't get here without intent forwarding anyway.
        item.user = Process.myUserHandle();

        Drawable icon = null;
        if (bitmap instanceof Bitmap) {
            icon = IconsHandler.createIconDrawable((Bitmap) bitmap, context);
        } else {
            Parcelable extra = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
            if (extra instanceof Intent.ShortcutIconResource) {
                icon = IconsHandler.createIconDrawable(
                        (Intent.ShortcutIconResource) extra, context);
            }
        }
        if (icon == null) {
            icon = BlissLauncher.getApplication(
                    context).getIconsHandler().getFullResDefaultActivityIcon();
        }
        item.packageName = intent.getPackage();
        item.id = UUID.randomUUID().toString();
        item.container = Constants.CONTAINER_DESKTOP;
        item.title = Utilities.trim(name);
        item.icon = BlissLauncher.getApplication(context).getIconsHandler().convertIcon(icon);
        item.launchIntent = intent;
        return item;
    }

}
