package foundation.e.blisslauncher.features.launcher;

import android.os.AsyncTask;

import java.util.Map;

import foundation.e.blisslauncher.core.database.model.ApplicationItem;
import foundation.e.blisslauncher.core.utils.AppUtils;

public class LoadAppsTask extends AsyncTask<Void, Void, Map<String, ApplicationItem>> {

    private AppProvider mAppProvider;

    LoadAppsTask() {
        super();
    }

    public void setAppProvider(AppProvider appProvider) {
        this.mAppProvider = appProvider;
    }

    @Override
    protected Map<String, ApplicationItem> doInBackground(Void... voids) {
        return AppUtils.loadAll(mAppProvider.getContext());
    }

    @Override
    protected void onPostExecute(Map<String, ApplicationItem> appItemPair) {
        super.onPostExecute(appItemPair);
        if (mAppProvider != null) {
            mAppProvider.loadAppsOver(appItemPair);
        }
    }
}
