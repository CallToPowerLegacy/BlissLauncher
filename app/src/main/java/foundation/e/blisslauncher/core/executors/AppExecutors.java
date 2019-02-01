package foundation.e.blisslauncher.core.executors;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AppExecutors {
    private static final AppExecutors ourInstance = new AppExecutors();
    private Executor diskExecutor;
    private Executor searchExecutor;
    public static AppExecutors getInstance() {
        return ourInstance;
    }

    private AppExecutors() {
        diskExecutor = Executors.newSingleThreadExecutor();
    }

    public Executor diskIO(){
        return diskExecutor;
    }
}
