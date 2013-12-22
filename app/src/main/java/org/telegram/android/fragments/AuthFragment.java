package org.telegram.android.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import org.telegram.android.R;
import org.telegram.android.StelsFragment;
import org.telegram.android.login.ActivationController;

/**
 * Created by ex3ndr on 21.12.13.
 */
public class AuthFragment extends StelsFragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View res = inflater.inflate(R.layout.auth_main, container, false);

        if (application.getKernel().getActivationController() == null) {
            application.getKernel().setActivationController(new ActivationController(application));
        }

        res.findViewById(R.id.confirmPhone).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.getKernel().getActivationController().doConfirmPhone();
            }
        });
        return res;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}
