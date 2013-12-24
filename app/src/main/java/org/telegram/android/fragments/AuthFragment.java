package org.telegram.android.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
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

    private int currentState = ActivationController.STATE_START;

    private View manual;
    private View automatic;
    private View progress;
    private View networkError;
    private View unableError;
    private View phoneRequest;
    private View phoneActivation;
    private View phoneSend;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View res = inflater.inflate(R.layout.auth_main, container, false);

        automatic = res.findViewById(R.id.automaticInit);
        manual = res.findViewById(R.id.manualPhone);
        progress = res.findViewById(R.id.progress);
        phoneRequest = res.findViewById(R.id.phoneRequestProgress);
        phoneActivation = res.findViewById(R.id.phoneActivation);
        networkError = res.findViewById(R.id.networkError);
        unableError = res.findViewById(R.id.unableError);
        phoneSend = res.findViewById(R.id.phoneSendProgress);

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
                startActivity(new Intent().setClass(getActivity(), PickCountryActivity.class));
            }
        });
        manual.findViewById(R.id.doActivation).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // getActivity().getSystemService()
                String number = ((TextView) manual.findViewById(R.id.phoneName)).getText().toString();
                number = "+" +
                        application.getKernel().getActivationController().getCurrentCountry().getCallPrefix() +
                        number;

                try {
                    final Phonenumber.PhoneNumber numberUtil = PhoneNumberUtil.getInstance().parse(number, application.getKernel().getActivationController().getCurrentCountry().getIso());
                    if (PhoneNumberUtil.getInstance().isValidNumber(numberUtil)) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setMessage("Is this your correct number? \n\n"
                                + PhoneNumberUtil.getInstance().format(numberUtil, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL) + "\n\n" +
                                "An SMS with your access code will be sent to this number.");
                        builder.setPositiveButton(R.string.st_yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                application.getKernel().getActivationController().doActivation(numberUtil.getNationalNumber() + "");
                            }
                        }).setNegativeButton(R.string.st_edit, null);
                        AlertDialog dialog = builder.create();
                        dialog.setCanceledOnTouchOutside(true);
                        dialog.show();
                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setMessage("Phone number "
                                + PhoneNumberUtil.getInstance().format(numberUtil, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL) + " is incorrect");
                        AlertDialog dialog = builder.create();
                        dialog.setCanceledOnTouchOutside(true);
                        dialog.show();
                    }
                } catch (NumberParseException e) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setMessage("Phone number "
                            + number + " is incorrect");
                    AlertDialog dialog = builder.create();
                    dialog.setCanceledOnTouchOutside(true);
                    dialog.show();
                }
            }
        });

        networkError.findViewById(R.id.tryAgain).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.getKernel().getActivationController().doTryAgain();
            }
        });
        unableError.findViewById(R.id.doPhoneActivation).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.getKernel().getActivationController().requestPhone();
            }
        });

        phoneActivation.findViewById(R.id.completePhoneActivation).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int code = Integer.parseInt(((TextView) phoneActivation.findViewById(R.id.code)).getText().toString());
                application.getKernel().getActivationController().sendPhoneRequest(code);
            }
        });


        if (application.getKernel().getActivationController() == null) {
            application.getKernel().setActivationController(new ActivationController(application));
        }

        manual.setVisibility(View.GONE);
        automatic.setVisibility(View.GONE);
        progress.setVisibility(View.GONE);
        networkError.setVisibility(View.GONE);
        unableError.setVisibility(View.GONE);
        phoneRequest.setVisibility(View.GONE);
        phoneActivation.setVisibility(View.GONE);
        phoneSend.setVisibility(View.GONE);

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
        if (application.getKernel().getActivationController() != null) {
            application.getKernel().getActivationController().setListener(null);
        }
    }

    @Override
    public void onStateChanged(int state) {
        if (currentState == state) {
            return;
        }

        // Progress
        if (currentState == ActivationController.STATE_ACTIVATION) {
            hideView(progress);
        }
        if (state == ActivationController.STATE_ACTIVATION) {
            showView(progress, currentState != ActivationController.STATE_START);
        } else {
            progress.setVisibility(View.GONE);
        }

        // Confirmation
        if (currentState == ActivationController.STATE_PHONE_CONFIRM) {
            hideView(automatic);
        }
        if (state == ActivationController.STATE_PHONE_CONFIRM) {
            showView(automatic, currentState != ActivationController.STATE_START);
        } else {
            automatic.setVisibility(View.GONE);
        }

        // Edit
        if (currentState == ActivationController.STATE_PHONE_EDIT) {
            hideView(manual);
        }
        if (state == ActivationController.STATE_PHONE_EDIT) {
            showView(manual, currentState != ActivationController.STATE_START);
        } else {
            manual.setVisibility(View.GONE);
        }

        // Network error
        if (currentState == ActivationController.STATE_ACTIVATION_ERROR_NETWORK) {
            hideView(networkError);
        }
        if (state == ActivationController.STATE_ACTIVATION_ERROR_NETWORK) {
            showView(networkError, currentState != ActivationController.STATE_START);
        } else {
            networkError.setVisibility(View.GONE);
        }

        // Unable
        if (currentState == ActivationController.STATE_ACTIVATION_ERROR_UNABLE) {
            hideView(unableError);
        }
        if (state == ActivationController.STATE_ACTIVATION_ERROR_UNABLE) {
            showView(unableError, currentState != ActivationController.STATE_START);
        } else {
            unableError.setVisibility(View.GONE);
        }

        // Manual activation request
        if (currentState == ActivationController.STATE_MANUAL_ACTIVATION_REQUEST) {
            hideView(phoneRequest);
        }
        if (state == ActivationController.STATE_MANUAL_ACTIVATION_REQUEST) {
            showView(phoneRequest, currentState != ActivationController.STATE_START);
        } else {
            phoneRequest.setVisibility(View.GONE);
        }

        // Manual activation
        if (currentState == ActivationController.STATE_MANUAL_ACTIVATION) {
            hideView(phoneActivation);
        }
        if (state == ActivationController.STATE_MANUAL_ACTIVATION) {
            showView(phoneActivation, currentState != ActivationController.STATE_START);
        } else {
            phoneActivation.setVisibility(View.GONE);
        }

        // Manual activation
        if (currentState == ActivationController.STATE_MANUAL_ACTIVATION_SEND) {
            hideView(phoneSend);
        }
        if (state == ActivationController.STATE_MANUAL_ACTIVATION_SEND) {
            showView(phoneSend, currentState != ActivationController.STATE_START);
        } else {
            phoneSend.setVisibility(View.GONE);
        }

        if (state == ActivationController.STATE_PHONE_CONFIRM) {
            getSherlockActivity().getSupportActionBar().setTitle("Your phone");
        } else if (state == ActivationController.STATE_ACTIVATION) {
            getSherlockActivity().getSupportActionBar().setTitle("Activating phone...");
        } else if (state == ActivationController.STATE_ACTIVATED) {
            application.getKernel().setActivationController(null);
            ((StartActivity) getActivity()).onSuccessAuth();
        } else if (state == ActivationController.STATE_ACTIVATION_ERROR_NETWORK) {
            getSherlockActivity().getSupportActionBar().setTitle("Telegram");
        } else if (state == ActivationController.STATE_ACTIVATION_ERROR_UNABLE) {
            getSherlockActivity().getSupportActionBar().setTitle("Telegram");
        } else if (state == ActivationController.STATE_PHONE_EDIT) {
            getSherlockActivity().getSupportActionBar().setTitle("Your phone");
        }

        currentState = state;
    }
}
