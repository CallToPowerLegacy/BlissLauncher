package org.indin.blisslaunchero.features.launcher;

import android.content.Context;
import android.os.AsyncTask;

import org.indin.blisslaunchero.core.database.model.ApplicationItem;
import org.indin.blisslaunchero.core.utils.AppUtils;

import java.lang.ref.WeakReference;
import java.util.Map;

public class LoadAppsTask extends AsyncTask<Void, Void, Map<String, ApplicationItem>> {

    private final WeakReference<Context> mContext;
    private WeakReference<AppProvider> mAppProvider;

    LoadAppsTask(Context context) {
        super();
        this.mContext = new WeakReference<>(context);
    }

    public void setAppProvider(AppProvider appProvider) {
        this.mAppProvider = new WeakReference<>(appProvider);
    }

    @Override
    protected Map<String, ApplicationItem> doInBackground(Void... voids) {
        return AppUtils.loadAll(mContext.get());
    }

    @Override
    protected void onPostExecute(Map<String, ApplicationItem> appItemPair) {
        super.onPostExecute(appItemPair);
        if (mAppProvider != null) {
            mAppProvider.get().loadAppsOver(appItemPair);
        }
    }
}
