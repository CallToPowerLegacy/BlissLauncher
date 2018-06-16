package org.indin.blisslaunchero.framework.util;

import android.content.ComponentName;
import android.content.Context;
import android.os.Process;
import android.os.UserHandle;


import org.indin.blisslaunchero.framework.UserManagerCompat;

import java.util.Arrays;

public class ComponentKey {

    public final ComponentName componentName;
    public final UserHandle user;

    private final int mHashCode;

    public ComponentKey(ComponentName componentName, UserHandle user) {
        Preconditions.assertNotNull(componentName);
        Preconditions.assertNotNull(user);
        this.componentName = componentName;
        this.user = user;
        mHashCode = Arrays.hashCode(new Object[] {componentName, user});

    }

    /**
     * Creates a new component key from an encoded component key string in the form of
     * [flattenedComponentString#userId].  If the userId is not present, then it defaults
     * to the current user.
     */
    public ComponentKey(Context context, String componentKeyStr) {
        int userDelimiterIndex = componentKeyStr.indexOf("#");
        if (userDelimiterIndex != -1) {
            String componentStr = componentKeyStr.substring(0, userDelimiterIndex);
            Long componentUser = Long.valueOf(componentKeyStr.substring(userDelimiterIndex + 1));
            componentName = ComponentName.unflattenFromString(componentStr);
            user = UserManagerCompat.getInstance(context)
                    .getUserForSerialNumber(componentUser.longValue());
        } else {
            // No user provided, default to the current user
            componentName = ComponentName.unflattenFromString(componentKeyStr);
            user = Process.myUserHandle();
        }
        Preconditions.assertNotNull(componentName);
        Preconditions.assertNotNull(user);
        mHashCode = Arrays.hashCode(new Object[] {componentName, user});
    }

    @Override
    public int hashCode() {
        return mHashCode;
    }

    @Override
    public boolean equals(Object o) {
        ComponentKey other = (ComponentKey) o;
        return other.componentName.equals(componentName) && other.user.equals(user);
    }

    /**
     * Encodes a component key as a string of the form [flattenedComponentString#userId].
     */
    @Override
    public String toString() {
        return componentName.flattenToString() + "#" + user;
    }
}
