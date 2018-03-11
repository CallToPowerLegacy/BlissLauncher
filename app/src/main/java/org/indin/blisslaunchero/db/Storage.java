package org.indin.blisslaunchero.db;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.GridLayout;

import org.indin.blisslaunchero.model.AppItem;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

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
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
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
            appData.put("componentName", appItem.getComponentName());
            appData.put("isFolder", appItem.isFolder());
            if(appItem.isFolder()) {
                appData.put("folderID", appItem.getFolderID());
                appData.put("folderName", appItem.getLabel());
                JSONArray subAppComponentNames = new JSONArray();
                for(int k=0;k<appItem.getSubApps().size();k++) {
                    subAppComponentNames.put(appItem.getSubApps().get(k).getComponentName());
                }
                appData.put("subApps", subAppComponentNames);
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
