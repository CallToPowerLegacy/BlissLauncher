package org.indin.blisslaunchero.features.launcher;

import java.lang.ref.WeakReference;

import org.indin.blisslaunchero.framework.utils.AppUtils;

import android.content.Context;
import android.os.AsyncTask;

public class LoadAppsTask extends AsyncTask<Void, Void, AllAppsList> {

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
    protected AllAppsList doInBackground(Void... voids) {
        return AppUtils.loadAll(mContext.get());
    }

    @Override
    protected void onPostExecute(AllAppsList appItemArrayMap) {
        super.onPostExecute(appItemArrayMap);
        if (mAppProvider != null) {
            mAppProvider.get().loadAppsOver(appItemArrayMap);
        }
    }
}
