package org.telegram.android.ui;

import org.telegram.android.StelsBaseFragment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Author: Korshakov Stepan
 * Created: 28.07.13 20:55
 */
public abstract class DataSourceUpdater {
    static final ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread res = new Thread(runnable);
            res.setName("DataSourceUpdater#" + res.hashCode());
            return res;
        }
    });

    private final AtomicBoolean isValid = new AtomicBoolean(true);
    private StelsBaseFragment fragment;

    public DataSourceUpdater(StelsBaseFragment srcFragment) {
        this.fragment = srcFragment;
    }

    public void markValid() {
        isValid.compareAndSet(false, true);
    }

    public void invalidate() {
        if (isValid.compareAndSet(true, false)) {
            fragment.secureCallback(new Runnable() {
                @Override
                public void run() {
                    if (fragment != null) {
                        executor.execute(new Runnable() {
                            @Override
                            public void run() {
                                doUpdate();
                                isValid.set(true);
                            }
                        });
                    }
                }
            });
        }
    }

    protected abstract void doUpdate();


    public void onDetach() {
        fragment = null;
    }
}