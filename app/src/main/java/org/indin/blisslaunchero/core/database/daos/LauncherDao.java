package org.indin.blisslaunchero.core.database.daos;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import org.indin.blisslaunchero.core.database.model.LauncherItem;

import java.util.List;

@Dao
public interface LauncherDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(LauncherItem launcherItem);

    @Query("SELECT * FROM launcher_items ORDER BY container, screen_id, cell")
    List<LauncherItem> getAllItems();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<LauncherItem> launcherItems);

    @Query("DELETE FROM launcher_items WHERE item_id = :id")
    void delete(String id);
}
