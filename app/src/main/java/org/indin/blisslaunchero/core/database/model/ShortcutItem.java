package org.indin.blisslaunchero.core.database.model;

import android.content.Intent;

import org.indin.blisslaunchero.core.utils.Constants;

public class ShortcutItem extends LauncherItem {

    /**
     * Intent used to launch this shortcut.
     */
    public Intent launchIntent;

    public String packageName;

    public ShortcutItem(){
        itemType = Constants.ITEM_TYPE_SHORTCUT;
    }

    @Override
    public Intent getIntent() {
        return launchIntent;
    }
}
