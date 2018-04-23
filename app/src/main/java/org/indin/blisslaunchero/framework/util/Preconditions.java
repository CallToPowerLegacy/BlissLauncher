package org.indin.blisslaunchero.framework.util;

import android.os.Looper;

/**
 * A set of utility methods for thread verification.
 */
public class Preconditions {

    public static void assertNotNull(Object o) {
        if (o == null) {
            throw new IllegalStateException();
        }
    }

    public static void assertWorkerThread() {
        //TODO: Uncommnet after LauncherModel
        /*if (!isSameLooper(LauncherModel.getWorkerLooper())) {
            throw new IllegalStateException();
        }*/
    }

    public static void assertUIThread() {
        if (!isSameLooper(Looper.getMainLooper())) {
            throw new IllegalStateException();
        }
    }

    public static void assertNonUiThread() {
        if (isSameLooper(Looper.getMainLooper())) {
            throw new IllegalStateException();
        }
    }

    private static boolean isSameLooper(Looper looper) {
        return Looper.myLooper() == looper;
    }
}
