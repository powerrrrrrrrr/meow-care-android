package com.meowcare.app;

import android.app.Application;
import com.meowcare.app.appearance.CustomIcon;
import com.meowcare.app.data.CareRepository;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** One process-wide queue serializes all database mutations, including notification queries. */
public final class CareApplication extends Application {
    private CareRepository repository;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    @Override public void onCreate() {
        super.onCreate(); repository = new CareRepository(this);
        io.execute(()->{
            try {CustomIcon.migratePinned(this);}
            catch(IOException | RuntimeException error) {android.util.Log.w("MeowCare","Could not refresh desktop icon; retry from icon settings",error);}
        });
    }
    public CareRepository repository() { return repository; }
    public ExecutorService io() { return io; }
}
