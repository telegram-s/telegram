package org.telegram.android.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import org.telegram.android.ui.TextUtil;

/**
 * Created by ex3ndr on 21.12.13.
 */
public class AuthFragment extends StelsFragment implements ActivationListener {

    private int currentState = ActivationController.STATE_START;

    private View manual;
    private View automatic;
    private View progress;
    private View networkError;
    private View phoneRequest;
    private View phoneActivation;
    private View phoneSend;
    private View wrongPhoneError;
    private View tooOftenError;
    private View expiredError;
    private View unknownError;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View res = inflater.inflate(R.layout.auth_main, container, false);


        if (application.getKernel().getActivationController() == null) {
            application.getKernel().setActivationController(new ActivationController(application));
        }

        automatic = res.findViewById(R.id.automaticInit);
        manual = res.findViewById(R.id.manualPhone);
        progress = res.findViewById(R.id.progress);
        phoneRequest = res.findViewById(R.id.phoneRequestProgress);
        phoneActivation = res.findViewById(R.id.phoneActivation);
        networkError = res.findViewById(R.id.networkError);
        phoneSend = res.findViewById(R.id.phoneSendProgress);
        tooOftenError = res.findViewById(R.id.tooOftenError);
        expiredError = res.findViewById(R.id.expiredError);
        unknownError = res.findViewById(R.id.unknownError);
        wrongPhoneError = res.findViewById(R.id.wrongPhoneError);

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


        String autoPhone = application.getKernel().getActivationController().getAutoPhone();
        if (autoPhone != null) {
            ((TextView) automatic.findViewById(R.id.automaticPhone)).setText(TextUtil.formatPhone(autoPhone));
        }

        manual.findViewById(R.id.countrySelect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent().setClass(getActivity(), PickCountryActivity.class));
            }
        });
        manual.findViewById(R.id.doActivation).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideKeyboard(manual.findViewById(R.id.phoneName));

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
                        }).setNegativeButton(R.string.st_edit, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                showKeyboard((EditText) manual.findViewById(R.id.phoneName));
                            }
                        });
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
                    builder.setPositiveButton(R.string.st_edit, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            showKeyboard((EditText) manual.findViewById(R.id.phoneName));
                        }
                    });
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
        networkError.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.getKernel().getActivationController().cancel();
            }
        });

        tooOftenError.findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.getKernel().getActivationController().cancel();
            }
        });

        unknownError.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.getKernel().getActivationController().cancel();
            }
        });

        unknownError.findViewById(R.id.tryAgain).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.getKernel().getActivationController().doTryAgain();
            }
        });

        expiredError.findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.getKernel().getActivationController().cancel();
            }
        });

        wrongPhoneError.findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.getKernel().getActivationController().cancel();
            }
        });

        phoneActivation.findViewById(R.id.completePhoneActivation).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideKeyboard(phoneActivation.findViewById(R.id.code));

                int code = Integer.parseInt(((TextView) phoneActivation.findViewById(R.id.code)).getText().toString());
                application.getKernel().getActivationController().sendPhoneRequest(code);
            }
        });

        manual.setVisibility(View.GONE);
        automatic.setVisibility(View.GONE);
        progress.setVisibility(View.GONE);
        networkError.setVisibility(View.GONE);
        phoneRequest.setVisibility(View.GONE);
        phoneActivation.setVisibility(View.GONE);
        phoneSend.setVisibility(View.GONE);
        tooOftenError.setVisibility(View.GONE);
        expiredError.setVisibility(View.GONE);
        unknownError.setVisibility(View.GONE);
        wrongPhoneError.setVisibility(View.GONE);

        return res;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (application.getKernel().getActivationController() != null) {
            application.getKernel().getActivationController().onPageShown();
            application.getKernel().getActivationController().setListener(this);

            ((TextView) manual.findViewById(R.id.countrySelect)).setText(application.getKernel().getActivationController().getCurrentCountry().getTitle());
            ((TextView) manual.findViewById(R.id.phoneCode)).setText("+" +
                    application.getKernel().getActivationController().getCurrentCountry().getCallPrefix());

            onStateChanged(application.getKernel().getActivationController().getCurrentState());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (application.getKernel().getActivationController() != null) {
            application.getKernel().getActivationController().onPageHidden();
            application.getKernel().getActivationController().setListener(null);
        }
    }

    @Override
    public boolean onBackPressed() {
        if (currentState == ActivationController.STATE_ERROR_NETWORK) {
            application.getKernel().getActivationController().cancel();
            return true;
        }

        if (currentState == ActivationController.STATE_ERROR_UNKNOWN) {
            application.getKernel().getActivationController().cancel();
            return true;
        }

        if (currentState == ActivationController.STATE_ERROR_TOO_OFTEN) {
            application.getKernel().getActivationController().cancel();
            return true;
        }

        if (currentState == ActivationController.STATE_ERROR_WRONG_PHONE) {
            application.getKernel().getActivationController().cancel();
            return true;
        }

        if (currentState == ActivationController.STATE_ERROR_EXPIRED) {
            application.getKernel().getActivationController().cancel();
            return true;
        }
        return false;
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
            hideKeyboard(manual.findViewById(R.id.phoneName));
        }
        if (state == ActivationController.STATE_PHONE_EDIT) {
            showView(manual, currentState != ActivationController.STATE_START);
            showKeyboard((EditText) manual.findViewById(R.id.phoneName));
        } else {
            manual.setVisibility(View.GONE);
            hideKeyboard(manual.findViewById(R.id.phoneName));
        }

        // Network error
        if (currentState == ActivationController.STATE_ERROR_NETWORK) {
            hideView(networkError);
        }
        if (state == ActivationController.STATE_ERROR_NETWORK) {
            showView(networkError, currentState != ActivationController.STATE_START);
        } else {
            networkError.setVisibility(View.GONE);
        }

        // Unknown error
        if (currentState == ActivationController.STATE_ERROR_UNKNOWN) {
            hideView(unknownError);
        }
        if (state == ActivationController.STATE_ERROR_UNKNOWN) {
            showView(unknownError, currentState != ActivationController.STATE_START);
        } else {
            unknownError.setVisibility(View.GONE);
        }

        // Too often error
        if (currentState == ActivationController.STATE_ERROR_TOO_OFTEN) {
            hideView(tooOftenError);
        }
        if (state == ActivationController.STATE_ERROR_TOO_OFTEN) {
            showView(tooOftenError, currentState != ActivationController.STATE_START);
        } else {
            tooOftenError.setVisibility(View.GONE);
        }

        // wrong phone error
        if (currentState == ActivationController.STATE_ERROR_WRONG_PHONE) {
            hideView(wrongPhoneError);
        }
        if (state == ActivationController.STATE_ERROR_WRONG_PHONE) {
            showView(wrongPhoneError, currentState != ActivationController.STATE_START);
        } else {
            wrongPhoneError.setVisibility(View.GONE);
        }

        // wrong phone error
        if (currentState == ActivationController.STATE_ERROR_EXPIRED) {
            hideView(expiredError);
        }
        if (state == ActivationController.STATE_ERROR_EXPIRED) {
            showView(expiredError, currentState != ActivationController.STATE_START);
        } else {
            expiredError.setVisibility(View.GONE);
        }

        // Unable
//        if (currentState == ActivationController.STATE_ACTIVATION_ERROR_UNABLE) {
//            hideView(unableError);
//        }
//        if (state == ActivationController.STATE_ACTIVATION_ERROR_UNABLE) {
//            showView(unableError, currentState != ActivationController.STATE_START);
//        } else {
//            unableError.setVisibility(View.GONE);
//        }

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
            hideKeyboard((EditText) phoneActivation.findViewById(R.id.code));
        }
        if (state == ActivationController.STATE_MANUAL_ACTIVATION) {
            showView(phoneActivation, currentState != ActivationController.STATE_START);
            showKeyboard((EditText) phoneActivation.findViewById(R.id.code));
        } else {
            phoneActivation.setVisibility(View.GONE);
            hideKeyboard((EditText) phoneActivation.findViewById(R.id.code));
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

        if (state == ActivationController.STATE_ACTIVATED) {
            application.getKernel().setActivationController(null);
            ((StartActivity) getActivity()).onSuccessAuth();
        }

        currentState = state;
    }
}
