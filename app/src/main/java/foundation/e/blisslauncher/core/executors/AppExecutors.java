package foundation.e.blisslauncher.core.executors;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AppExecutors {
    private static final AppExecutors ourInstance = new AppExecutors();
    private Executor diskExecutor;
    private Executor appExecutor;
    private Executor searchExecutor;
    private Executor shortcutExecutor;

    public static AppExecutors getInstance() {
        return ourInstance;
    }

    private AppExecutors() {
        diskExecutor = Executors.newSingleThreadExecutor();
        appExecutor = Executors.newSingleThreadExecutor();
        shortcutExecutor = Executors.newSingleThreadExecutor();
    }

    public Executor diskIO(){
        return diskExecutor;
    }

    public Executor appIO(){
        return appExecutor;
    }

    public Executor shortcutIO() {
        return shortcutExecutor;
    }
}
