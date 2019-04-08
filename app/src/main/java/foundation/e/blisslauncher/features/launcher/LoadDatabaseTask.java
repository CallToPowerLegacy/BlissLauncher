package foundation.e.blisslauncher.features.launcher;

import android.os.AsyncTask;

import java.util.List;

import foundation.e.blisslauncher.core.database.LauncherDB;
import foundation.e.blisslauncher.core.database.model.LauncherItem;
import foundation.e.blisslauncher.core.migrate.Migration;

public class LoadDatabaseTask extends AsyncTask<Void, Void, List<LauncherItem>> {

    private AppProvider mAppProvider;

    LoadDatabaseTask() {
        super();
    }

    public void setAppProvider(AppProvider appProvider) {
        this.mAppProvider = appProvider;
    }

    @Override
    protected List<LauncherItem> doInBackground(Void... voids) {
        Migration.migrateSafely(mAppProvider.getContext());
        return LauncherDB.getDatabase(mAppProvider.getContext()).launcherDao().getAllItems();
    }

    @Override
    protected void onPostExecute(List<LauncherItem> launcherItems) {
        super.onPostExecute(launcherItems);
        if (mAppProvider != null) {
            mAppProvider.loadDatabaseOver(launcherItems);
        }
    }
}
