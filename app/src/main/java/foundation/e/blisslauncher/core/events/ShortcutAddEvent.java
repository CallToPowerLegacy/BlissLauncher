package foundation.e.blisslauncher.core.events;

import foundation.e.blisslauncher.core.database.model.ShortcutItem;

public class ShortcutAddEvent {
    private ShortcutItem mShortcutItem;

    public ShortcutAddEvent(ShortcutItem shortcutItem){
        this.mShortcutItem = shortcutItem;
    }

    public ShortcutItem getShortcutItem() {
        return mShortcutItem;
    }
}
