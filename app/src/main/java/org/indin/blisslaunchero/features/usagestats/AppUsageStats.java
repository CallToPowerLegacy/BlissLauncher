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
package org.indin.blisslaunchero.features.usagestats;

import static android.app.usage.UsageStatsManager.INTERVAL_BEST;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import org.indin.blisslaunchero.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AppUsageStats {

    private final Context mContext;
    private final UsageStatsManager mUsageStatsManager;

    private static final String TAG = "AppUsageStats";

    public AppUsageStats(Context context) {
        this.mContext = context;
        mUsageStatsManager = (UsageStatsManager) context.getSystemService(
                Context.USAGE_STATS_SERVICE);
    }

    public List<UsageStats> getUsageStats() {
        List<UsageStats> usageStats = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -1);

        List<UsageStats> stats = mUsageStatsManager.queryUsageStats(INTERVAL_BEST,
                cal.getTimeInMillis(), System.currentTimeMillis());
        Map<String, UsageStats> aggregatedStats = new HashMap<>();
        final int statCount = stats.size();
        for (int i = 0; i < statCount; i++) {
            UsageStats newStat = stats.get(i);
            UsageStats existingStat = aggregatedStats.get(newStat.getPackageName());
            if (existingStat == null) {
                aggregatedStats.put(newStat.getPackageName(), newStat);
            } else {
                existingStat.add(newStat);
            }
        }

        if (aggregatedStats.size() == 0) {
            Log.i(TAG, "The user may not allow the access to apps usage. ");
            Toast.makeText(mContext,
                    mContext.getString(R.string.explanation_access_to_appusage_is_not_enabled),
                    Toast.LENGTH_LONG).show();
            mContext.startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            Log.i(TAG, "getUsageStats: here");
        } else {
            Set<Map.Entry<String, UsageStats>> set = aggregatedStats.entrySet();
            List<Map.Entry<String, UsageStats>> list = new ArrayList<>(set);
            Collections.sort(list,
                    (o1, o2) -> Long.compare(o2.getValue().getTotalTimeInForeground(),
                            o1.getValue().getTotalTimeInForeground()));
            for (Map.Entry<String, UsageStats> stringUsageStatsEntry : list) {
                usageStats.add(stringUsageStatsEntry.getValue());
            }
        }
        Log.i(TAG, "getUsageStats: here2");

        return usageStats;
    }
}
