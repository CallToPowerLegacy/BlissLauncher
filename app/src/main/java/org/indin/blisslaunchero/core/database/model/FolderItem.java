package org.indin.blisslaunchero.core.database.model;

import org.indin.blisslaunchero.core.utils.Constants;

import java.util.List;

public class FolderItem extends LauncherItem {

    /**
     * Stores networkItems that user saved in this folder.
     */
    public List<LauncherItem> items;

    public FolderItem() {
        itemType = Constants.ITEM_TYPE_FOLDER;
    }

}
