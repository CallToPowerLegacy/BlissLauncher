package org.indin.blisslaunchero.features.launcher;

import android.content.Context;
import android.util.Log;

import org.indin.blisslaunchero.framework.database.model.AppItem;
import org.indin.blisslaunchero.framework.utils.AppUtils;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;

import io.fabric.sdk.android.services.concurrency.AsyncTask;

public class LoadAppsTask extends AsyncTask<Void, Void, LinkedHashMap<String, AppItem>> {

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
    protected LinkedHashMap<String, AppItem> doInBackground(Void... voids) {
        long start = System.nanoTime();

        LinkedHashMap<String, AppItem> appArrayMap = new LinkedHashMap<>();
        for (AppItem appItem : AppUtils.loadAll(mContext.get())) {
            appArrayMap.put(appItem.getPackageName(), appItem);
        }
        long end = System.nanoTime();
        Log.i("time", Long.toString((end - start) / 1000000) + " milliseconds to list apps");
        return appArrayMap;
    }

    @Override
    protected void onPostExecute(LinkedHashMap<String, AppItem> appItemArrayMap) {
        super.onPostExecute(appItemArrayMap);
        if(mAppProvider!=null){
            mAppProvider.get().loadAppsOver(appItemArrayMap);
        }
    }
}
