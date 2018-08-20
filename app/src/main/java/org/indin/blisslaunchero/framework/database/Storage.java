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
package org.indin.blisslaunchero.framework.database;

import java.util.List;

import org.indin.blisslaunchero.framework.database.model.AppItem;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.GridLayout;

public class Storage {

    private final String TAG = "BLISS_STORAGE";
    private final Context context;
    private final SharedPreferences prefs;

    public Storage(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences("launcher_layout", Context.MODE_PRIVATE);
    }

    /**
     * Loop through all the child elements of all the pages, and the dock, to create a JSON
     * document, which is then stored in the shared-preferences.
     * @param pages
     * @param dock
     */
    public void save(final List<GridLayout> pages, final GridLayout dock) {
        AsyncTask.execute(() -> {
            JSONObject data = new JSONObject();
            JSONArray pagesData = new JSONArray();
            JSONArray dockData = new JSONArray();
            try {
                for (int i = 0; i < pages.size(); i++) {
                    stuffData(pagesData, pages.get(i));
                }

                stuffData(dockData, dock);
                data.put("pages", pagesData);
                data.put("dock", dockData);

                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("LAYOUT", data.toString(4));
                editor.putBoolean("LAYOUT_PRESENT", true);
                editor.commit();
            } catch(Exception e) {
                Log.e(TAG, "Couldn't save layout " + e);
            }
        });
    }

    /**
     * Reads the tags of the View items of a single page to populate a JSONArray
     * @param storageArray
     * @param layout
     * @throws Exception
     */
    private void stuffData(JSONArray storageArray, ViewGroup layout) throws Exception {
        JSONArray apps = new JSONArray();
        for (int j = 0; j < layout.getChildCount(); j++) {
            List<Object> tags = (List<Object>) layout.getChildAt(j).getTag();
            AppItem appItem = (AppItem) tags.get(2);
            JSONObject appData = new JSONObject();
            appData.put("index", j);
            appData.put("packageName", appItem.getPackageName());
            appData.put("isFolder", appItem.isFolder());
            if(appItem.isFolder()) {
                appData.put("folderID", appItem.getFolderID());
                appData.put("folderName", appItem.getLabel());
                JSONArray subAppPackageNames = new JSONArray();
                for(int k=0;k<appItem.getSubApps().size();k++) {
                    subAppPackageNames.put(appItem.getSubApps().get(k).getPackageName());
                }
                appData.put("subApps", subAppPackageNames);
            }
            apps.put(appData);
        }
        storageArray.put(apps);
    }

    /**
     * Converts the JSON data stored as a string back into valid JSON objects
     * @return
     */
    public StorageData load() {
        StorageData storageData = new StorageData();
        try {
            JSONObject jsonData = new JSONObject(prefs.getString("LAYOUT", ""));
            storageData.pages = jsonData.getJSONArray("pages");
            storageData.dock = jsonData.getJSONArray("dock");
        } catch (JSONException e) {
            Log.e(TAG, "Could not load data " + e);
        }

        return storageData;
    }

    /**
     * Returns true if the shared preferences contains a previously stored layout
     * @return
     */
    public boolean isLayoutPresent() {
        return prefs.getBoolean("LAYOUT_PRESENT", false);
    }

    /**
     * A class that simplifies access to the stored JSON data.
     */
    public static class StorageData {
        public JSONArray pages;
        public JSONArray dock;
        private String TAG = "BLISS_STORAGE_DATA";

        public int getNPages() {
            return pages.length();
        }

        public int getNDocked() {
            try {
                return dock.getJSONArray(0).length();
            } catch (JSONException e) {
                Log.e(TAG, "Couldn't count dock items.");
                return 0;
            }
        }
    }

    public boolean isWallpaperShown() {
        return prefs.getBoolean("WALLPAPER_SHOWN", false);
    }

    public void setWallpaperShown() {
        prefs.edit().putBoolean("WALLPAPER_SHOWN", true).apply();
    }
}
