package foundation.e.blisslauncher.core.database;


import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import foundation.e.blisslauncher.core.database.converters.CharSequenceConverter;
import foundation.e.blisslauncher.core.database.daos.LauncherDao;
import foundation.e.blisslauncher.core.database.daos.WidgetDao;
import foundation.e.blisslauncher.core.database.model.LauncherItem;
import foundation.e.blisslauncher.core.database.model.WidgetItem;

@Database(entities = {LauncherItem.class, WidgetItem.class}, version = 4, exportSchema = false)
@TypeConverters({CharSequenceConverter.class})
public abstract class LauncherDB extends RoomDatabase {

    public abstract LauncherDao launcherDao();

    public abstract WidgetDao widgetDao();

    private static volatile LauncherDB INSTANCE;

    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `widget_items` (`id` INTEGER NOT NULL, `height` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        }
    };

    private static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
        }
    };

    public static LauncherDB getDatabase(Context context) {
        if (INSTANCE == null) {
            synchronized (LauncherDB.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            LauncherDB.class, "launcher_db")
                            .addMigrations(MIGRATION_3_4)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static final class UserHandleMigration extends Migration {
        private long userSerialNumber;

        public UserHandleMigration(int startVersion, int endVersion, long userSerialNumber) {
            super(startVersion, endVersion);
            this.userSerialNumber = userSerialNumber;
        }

        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            String suffix = "\"/" + userSerialNumber +"\"";
            String query = "UPDATE launcher_items set item_id=item_id || " + suffix +  "WHERE item_type <> 2";
            database.execSQL(query);
        }
    }
}
