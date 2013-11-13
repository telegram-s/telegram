package org.telegram.android.ui;

import org.telegram.android.StelsBaseFragment;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Author: Korshakov Stepan
 * Created: 29.07.13 23:11
 */
public class UiCyclic {
    private AtomicBoolean isStopped = new AtomicBoolean(true);
    private Runnable runnable;
    private Runnable cyclicRunnable;
    private StelsBaseFragment fragment;
    private int delta;

    public UiCyclic(StelsBaseFragment fragment, Runnable runnable, int delta) {
        this.fragment = fragment;
        this.runnable = runnable;
        this.delta = delta;
        this.cyclicRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isStopped.get()) {
                    UiCyclic.this.runnable.run();
                }
                if (!isStopped.get()) {
                    UiCyclic.this.fragment.postDelayerWeak(cyclicRunnable, UiCyclic.this.delta);
                }
            }
        };
    }

    public void start() {
        if (isStopped.compareAndSet(true, false)) {
            cyclicRunnable.run();
        }
    }

    public void manualRun() {
        runnable.run();
    }

    public void stop() {
        isStopped.set(true);
    }
}
