package foundation.e.blisslauncher.core.database;

import android.content.Context;
import android.util.Log;
import android.widget.GridLayout;

import java.util.ArrayList;
import java.util.List;

import foundation.e.blisslauncher.core.customviews.BlissFrameLayout;
import foundation.e.blisslauncher.core.database.model.FolderItem;
import foundation.e.blisslauncher.core.database.model.LauncherItem;
import foundation.e.blisslauncher.core.database.model.WidgetItem;
import foundation.e.blisslauncher.core.executors.AppExecutors;
import foundation.e.blisslauncher.core.utils.Constants;
import io.reactivex.Single;

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
        List<LauncherItem> items = new ArrayList<>();
        for (int i = 0; i < dock.getChildCount(); i++) {
            LauncherItem launcherItem = ((BlissFrameLayout) dock.getChildAt(i)).getLauncherItem();
            if (launcherItem.itemType == Constants.ITEM_TYPE_FOLDER) {
                FolderItem folderItem = (FolderItem) launcherItem;
                folderItem.screenId = -1;
                folderItem.cell = i;
                folderItem.container = Constants.CONTAINER_HOTSEAT;
                items.add(launcherItem);

                for (int j = 0; j < folderItem.items.size(); j++) {
                    LauncherItem item = folderItem.items.get(j);
                    item.screenId = -1;
                    item.container = Long.parseLong(folderItem.id);
                    item.cell = j;
                    items.add(item);
                }
            } else {
                launcherItem.screenId = -1;
                launcherItem.container = Constants.CONTAINER_HOTSEAT;
                launcherItem.cell = i;
                items.add(launcherItem);
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
                    items.add(folderItem);
                    for (int k = 0; k < folderItem.items.size(); k++) {
                        LauncherItem item = folderItem.items.get(k);
                        item.screenId = -1;
                        item.container = Long.parseLong(folderItem.id);
                        item.cell = k;
                        items.add(item);
                    }
                } else {
                    launcherItem.screenId = i;
                    launcherItem.container = Constants.CONTAINER_DESKTOP;
                    launcherItem.cell = j;
                    items.add(launcherItem);
                }
            }
        }
        Log.i("Database", "saveLauncherItems: "+items.size());
        LauncherDB.getDatabase(mContext).launcherDao().insertAll(items);
    }

    public void removeShortcut(String name) {
        mAppExecutors.diskIO().execute(
                () -> LauncherDB.getDatabase(mContext).launcherDao().deleteShortcut(name));
    }

    // Already invoked in a disk io thread, so no need to execute in separate thread here.
    public void migrateComponent(String old_component_name, String new_component_name) {
        LauncherDB.getDatabase(mContext).launcherDao().delete(new_component_name);
        LauncherDB.getDatabase(mContext).launcherDao().updateComponent(old_component_name,
                new_component_name);
    }

    public Single<Integer> getHeightOfWidget(int id) {
        return Single.defer(() -> Single.just(LauncherDB.getDatabase(mContext).widgetDao().getHeight(id)));
    }

    public void saveWidget(int id, int height) {
        WidgetItem widgetItem = new WidgetItem(id, height);
        mAppExecutors.diskIO().execute(
                () -> LauncherDB.getDatabase(mContext).widgetDao().insert(widgetItem));
    }

    public void removeWidget(int id) {
        mAppExecutors.diskIO().execute(() -> LauncherDB.getDatabase(mContext).widgetDao().delete(id));
    }
}
