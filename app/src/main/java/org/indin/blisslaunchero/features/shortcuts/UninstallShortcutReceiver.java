package org.indin.blisslaunchero.features.shortcuts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.indin.blisslaunchero.core.database.DatabaseManager;

public class UninstallShortcutReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent data) {
        String name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        DatabaseManager databaseManager = DatabaseManager.getManager(context);
        databaseManager.removeShortcut(name);
    }

}
