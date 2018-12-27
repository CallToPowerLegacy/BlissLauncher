package org.indin.blisslaunchero.core.database;

import android.content.Context;
import android.widget.GridLayout;

import org.indin.blisslaunchero.core.customviews.BlissFrameLayout;
import org.indin.blisslaunchero.core.database.model.FolderItem;
import org.indin.blisslaunchero.core.database.model.LauncherItem;
import org.indin.blisslaunchero.core.executors.AppExecutors;
import org.indin.blisslaunchero.core.utils.Constants;

import java.util.List;

public class DatabaseManager {

    private AppExecutors mAppExecutors;

    private static volatile DatabaseManager INSTANCE;
    private Context mContext;

    private DatabaseManager(Context context) {
        this.mContext = context;
        mAppExecutors = AppExecutors.getInstance();
    }

    public static DatabaseManager getManager(Context context) {
        if (INSTANCE == null) {
            synchronized (DatabaseManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new DatabaseManager(context);
                }
            }
        }
        return INSTANCE;
    }

    public void removeLauncherItem(String itemId) {
        mAppExecutors.diskIO().execute(
                () -> LauncherDB.getDatabase(mContext).launcherDao().delete(itemId));
    }

    public void saveLayouts(List<GridLayout> pages, GridLayout dock) {
        mAppExecutors.diskIO().execute(() -> saveLauncherItems(pages, dock));
    }

    private void saveLauncherItems(final List<GridLayout> pages, final GridLayout dock) {
        for (int i = 0; i < dock.getChildCount(); i++) {
            LauncherItem launcherItem = ((BlissFrameLayout) dock.getChildAt(i)).getLauncherItem();
            if (launcherItem.itemType == Constants.ITEM_TYPE_FOLDER) {
                FolderItem folderItem = (FolderItem) launcherItem;
                folderItem.screenId = -1;
                folderItem.cell = i;
                folderItem.container = Constants.CONTAINER_HOTSEAT;
                LauncherDB.getDatabase(mContext).launcherDao().insert(folderItem);

                for (int j = 0; j < folderItem.items.size(); j++) {
                    LauncherItem item = folderItem.items.get(j);
                    item.screenId = -1;
                    item.container = Long.parseLong(folderItem.id);
                    item.cell = j;
                    LauncherDB.getDatabase(mContext).launcherDao().insert(item);
                }
            } else {
                launcherItem.screenId = -1;
                launcherItem.container = Constants.CONTAINER_HOTSEAT;
                launcherItem.cell = i;
                LauncherDB.getDatabase(mContext).launcherDao().insert(launcherItem);
            }
        }

        for (int i = 0; i < pages.size(); i++) {
            GridLayout gridLayout = pages.get(i);
            for (int j = 0; j < gridLayout.getChildCount(); j++) {
                LauncherItem launcherItem = ((BlissFrameLayout) gridLayout.getChildAt(
                        j)).getLauncherItem();
                if (launcherItem.itemType == Constants.ITEM_TYPE_FOLDER) {
                    FolderItem folderItem = (FolderItem) launcherItem;
                    folderItem.screenId = i;
                    folderItem.cell = j;
                    folderItem.container = Constants.CONTAINER_DESKTOP;
                    LauncherDB.getDatabase(mContext).launcherDao().insert(folderItem);
                    for (int k = 0; k < folderItem.items.size(); k++) {
                        LauncherItem item = folderItem.items.get(k);
                        item.screenId = -1;
                        item.container = Long.parseLong(folderItem.id);
                        item.cell = k;
                        LauncherDB.getDatabase(mContext).launcherDao().insert(item);
                    }
                } else {
                    launcherItem.screenId = i;
                    launcherItem.container = Constants.CONTAINER_DESKTOP;
                    launcherItem.cell = j;
                    LauncherDB.getDatabase(mContext).launcherDao().insert(launcherItem);
                }
            }
        }
    }
}
