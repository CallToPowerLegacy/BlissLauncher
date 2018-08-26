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

import android.util.Log;

import com.jakewharton.rxrelay2.BehaviorRelay;

public class AppsRepository {

    private static final String TAG = "AppsRepository";
    private BehaviorRelay<AllAppsList> appsRelay;

    private static AppsRepository sAppsRepository;

    private AppsRepository() {
        appsRelay = BehaviorRelay.create();
    }

    public static AppsRepository getAppsRepository() {
        if (sAppsRepository == null) {
            Log.d(TAG, "getAppsRepository() called");
            sAppsRepository = new AppsRepository();
        }
        return sAppsRepository;
    }

    public void updateAppsRelay(AllAppsList allAppsList) {
        this.appsRelay.accept(allAppsList);
    }

    public BehaviorRelay<AllAppsList> getAppsRelay() {
        return appsRelay;
    }
}
