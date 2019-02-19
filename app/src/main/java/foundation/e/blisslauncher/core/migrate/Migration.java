package foundation.e.blisslauncher.core.migrate;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import foundation.e.blisslauncher.core.Preferences;
import foundation.e.blisslauncher.core.database.DatabaseManager;

public class Migration {

    public static volatile Migration instance;

    private static final String TAG = "Migration";

    private Migration() {
    }

    public static void migrateSafely(Context context) {
        if (instance == null) {
            synchronized (Migration.class) {
                if (instance == null) {
                    instance = new Migration();
                }
            }
        }
        String migrationInfo = instance.readJSONFromAsset(context);
        CurrentMigration currentMigration = new Gson().fromJson(migrationInfo,
                CurrentMigration.class);
        int oldVersion = Preferences.getCurrentMigrationVersion(context);
        if (oldVersion < currentMigration.currentVersion) {
            List<MigrationInfo> infos = currentMigration.migrate_infos.parallelStream().filter(
                    migrationInfo1 -> migrationInfo1.startVersion >= oldVersion).collect(
                    Collectors.toList());
            Log.i(TAG, "migrateSafely: "+infos.size());
            Collections.sort(infos);
            for (MigrationInfo info : infos) {
                List<MigrateComponentInfo> components = info.components;
                for (MigrateComponentInfo component : components) {
                    DatabaseManager.getManager(context).migrateComponent(
                            component.old_component_name, component.new_component_name);
                }
            }
            Preferences.setCurrentMigrationVersion(context, currentMigration.currentVersion);
        }
    }

    private String readJSONFromAsset(Context context) {
        String json = null;
        try {
            InputStream is = context.getAssets().open("migrate_info.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }
}
