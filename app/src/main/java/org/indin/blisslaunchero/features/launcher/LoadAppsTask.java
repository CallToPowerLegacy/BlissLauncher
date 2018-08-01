package org.indin.blisslaunchero.features.launcher;

import android.content.Context;
import android.util.Log;

import org.indin.blisslaunchero.framework.database.model.AppItem;
import org.indin.blisslaunchero.framework.utils.AppUtils;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;

import io.fabric.sdk.android.services.concurrency.AsyncTask;

public class LoadAppsTask extends AsyncTask<Void, Void, AllAppsList> {

    final WeakReference<Context> mContext;
    private WeakReference<AppProvider> mAppProvider;

    private static final String TAG = "LoadAppsTask";

    public LoadAppsTask(Context context){
        super();
        this.mContext = new WeakReference<>(context);
    }

    public void setAppProvider(AppProvider appProvider){
        this.mAppProvider = new WeakReference<>(appProvider);
    }

    @Override
    protected AllAppsList doInBackground(Void... voids) {
        return AppUtils.loadAll(mContext.get());
    }

    @Override
    protected void onPostExecute(AllAppsList appItemArrayMap) {
        super.onPostExecute(appItemArrayMap);
        if(mAppProvider!=null){
            mAppProvider.get().loadAppsOver(appItemArrayMap);
        }
    }
}
