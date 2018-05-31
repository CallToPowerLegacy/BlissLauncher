package org.indin.blisslaunchero.features.launcher;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import org.indin.blisslaunchero.data.model.AppItem;

import java.util.List;

@Dao
public interface AppsDao {

    @Query("SELECT * FROM apps")
    List<AppItem> getAll();

    @Query("SELECT * FROM apps WHERE package_name = :packageName")
    AppItem getAppByPackageName(String packageName);

    @Insert
    void insertAll(AppItem... apps);

    @Delete
    void delete(AppItem appItem);
}

