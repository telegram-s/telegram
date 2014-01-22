package org.telegram.android.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.text.BidiFormatter;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.extradea.framework.images.tasks.UriImageTask;
import com.extradea.framework.images.ui.FastWebImageView;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.telegram.android.base.MediaReceiverFragment;
import org.telegram.android.R;
import org.telegram.android.StartActivity;
import org.telegram.android.activity.PickCountryActivity;
import org.telegram.android.login.ActivationController;
import org.telegram.android.login.ActivationListener;
import org.telegram.android.ui.TextUtil;

import java.io.File;

/**
 * Created by ex3ndr on 21.12.13.
 */
public class AuthFragment extends MediaReceiverFragment implements ActivationListener {

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
    private View wrongCodeError;
    private View signupPage;
    private View signupPageProgress;

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
        wrongCodeError = res.findViewById(R.id.wrongCodeError);
        signupPage = res.findViewById(R.id.signupPage);
        signupPageProgress = res.findViewById(R.id.completeSignupProgress);

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

        ((EditText) manual.findViewById(R.id.phoneName)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.toString().trim().length() > 0) {
                    manual.findViewById(R.id.doActivation).setEnabled(true);
                } else {
                    manual.findViewById(R.id.doActivation).setEnabled(false);
                }
            }
        });
        ((EditText) manual.findViewById(R.id.phoneName)).setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_DONE) {
                    if (textView.getText().toString().trim().length() > 0) {
                        doActivation();
                    }
                    return true;
                }
                return false;
            }
        });

        if (((EditText) manual.findViewById(R.id.phoneName)).getText().toString().trim().length() > 0) {
            manual.findViewById(R.id.doActivation).setEnabled(true);
        } else {
            manual.findViewById(R.id.doActivation).setEnabled(false);
        }

        manual.findViewById(R.id.doActivation).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doActivation();
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
                doSendCode();
            }
        });

        ((EditText) phoneActivation.findViewById(R.id.code)).setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_DONE) {
                    if (textView.getText().toString().trim().length() > 0) {
                        doSendCode();
                    }
                    return true;
                }
                return false;
            }
        });

        ((EditText) phoneActivation.findViewById(R.id.code)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.toString().trim().length() > 0) {
                    phoneActivation.findViewById(R.id.completePhoneActivation).setEnabled(true);
                } else {
                    phoneActivation.findViewById(R.id.completePhoneActivation).setEnabled(false);
                }
            }
        });

        if (((EditText) phoneActivation.findViewById(R.id.code)).getText().toString().trim().length() > 0) {
            phoneActivation.findViewById(R.id.completePhoneActivation).setEnabled(true);
        } else {
            phoneActivation.findViewById(R.id.completePhoneActivation).setEnabled(false);
        }

        wrongCodeError.findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.getKernel().getActivationController().cancel();
            }
        });

        FastWebImageView avatar = ((FastWebImageView) signupPage.findViewById(R.id.avatar));
        avatar.setLoadingDrawable(getResources().getDrawable(R.drawable.st_user_placeholder_grey));
        avatar.setScaleTypeImage(FastWebImageView.SCALE_TYPE_FIT_CROP);
        avatar.setScaleTypeEmpty(FastWebImageView.SCALE_TYPE_FIT_CROP);

        signupPage.findViewById(R.id.changeAvatarButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (application.getKernel().getActivationController().getManualAvatarUri() != null) {
                    requestPhotoChooserWithDelete(0);
                } else {
                    requestPhotoChooser(0);
                }
            }
        });

        signupPage.findViewById(R.id.completeSignup).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String firstName = ((EditText) signupPage.findViewById(R.id.firstName)).getText().toString();
                String lastName = ((EditText) signupPage.findViewById(R.id.lastName)).getText().toString();

                application.getKernel().getActivationController().setManualFirstname(firstName);
                application.getKernel().getActivationController().setManualLastname(lastName);


                if (firstName == null || firstName.trim().length() == 0) {
                    Toast.makeText(getActivity(), R.string.st_error_first_name_incorrect, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (lastName == null || lastName.trim().length() == 0) {
                    Toast.makeText(getActivity(), R.string.st_error_first_name_incorrect, Toast.LENGTH_SHORT).show();
                    return;
                }

                application.getKernel().getActivationController().doCompleteSignUp(firstName, lastName, application.getKernel().getActivationController().getManualAvatarUri());
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
        wrongCodeError.setVisibility(View.GONE);
        signupPage.setVisibility(View.GONE);
        signupPageProgress.setVisibility(View.GONE);

        return res;
    }

    private void doSendCode() {
        hideKeyboard(phoneActivation.findViewById(R.id.code));

        int code;
        try {
            code = Integer.parseInt(((TextView) phoneActivation.findViewById(R.id.code)).getText().toString());
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Wrong activation code", Toast.LENGTH_SHORT).show();
            return;
        }
        application.getKernel().getActivationController().doSendCode(code);
    }

    private void doActivation() {
        hideKeyboard(manual.findViewById(R.id.phoneName));

        String number = ((TextView) manual.findViewById(R.id.phoneName)).getText().toString();
        number = "+" +
                application.getKernel().getActivationController().getCurrentCountry().getCallPrefix() +
                number;

        try {
            final Phonenumber.PhoneNumber numberUtil = PhoneNumberUtil.getInstance().parse(number, application.getKernel().getActivationController().getCurrentCountry().getIso());
            if (PhoneNumberUtil.getInstance().isValidNumber(numberUtil)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setMessage(
                        getStringSafe(R.string.st_auth_confirm_phone)
                                .replace("\\n", "\n")
                                .replace("{0}", BidiFormatter.getInstance().unicodeWrap(PhoneNumberUtil.getInstance().format(numberUtil, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL))));
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
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setMessage(
                        getStringSafe(R.string.st_auth_incorrect_phone)
                                .replace("{0}", PhoneNumberUtil.getInstance()
                                        .format(numberUtil, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)));
                AlertDialog dialog = builder.create();
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
            }
        } catch (NumberParseException e) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setMessage(getStringSafe(R.string.st_auth_incorrect_phone).replace("{0}", number));
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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSherlockActivity().getSupportActionBar().setHomeButtonEnabled(false);
        getSherlockActivity().getSupportActionBar().setDisplayUseLogoEnabled(true);
        getSherlockActivity().getSupportActionBar().setDisplayShowHomeEnabled(true);
        switch (currentState) {
            default:
            case ActivationController.STATE_MANUAL_ACTIVATION:
            case ActivationController.STATE_MANUAL_ACTIVATION_REQUEST:
                getSherlockActivity().getSupportActionBar().setTitle(getStringSafe(R.string.st_auth_state_common));
                break;
            case ActivationController.STATE_PHONE_CONFIRM:
            case ActivationController.STATE_PHONE_EDIT:
            case ActivationController.STATE_ERROR_WRONG_PHONE:
                getSherlockActivity().getSupportActionBar().setTitle(getStringSafe(R.string.st_auth_state_phone));
                break;
            case ActivationController.STATE_ERROR_NETWORK:
                getSherlockActivity().getSupportActionBar().setTitle(getStringSafe(R.string.st_auth_state_error_connection));
                break;
            case ActivationController.STATE_ERROR_EXPIRED:
            case ActivationController.STATE_ERROR_TOO_OFTEN:
            case ActivationController.STATE_ERROR_UNKNOWN:
                getSherlockActivity().getSupportActionBar().setTitle(getStringSafe(R.string.st_auth_state_error));
                break;
            case ActivationController.STATE_SIGNUP:
            case ActivationController.STATE_SIGNUP_REQUEST:
                getSherlockActivity().getSupportActionBar().setTitle(getStringSafe(R.string.st_auth_state_creating));
                break;
            case ActivationController.STATE_MANUAL_ACTIVATION_SEND:
            case ActivationController.STATE_ACTIVATION:
            case ActivationController.STATE_REQUEST_CODE:
                getSherlockActivity().getSupportActionBar().setTitle(getStringSafe(R.string.st_auth_state_activation));
                break;
        }

        if (currentState == ActivationController.STATE_ACTIVATION) {
            inflater.inflate(R.menu.auth_menu, menu);
        }
        if (currentState == ActivationController.STATE_MANUAL_ACTIVATION) {
            if (application.getKernel().getActivationController() != null) {
                if (!application.getKernel().getActivationController().isPhoneRequested()) {
                    inflater.inflate(R.menu.auth_manual_menu, menu);
                }
            }
        }
    }

    private void updateBarTitle() {
        getSherlockActivity().invalidateOptionsMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.enterCode) {
            if (application.getKernel().getActivationController() != null) {
                application.getKernel().getActivationController().manualCodeEnter();
            }
            return true;
        }
        if (item.getItemId() == R.id.requestCall) {
            if (application.getKernel().getActivationController() != null) {
                application.getKernel().getActivationController().requestPhone();
            }
            return true;
        }
        return false;
    }

    @Override
    protected void onPhotoArrived(String fileName, int width, int height, int requestId) {
        Uri uri = Uri.fromFile(new File(fileName));

        application.getKernel().getActivationController().setManualAvatarUri(uri.toString());
        ((FastWebImageView) signupPage.findViewById(R.id.avatar)).requestTask(new UriImageTask(uri.toString()));
    }

    @Override
    protected void onPhotoArrived(Uri uri, int width, int height, int requestId) {
        application.getKernel().getActivationController().setManualAvatarUri(uri.toString());
        ((FastWebImageView) signupPage.findViewById(R.id.avatar)).requestTask(new UriImageTask(uri.toString()));
    }

    @Override
    protected void onPhotoDeleted(int requestId) {
        application.getKernel().getActivationController().setManualAvatarUri(null);
        ((FastWebImageView) signupPage.findViewById(R.id.avatar)).requestTask(null);
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

            if (application.getKernel().getActivationController().getManualCode() != null) {
                ((EditText) phoneActivation.findViewById(R.id.code)).setText(application.getKernel().getActivationController().getManualCode());
            }

            onStateChanged(application.getKernel().getActivationController().getCurrentState());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (application.getKernel().getActivationController() != null) {
            application.getKernel().getActivationController().onPageHidden();
            application.getKernel().getActivationController().setListener(null);

            if (currentState == ActivationController.STATE_SIGNUP) {
                application.getKernel().getActivationController().setManualFirstname(((EditText) signupPage.findViewById(R.id.firstName)).getText().toString());
                application.getKernel().getActivationController().setManualLastname(((EditText) signupPage.findViewById(R.id.lastName)).getText().toString());
            }

            if (currentState == ActivationController.STATE_PHONE_EDIT) {
                application.getKernel().getActivationController().setManualPhone(((EditText) manual.findViewById(R.id.phoneName)).getText().toString());
            }

            if (currentState == ActivationController.STATE_MANUAL_ACTIVATION) {
                application.getKernel().getActivationController().setManualCode(
                        ((EditText) phoneActivation.findViewById(R.id.code)).getText().toString());
            }
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

        if (currentState == ActivationController.STATE_ERROR_WRONG_CODE) {
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
        if ((currentState == ActivationController.STATE_ACTIVATION ||
                currentState == ActivationController.STATE_REQUEST_CODE) &&
                (state == ActivationController.STATE_ACTIVATION ||
                        state == ActivationController.STATE_REQUEST_CODE)) {
            hideView(progress);
        }
        if (state == ActivationController.STATE_ACTIVATION ||
                state == ActivationController.STATE_REQUEST_CODE) {
            showView(progress, currentState != ActivationController.STATE_START);
        } else {
            progress.setVisibility(View.GONE);
        }

        // Progress signup
        if (currentState == ActivationController.STATE_SIGNUP_REQUEST) {
            hideView(signupPageProgress);
        }
        if (state == ActivationController.STATE_SIGNUP_REQUEST) {
            showView(signupPageProgress, currentState != ActivationController.STATE_START);
        } else {
            signupPageProgress.setVisibility(View.GONE);
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

        // Wrong code error
        if (currentState == ActivationController.STATE_ERROR_WRONG_CODE) {
            hideView(wrongCodeError);
        }
        if (state == ActivationController.STATE_ERROR_WRONG_CODE) {
            showView(wrongCodeError, currentState != ActivationController.STATE_START);
        } else {
            wrongCodeError.setVisibility(View.GONE);
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

        // Signup
        if (currentState == ActivationController.STATE_SIGNUP) {
            application.getKernel().getActivationController().setManualFirstname(((EditText) signupPage.findViewById(R.id.firstName)).getText().toString());
            application.getKernel().getActivationController().setManualLastname(((EditText) signupPage.findViewById(R.id.lastName)).getText().toString());
            hideView(signupPage);
        }
        if (state == ActivationController.STATE_SIGNUP) {
            if (application.getKernel().getActivationController().getManualFirstname() != null ||
                    application.getKernel().getActivationController().getManualLastname() != null) {
                ((EditText) signupPage.findViewById(R.id.firstName)).setText(application.getKernel().getActivationController().getManualFirstname());
                ((EditText) signupPage.findViewById(R.id.lastName)).setText(application.getKernel().getActivationController().getManualLastname());
            } else {
                ((EditText) signupPage.findViewById(R.id.firstName)).setText(application.getKernel().getActivationController().getAutoFirstname());
                ((EditText) signupPage.findViewById(R.id.lastName)).setText(application.getKernel().getActivationController().getAutoLastname());
            }

            if (application.getKernel().getActivationController().getManualAvatarUri() != null) {
                ((FastWebImageView) signupPage.findViewById(R.id.avatar)).requestTask(new UriImageTask(application.getKernel().getActivationController().getManualAvatarUri()));
            } else {
                ((FastWebImageView) signupPage.findViewById(R.id.avatar)).requestTask(null);
            }

            showView(signupPage, currentState != ActivationController.STATE_START);
        } else {
            signupPage.setVisibility(View.GONE);
        }

        if (currentState == ActivationController.STATE_PHONE_EDIT) {
            application.getKernel().getActivationController().setManualPhone(((EditText) manual.findViewById(R.id.phoneName)).getText().toString());
        }
        if (state == ActivationController.STATE_PHONE_EDIT) {
            if (application.getKernel().getActivationController().getManualPhone() != null) {
                ((EditText) manual.findViewById(R.id.phoneName)).setText(application.getKernel().getActivationController().getManualPhone());
            }
        }

        if (state == ActivationController.STATE_ACTIVATED) {
            application.getKernel().setActivationController(null);
            ((StartActivity) getActivity()).onSuccessAuth();
        }

        currentState = state;

        updateBarTitle();
    }
}
