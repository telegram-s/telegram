package org.telegram.android.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.telegram.android.R;
import org.telegram.android.StartActivity;
import org.telegram.android.StelsFragment;
import org.telegram.android.activity.PickCountryActivity;
import org.telegram.android.login.ActivationController;
import org.telegram.android.login.ActivationListener;

/**
 * Created by ex3ndr on 21.12.13.
 */
public class AuthFragment extends StelsFragment implements ActivationListener {

    private View manual;
    private View automatic;
    private View progress;
    private View networkError;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View res = inflater.inflate(R.layout.auth_main, container, false);

        automatic = res.findViewById(R.id.automaticInit);
        manual = res.findViewById(R.id.manualPhone);
        progress = res.findViewById(R.id.progress);
        networkError = res.findViewById(R.id.networkError);

        automatic.findViewById(R.id.confirmPhone).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.getKernel().getActivationController().doConfirmPhone();
            }
        });
        automatic.findViewById(R.id.editPhone).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.getKernel().getActivationController().doEditPhone();
            }
        });

        manual.findViewById(R.id.countrySelect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(new Intent().setClass(getActivity(), PickCountryActivity.class), 0);
            }
        });


        if (application.getKernel().getActivationController() == null) {
            application.getKernel().setActivationController(new ActivationController(application));
        }

        manual.setVisibility(View.GONE);
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

        ((TextView) manual.findViewById(R.id.countrySelect)).setText(application.getKernel().getActivationController().getCurrentCountry().getTitle());
        ((TextView) manual.findViewById(R.id.phoneCode)).setText("+" +
                application.getKernel().getActivationController().getCurrentCountry().getCallPrefix());
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
            manual.setVisibility(View.GONE);
            getSherlockActivity().getSupportActionBar().setTitle("Your phone");
        } else if (state == ActivationController.STATE_ACTIVATION) {
            automatic.setVisibility(View.GONE);
            progress.setVisibility(View.VISIBLE);
            networkError.setVisibility(View.GONE);
            manual.setVisibility(View.GONE);
            getSherlockActivity().getSupportActionBar().setTitle("Activating phone...");
        } else if (state == ActivationController.STATE_ACTIVATED) {
            ((StartActivity) getActivity()).onSuccessAuth();
        } else if (state == ActivationController.STATE_ACTIVATION_ERROR_NETWORK) {
            automatic.setVisibility(View.GONE);
            progress.setVisibility(View.GONE);
            networkError.setVisibility(View.VISIBLE);
            manual.setVisibility(View.GONE);
            getSherlockActivity().getSupportActionBar().setTitle("Telegram");
        } else if (state == ActivationController.STATE_ACTIVATION_ERROR_UNABLE) {
            automatic.setVisibility(View.GONE);
            progress.setVisibility(View.GONE);
            networkError.setVisibility(View.GONE);
            manual.setVisibility(View.GONE);
            getSherlockActivity().getSupportActionBar().setTitle("Telegram");
        } else if (state == ActivationController.STATE_PHONE_EDIT) {
            automatic.setVisibility(View.GONE);
            progress.setVisibility(View.GONE);
            networkError.setVisibility(View.GONE);
            manual.setVisibility(View.VISIBLE);
            getSherlockActivity().getSupportActionBar().setTitle("Your phone");
        }
    }
}
