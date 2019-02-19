package foundation.e.blisslauncher.core.migrate;

import java.util.List;

public class MigrationInfo implements Comparable<MigrationInfo> {
    public int startVersion;
    public int endVersion;
    public List<MigrateComponentInfo> components;

    @Override
    public int compareTo(MigrationInfo o) {
        return this.startVersion - o.startVersion;
    }
}
