/*
 * Copyright 2018 /e/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.indin.blisslaunchero.features.launcher;

import java.lang.ref.WeakReference;

import org.indin.blisslaunchero.framework.utils.AppUtils;

import android.content.Context;
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
