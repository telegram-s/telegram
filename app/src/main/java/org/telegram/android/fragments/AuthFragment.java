package org.telegram.android.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import org.telegram.android.R;
import org.telegram.android.StartActivity;
import org.telegram.android.StelsFragment;
import org.telegram.android.login.ActivationController;
import org.telegram.android.login.ActivationListener;

/**
 * Created by ex3ndr on 21.12.13.
 */
public class AuthFragment extends StelsFragment implements ActivationListener {

    private int currentState = ActivationController.STATE_START;

    private View automatic;
    private View progress;
    private View networkError;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View res = inflater.inflate(R.layout.auth_main, container, false);

        automatic = res.findViewById(R.id.automaticInit);
        progress = res.findViewById(R.id.progress);
        networkError = res.findViewById(R.id.networkError);

        res.findViewById(R.id.confirmPhone).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.getKernel().getActivationController().doConfirmPhone();
            }
        });

        if (application.getKernel().getActivationController() == null) {
            application.getKernel().setActivationController(new ActivationController(application));
        }

        automatic.setVisibility(View.GONE);
        progress.setVisibility(View.GONE);
        networkError.setVisibility(View.GONE);

        return res;
    }

    @Override
    public void onResume() {
        super.onResume();

        application.getKernel().getActivationController().setListener(this);
        onStateChanged(application.getKernel().getActivationController().getCurrentState());
    }

    @Override
    public void onPause() {
        super.onPause();
        application.getKernel().getActivationController().setListener(null);
    }

    @Override
    public void onStateChanged(int state) {
        if (state == ActivationController.STATE_PHONE_CONFIRM) {
            automatic.setVisibility(View.VISIBLE);
            progress.setVisibility(View.GONE);
            networkError.setVisibility(View.GONE);
        } else if (state == ActivationController.STATE_ACTIVATION) {
            automatic.setVisibility(View.GONE);
            progress.setVisibility(View.VISIBLE);
            networkError.setVisibility(View.GONE);
        } else if (state == ActivationController.STATE_ACTIVATED) {
            ((StartActivity) getActivity()).onSuccessAuth();
        }
    }
}
