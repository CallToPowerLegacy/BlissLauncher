package foundation.e.blisslauncher.features.launcher;

import android.content.Context;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;
import java.util.List;

import foundation.e.blisslauncher.core.database.LauncherDB;
import foundation.e.blisslauncher.core.database.model.LauncherItem;

public class LoadDatabaseTask extends AsyncTask<Void, Void, List<LauncherItem>> {

    private final WeakReference<Context> mContext;
    private WeakReference<AppProvider> mAppProvider;

    LoadDatabaseTask(Context context) {
        super();
        this.mContext = new WeakReference<>(context);
    }

    public void setAppProvider(AppProvider appProvider) {
        this.mAppProvider = new WeakReference<>(appProvider);
    }

    @Override
    protected List<LauncherItem> doInBackground(Void... voids) {
        return LauncherDB.getDatabase(mContext.get()).launcherDao().getAllItems();
    }

    @Override
    protected void onPostExecute(List<LauncherItem> launcherItems) {
        super.onPostExecute(launcherItems);
        if (mAppProvider != null) {
            mAppProvider.get().loadDatabaseOver(launcherItems);
        }
    }
}
