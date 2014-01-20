package org.telegram.android.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import org.telegram.android.R;
import org.telegram.android.StartActivity;
import org.telegram.android.base.TelegramFragment;
import org.telegram.android.kernel.KernelsLoader;

/**
 * Created by ex3ndr on 20.01.14.
 */
public class UpgradeFragment extends TelegramFragment implements KernelsLoader.KernelsLoadingListener {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View res = inflater.inflate(R.layout.recover, container, false);
        return res;
    }

    @Override
    public void onResume() {
        super.onResume();
        application.getKernelsLoader().addListener(this);
        if (application.getKernelsLoader().isLoaded()) {
            ((StartActivity) getActivity()).doInitApp(false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        application.getKernelsLoader().removeListener(this);
    }

    @Override
    public void onKernelsLoaded() {
        ((StartActivity) getActivity()).doInitApp(false);
    }
}
