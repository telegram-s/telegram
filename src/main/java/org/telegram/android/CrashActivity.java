package org.telegram.android;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 01.10.13
 * Time: 1:26
 */
public class CrashActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                throw new RuntimeException();
            }
        }, 1000);
    }
}