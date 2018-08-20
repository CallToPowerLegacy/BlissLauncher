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

import java.util.LinkedHashMap;
import java.util.List;

import org.indin.blisslaunchero.framework.database.model.AppItem;

public class AllAppsList {

    public LinkedHashMap<String,AppItem> launchableApps;
    public List<String> defaultPinnedAppsPackages;

    public AllAppsList(LinkedHashMap<String, AppItem> launchableApps, List<String> defaultPinnedAppsPackages){
        this.launchableApps = launchableApps;
        this.defaultPinnedAppsPackages = defaultPinnedAppsPackages;
    }
}
