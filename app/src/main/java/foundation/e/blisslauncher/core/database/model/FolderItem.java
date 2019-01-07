package foundation.e.blisslauncher.core.database.model;

import java.util.List;

import foundation.e.blisslauncher.core.utils.Constants;

public class FolderItem extends LauncherItem {

    /**
     * Stores networkItems that user saved in this folder.
     */
    public List<LauncherItem> items;

    public FolderItem() {
        itemType = Constants.ITEM_TYPE_FOLDER;
    }

}
