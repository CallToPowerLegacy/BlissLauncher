package org.indin.blisslaunchero.core.events;

import org.indin.blisslaunchero.core.database.model.ShortcutItem;

public class ShortcutAddEvent {
    private ShortcutItem mShortcutItem;

    public ShortcutAddEvent(ShortcutItem shortcutItem){
        this.mShortcutItem = shortcutItem;
    }

    public ShortcutItem getShortcutItem() {
        return mShortcutItem;
    }
}
