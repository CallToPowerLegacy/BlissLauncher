package foundation.e.blisslauncher.core.events;

import foundation.e.blisslauncher.core.utils.UserHandle;

public class AppRemoveEvent extends Event {
    private String packageName;
    private UserHandle userHandle;

    public static final int TYPE = 602;

    public AppRemoveEvent(String packageName, UserHandle userHandle) {
        super(TYPE);
        this.packageName = packageName;
        this.userHandle = userHandle;
    }

    public String getPackageName() {
        return packageName;
    }

    public UserHandle getUserHandle() {
        return userHandle;
    }
}
