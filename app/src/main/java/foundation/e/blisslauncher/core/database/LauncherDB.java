package foundation.e.blisslauncher.core.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;
import android.content.Context;

import foundation.e.blisslauncher.core.database.converters.CharSequenceConverter;
import foundation.e.blisslauncher.core.database.daos.LauncherDao;
import foundation.e.blisslauncher.core.database.model.LauncherItem;

@Database(entities = {LauncherItem.class}, version = 3, exportSchema = false)
@TypeConverters({CharSequenceConverter.class})
public abstract class LauncherDB extends RoomDatabase {

    public abstract LauncherDao launcherDao();

    private static volatile LauncherDB INSTANCE;

    public static LauncherDB getDatabase(Context context){
        if (INSTANCE == null) {
            synchronized (LauncherDB.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            LauncherDB.class, "launcher_db")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
