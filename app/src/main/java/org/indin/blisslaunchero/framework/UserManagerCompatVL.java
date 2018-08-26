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
package org.indin.blisslaunchero.framework;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.indin.blisslaunchero.framework.utils.Constants;
import org.indin.blisslaunchero.framework.utils.LongArrayMap;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.ArrayMap;

public class UserManagerCompatVL extends UserManagerCompat {

    protected final UserManager mUserManager;
    private final PackageManager mPm;
    private final Context mContext;

    protected LongArrayMap<UserHandle> mUsers;
    // Create a separate reverse map as LongArrayMap.indexOfValue checks if objects are same
    // and not {@link Object#equals}
    protected ArrayMap<UserHandle, Long> mUserToSerialMap;

    UserManagerCompatVL(Context context) {
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mPm = context.getPackageManager();
        mContext = context;
    }

    @Override
    public long getSerialNumberForUser(UserHandle user) {
        synchronized (this) {
            if (mUserToSerialMap != null) {
                Long serial = mUserToSerialMap.get(user);
                return serial == null ? 0 : serial;
            }
        }
        return mUserManager.getSerialNumberForUser(user);
    }

    @Override
    public UserHandle getUserForSerialNumber(long serialNumber) {
        synchronized (this) {
            if (mUsers != null) {
                return mUsers.get(serialNumber);
            }
        }
        return mUserManager.getUserForSerialNumber(serialNumber);
    }

    @Override
    public boolean isQuietModeEnabled(UserHandle user) {
        return false;
    }

    @Override
    public boolean isUserUnlocked(UserHandle user) {
        return true;
    }

    @Override
    public boolean isDemoUser() {
        return false;
    }

    @Override
    public void enableAndResetCache() {
        synchronized (this) {
            mUsers = new LongArrayMap<>();
            mUserToSerialMap = new ArrayMap<>();
            List<UserHandle> users = mUserManager.getUserProfiles();
            if (users != null) {
                for (UserHandle user : users) {
                    long serial = mUserManager.getSerialNumberForUser(user);
                    mUsers.put(serial, user);
                    mUserToSerialMap.put(user, serial);
                }
            }
        }
    }

    @Override
    public List<UserHandle> getUserProfiles() {
        synchronized (this) {
            if (mUsers != null) {
                return new ArrayList<>(mUserToSerialMap.keySet());
            }
        }

        List<UserHandle> users = mUserManager.getUserProfiles();
        return users == null ? Collections.emptyList() : users;
    }

    @Override
    public CharSequence getBadgedLabelForUser(CharSequence label, UserHandle user) {
        if (user == null) {
            return label;
        }
        return mPm.getUserBadgedLabel(label, user);
    }

    @Override
    public long getUserCreationTime(UserHandle user) {
        SharedPreferences prefs = Preferences.getPrefs(mContext);
        String key = Constants.USER_CREATION_TIME_KEY + getSerialNumberForUser(user);
        if (!prefs.contains(key)) {
           Preferences.setUserCreationTime(mContext, key);
        }
        return prefs.getLong(key, 0);
    }
}
