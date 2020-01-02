package foundation.e.blisslauncher.core.database.daos;


import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import foundation.e.blisslauncher.core.database.model.LauncherItem;
import foundation.e.blisslauncher.core.utils.Constants;

@Dao
public interface LauncherDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(LauncherItem launcherItem);

    @Query("SELECT * FROM launcher_items ORDER BY container, screen_id, cell")
    List<LauncherItem> getAllItems();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<LauncherItem> launcherItems);

    @Query("DELETE FROM launcher_items WHERE item_id = :id")
    void delete(String id);

    @Query("DELETE FROM launcher_items WHERE title = :name and item_type = "
            + Constants.ITEM_TYPE_SHORTCUT)
    void deleteShortcut(String name);

    @Query("UPDATE launcher_items SET item_id = :newComponentName WHERE item_id = :id")
    int updateComponent(String id, String newComponentName);
}
