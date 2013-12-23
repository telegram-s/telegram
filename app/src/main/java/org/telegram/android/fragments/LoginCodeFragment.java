package org.telegram.android.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.text.BidiFormatter;
import android.text.Html;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import org.telegram.android.R;
import org.telegram.android.StartActivity;
import org.telegram.android.StelsFragment;
import org.telegram.android.login.AutoActivationListener;
import org.telegram.android.login.AutoActivationReceiver;
import org.telegram.android.tasks.AsyncAction;
import org.telegram.android.tasks.AsyncException;
import org.telegram.android.tasks.ProgressInterface;
import org.telegram.android.ui.TextUtil;
import org.telegram.android.ui.UiCyclic;
import org.telegram.api.auth.TLAuthorization;
import org.telegram.api.engine.RpcException;
import org.telegram.api.requests.TLRequestAuthSendCall;
import org.telegram.api.requests.TLRequestAuthSignIn;

/**
 * Author: Korshakov Stepan
 * Created: 28.07.13 2:19
 */
public class LoginCodeFragment extends StelsFragment implements ViewTreeObserver.OnGlobalLayoutListener {

    private String phoneHash;
    private String phoneNumber;
    private int sentTime;
    private AutoActivationReceiver receiver;

    private TextView counterTextView;
    private EditText codeEditText;

    private UiCyclic cyclic;

    private boolean codeSending = false;
    private boolean codeEntered = false;
    private boolean callSent = false;

    private View progressView;
    private View contentView;
    private ScrollView contentScroll;
    private View focus;
    private Button nextButton;
    private Button backButton;

    private int lastHeight = 0;

    public LoginCodeFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            phoneHash = savedInstanceState.getString("phoneHash");
            phoneNumber = savedInstanceState.getString("phoneNumber");
            sentTime = savedInstanceState.getInt("sentTime");
            codeEntered = savedInstanceState.getBoolean("sentTime");
            callSent = savedInstanceState.getBoolean("callSent");
            codeSending = savedInstanceState.getBoolean("codeSending");
        }
    }

    public LoginCodeFragment(String phoneNumber, String phoneHash) {
        this.phoneHash = phoneHash;
        this.phoneNumber = phoneNumber;
        this.sentTime = (int) (System.currentTimeMillis() / 1000);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            phoneHash = savedInstanceState.getString("phoneHash");
            phoneNumber = savedInstanceState.getString("phoneNumber");
            sentTime = savedInstanceState.getInt("sentTime");
            codeEntered = savedInstanceState.getBoolean("sentTime");
            callSent = savedInstanceState.getBoolean("callSent");
            codeSending = savedInstanceState.getBoolean("codeSending");
        }

        receiver = new AutoActivationReceiver(getActivity());
        receiver.startReceivingActivation(sentTime, new AutoActivationListener() {
            @Override
            public void onCodeReceived(final int code) {
                LoginCodeFragment.this.onCodeArrived(code);
            }
        });

        View res = inflater.inflate(R.layout.login_code, container, false);

        progressView = res.findViewById(R.id.progress);
        contentView = res.findViewById(R.id.content);
        focus = res.findViewById(R.id.focuser);
        nextButton = (Button) res.findViewById(R.id.doNext);
        backButton = (Button) res.findViewById(R.id.doBack);

        if (application.isRTL()) {
            nextButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.st_auth_next, 0, 0, 0);
        } else {
            nextButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.st_auth_next, 0);
        }

        contentScroll = (ScrollView) res.findViewById(R.id.containerScroll);
        contentScroll.getViewTreeObserver().addOnGlobalLayoutListener(this);

        setDefaultProgressInterface(new ProgressInterface() {
            @Override
            public void showContent() {
                if (contentView != null) {
                    contentView.setVisibility(View.VISIBLE);
                }
                if (nextButton != null) {
                    nextButton.setVisibility(View.VISIBLE);
                }
                if (backButton != null) {
                    backButton.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void hideContent() {
                if (contentView != null) {
                    contentView.setVisibility(View.GONE);
                }
                if (nextButton != null) {
                    nextButton.setVisibility(View.GONE);
                }
                if (backButton != null) {
                    backButton.setVisibility(View.GONE);
                }
            }

            @Override
            public void showProgress() {
                if (progressView != null) {
                    progressView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void hideProgress() {
                if (progressView != null) {
                    progressView.setVisibility(View.GONE);
                }
            }
        });

        String formattedPhone = TextUtil.formatPhone(phoneNumber);
        String message = getStringSafe(R.string.st_login_code_hint)
                .replace("{0}", "<b>" + BidiFormatter.getInstance().unicodeWrap(formattedPhone) + "</b>");
        ((TextView) res.findViewById(R.id.message)).setText(Html.fromHtml(message));

        res.findViewById(R.id.doNext).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doConfirmCode(codeEditText.getText().toString());
            }
        });

        codeEditText = (EditText) res.findViewById(R.id.code);

        codeEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    doConfirmCode(codeEditText.getText().toString());
                    return true;
                }
                return false;
            }
        });

        fixEditText(codeEditText);

        counterTextView = (TextView) res.findViewById(R.id.counter);


        counterTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestPhoneActivation();
            }
        });

        if (callSent) {
            counterTextView.setEnabled(false);
            counterTextView.setText(R.string.st_login_code_call_sent);
            counterTextView.setEnabled(false);
        } else {
            counterTextView.setEnabled(true);
        }

        focus.requestFocus();

        /*if (isLargeDisplay()) {
            showKeyboard(codeEditText);
        }*/
        // codeEditText.requestFocus();

        return res;
    }

    public void onCodeArrived(final int code) {
        sendEvent("arrived", "" + code);
        secureCallback(new Runnable() {
            @Override
            public void run() {
                if (!codeEntered && !codeSending) {
                    codeEditText.setText("" + code);
                    doConfirmCode("" + code);
                }
            }
        });
    }

    private void onTimeout(boolean fromBackground) {
        sendEvent("from_background", "from_bg: " + fromBackground);

        if (fromBackground || codeSending) {
            counterTextView.setText(Html.fromHtml("<font color='#006FC8'>" + getStringSafe(R.string.st_login_code_call_manual) + "</font>"));
            AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.st_login_code_call_header)
                    .setMessage(R.string.st_login_code_call_message)
                    .setNegativeButton(R.string.st_no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            sendEvent("request_phone_no");
                        }
                    })
                    .setPositiveButton(R.string.st_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            sendEvent("request_phone_yes");
                            secureCallback(new Runnable() {
                                @Override
                                public void run() {
                                    requestPhoneActivation();
                                }
                            });
                        }
                    }).create();
            dialog.setCanceledOnTouchOutside(true);
            dialog.show();
        } else {
            if (cyclic != null) {
                cyclic.stop();
                cyclic = null;
            }
            requestPhoneActivation();
        }
    }

    private void requestPhoneActivation() {
        if (callSent) {
            return;
        }

        counterTextView.setText(Html.fromHtml("<font color='#006FC8'>" + getStringSafe(R.string.st_login_code_call_in_progress) + "</font>"));
        if (cyclic != null) {
            cyclic.stop();
            cyclic = null;
        }

        runUiTask(new AsyncAction() {

            private boolean expired = false;

            @Override
            public void execute() throws AsyncException {
                try {
                    rpcRaw(new TLRequestAuthSendCall(phoneNumber, phoneHash));
                } catch (RpcException e) {
                    if (e.getErrorCode() == 400) {
                        String message = e.getErrorTag();
                        if (message.startsWith("PHONE_NUMBER_INVALID")) {
                            throw new AsyncException(getStringSafe(R.string.st_error_invalid_phone));
                        } else if (message.startsWith("PHONE_CODE_EXPIRED")) {
                            expired = true;
                            return;
                        }
                    }
                    throw new AsyncException(AsyncException.ExceptionType.UNKNOWN_ERROR);
                }
            }

            @Override
            public void afterExecute() {
                if (expired) {
                    Toast.makeText(getActivity(), R.string.st_login_code_expired, Toast.LENGTH_SHORT).show();
                    getActivity().onBackPressed();
                } else {
                    contentView.setVisibility(View.VISIBLE);
                    nextButton.setVisibility(View.VISIBLE);
                    backButton.setVisibility(View.VISIBLE);
                    counterTextView.setText(R.string.st_login_code_call_sent);
                    counterTextView.setEnabled(false);
                    callSent = true;
                }
            }
        });
    }

    public void doConfirmCode(final String code) {
        sendEvent("confirm_code", code);

        if (codeSending)
            return;
        codeSending = true;
        codeEntered = true;
        if (cyclic != null) {
            cyclic.stop();
            cyclic = null;
        }
        counterTextView.setText(Html.fromHtml("<font color='#006FC8'>" + getStringSafe(R.string.st_login_code_call_manual) + "</font>"));

        hideKeyboard(codeEditText);
        focus.requestFocus();

        runUiTask(new AsyncAction() {
            private TLAuthorization authorization;
            private boolean expired = false;

            @Override
            public void execute() throws AsyncException {
                try {
                    authorization = rpcRaw(new TLRequestAuthSignIn(phoneNumber, phoneHash, code));
                    application.getKernel().logIn(authorization);
                } catch (RpcException e) {
                    if (e.getErrorCode() == 400) {
                        if ("PHONE_CODE_EXPIRED".equals(e.getErrorTag())) {
                            expired = true;
                            return;
                        } else if ("PHONE_NUMBER_UNOCCUPIED".equals(e.getErrorTag())) {
                            return;
                        }
                    }
                    throw new AsyncException(e);
                }
            }

            @Override
            public void onCanceled() {
                codeSending = false;
                contentView.setVisibility(View.VISIBLE);
                nextButton.setVisibility(View.VISIBLE);
                backButton.setVisibility(View.VISIBLE);
                if (isLargeDisplay()) {
                    showKeyboard(codeEditText);
                }
            }

            @Override
            public void afterExecute() {
                codeSending = false;
                if (authorization != null) {
                    contentView.setVisibility(View.GONE);
                    ((StartActivity) getActivity()).onSuccessAuth();
                } else {
                    if (expired) {
                        Toast.makeText(getActivity(), R.string.st_login_code_expired, Toast.LENGTH_SHORT).show();
                        getActivity().onBackPressed();
                    } else {
                        ((StartActivity) getActivity()).doPerformSignup(phoneNumber, phoneHash, code);
                    }
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("phoneHash", phoneHash);
        outState.putString("phoneNumber", phoneNumber);
        outState.putInt("sentTime", sentTime);
        outState.putBoolean("codeEntered", codeEntered);
        outState.putBoolean("callSent", callSent);
        outState.putBoolean("codeSending", codeSending);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (cyclic != null) {
            cyclic.stop();
            cyclic = null;
        }

        if (!isLargeDisplay()) {
            hideKeyboard(codeEditText);
            focus.requestFocus();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (isLargeDisplay()) {
            showKeyboard(codeEditText);
        }

        if (!codeEntered && !callSent) {
            int secs = 59 - (int) (System.currentTimeMillis() / 1000 - sentTime);

            if (secs > 5) {
                if (cyclic == null) {
                    cyclic = new UiCyclic(this, new Runnable() {
                        @Override
                        public void run() {
                            int secs = 59 - (int) (System.currentTimeMillis() / 1000 - sentTime);
                            if (secs == 0) {
                                onTimeout(false);
                                if (cyclic != null) {
                                    cyclic.stop();
                                    cyclic = null;
                                }
                            } else {
                                counterTextView.setText(getStringSafe(R.string.st_login_code_timer).replace("{0}",
                                        TextUtil.formatDuration(secs)));
                            }
                        }
                    }, 1000);
                    cyclic.start();
                }
            } else {
                if (cyclic != null) {
                    cyclic.stop();
                    cyclic = null;
                }
                onTimeout(true);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (receiver != null) {
            receiver.stopReceivingActivation();
            receiver = null;
        }
        codeEditText = null;
        counterTextView = null;
        progressView = null;
        contentView = null;
        contentScroll.getViewTreeObserver().removeGlobalOnLayoutListener(this);
    }

    @Override
    public void onGlobalLayout() {
        if (contentScroll != null) {
            if (lastHeight != contentScroll.getHeight()) {
                lastHeight = contentScroll.getHeight();
                contentScroll.smoothScrollTo(0, contentScroll.getChildAt(0).getHeight() - contentScroll.getHeight());
            }
        }
    }

    private boolean isLargeDisplay() {
        return application.getResources().getDisplayMetrics().heightPixels > getPx(540);
    }
}