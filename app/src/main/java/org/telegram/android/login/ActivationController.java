package org.telegram.android.login;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyManager;
import org.telegram.android.R;
import org.telegram.android.StelsApplication;
import org.telegram.android.log.Logger;
import org.telegram.android.tasks.AsyncException;
import org.telegram.api.auth.TLAuthorization;
import org.telegram.api.auth.TLSentCode;
import org.telegram.api.engine.RpcCallback;
import org.telegram.api.requests.TLRequestAuthSendCode;
import org.telegram.api.requests.TLRequestAuthSignIn;
import org.telegram.config.ApiConfig;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ex3ndr on 22.12.13.
 */
public class ActivationController {
    private static final String TAG = "ActivationController";

    private static final Pattern REGEXP_PATTERN = Pattern.compile("[A-Z_0-9]+");

    private static String getErrorTag(String srcMessage) {
        if (srcMessage == null) {
            return "UNKNOWN";
        }
        Matcher matcher = REGEXP_PATTERN.matcher(srcMessage);
        if (matcher.find()) {
            return matcher.group();
        }
        return "UNKNOWN";
    }

    public static final int STATE_START = 0;
    public static final int STATE_PHONE_CONFIRM = 1;
    public static final int STATE_PHONE_EDIT = 2;
    public static final int STATE_ACTIVATION = 3;
    public static final int STATE_ACTIVATION_ERROR_NETWORK = 4;
    public static final int STATE_ACTIVATION_ERROR_UNABLE = 5;
    public static final int STATE_ACTIVATION_ERROR_WRONG_PHONE = 6;
    public static final int STATE_MANUAL_ACTIVATION = 7;
    public static final int STATE_SIGNUP = 8;
    public static final int STATE_ACTIVATED = 9;

    private ActivationListener listener;

    private StelsApplication application;
    private int currentState;
    private String phone;
    private String phoneCodeHash;
    private int sentTime;

    private Handler handler = new Handler(Looper.getMainLooper());

    private AutoActivationReceiver receiver;
    private AutoActivationListener autoListener;

    private Runnable cancelAutomatic = new Runnable() {
        @Override
        public void run() {
            cancelAutomatic();
        }
    };

    private static String findPhone(StelsApplication application) {
        TelephonyManager manager = (TelephonyManager) application.getSystemService(Context.TELEPHONY_SERVICE);
        AccountManager am = AccountManager.get(application);
        Account[] accounts = am.getAccounts();

        String foundedWaPhone = null;
        String foundedSysPhone = null;

        for (Account ac : accounts) {
            String acname = ac.name;
            String actype = ac.type;

            if (actype.equals("com.whatsapp")) {
                foundedWaPhone = acname;
                break;
            }
        }

        String telephonyNum = manager.getLine1Number();
        if (telephonyNum != null && telephonyNum.trim().length() > 0) {
            foundedSysPhone = telephonyNum;
        }

        if (foundedSysPhone != null && foundedWaPhone != null) {
            if (foundedSysPhone.equals(foundedWaPhone)) {
                application.getKernel().sendEvent("phone_both_eq", foundedWaPhone);
            } else {
                application.getKernel().sendEvent("phone_both_neq", foundedWaPhone + ", " + foundedSysPhone);
            }
        } else if (foundedWaPhone != null) {
            application.getKernel().sendEvent("phone_wa", foundedWaPhone);
        } else if (foundedSysPhone != null) {
            application.getKernel().sendEvent("phone_sys", foundedSysPhone);
        } else {
            application.getKernel().sendEvent("phone_none");
        }

        if (foundedWaPhone != null) {
            return foundedWaPhone;
        } else if (foundedSysPhone != null) {
            return foundedSysPhone;
        }

        return null;
    }

    public ActivationController(StelsApplication application) {
        this.application = application;
        this.phone = findPhone(application);
        if (this.phone != null) {
            this.currentState = STATE_PHONE_CONFIRM;
            Logger.d(TAG, "Founded phone: " + phone);
        } else {
            this.currentState = STATE_PHONE_EDIT;
            Logger.d(TAG, "Unable to find phone");
        }
    }

    public void doConfirmPhone() {
        Logger.d(TAG, "doConfirmPhone");
        if (this.currentState != STATE_PHONE_CONFIRM) {
            throw new RuntimeException("Invalid state for doConfirmPhone");
        }
        startActivation();
    }

    private void startActivation() {
        Logger.d(TAG, "startActivation");
        doChangeState(STATE_ACTIVATION);

        application.getApi().doRpcCall(new TLRequestAuthSendCode(phone, 0, ApiConfig.API_ID, ApiConfig.API_HASH,
                application.getString(R.string.st_lang)), 15000,
                new RpcCallback<TLSentCode>() {
                    @Override
                    public void onResult(final TLSentCode result) {
                        sentTime = (int) (System.currentTimeMillis() / 1000);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Logger.d(TAG, "onSmsSent");
                                phoneCodeHash = result.getPhoneCodeHash();
                                startCodeSearch();
                            }
                        });
                    }

                    @Override
                    public void onError(int errorCode, String message) {
                        if (errorCode == 0) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Logger.d(TAG, "onSmsSent error");
                                    doChangeState(STATE_ACTIVATION_ERROR_NETWORK);
                                }
                            });
                            return;
                        }

                        String tagError = getErrorTag(message);

                        if (errorCode == 303) {
                            int destDC;
                            if (tagError.startsWith("NETWORK_MIGRATE_")) {
                                destDC = Integer.parseInt(tagError.substring("NETWORK_MIGRATE_".length()));
                            } else if (tagError.startsWith("PHONE_MIGRATE_")) {
                                destDC = Integer.parseInt(tagError.substring("PHONE_MIGRATE_".length()));
                            } else if (tagError.startsWith("USER_MIGRATE_")) {
                                destDC = Integer.parseInt(tagError.substring("USER_MIGRATE_".length()));
                            } else {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Logger.d(TAG, "onSmsSent error");
                                        doChangeState(STATE_ACTIVATION_ERROR_NETWORK);
                                    }
                                });
                                return;
                            }
                            application.getApi().switchToDc(destDC);
                            startActivation();
                            return;
                        }

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Logger.d(TAG, "onSmsSent error");
                                doChangeState(STATE_ACTIVATION_ERROR_NETWORK);
                            }
                        });
                    }
                });
    }

    private void startCodeSearch() {
        Logger.d(TAG, "startCodeSearch");
        handler.postDelayed(cancelAutomatic, 30000);
        receiver = new AutoActivationReceiver(application);
        receiver.startReceivingActivation(sentTime, new AutoActivationListener() {
            @Override
            public void onCodeReceived(int code) {
                performAutomatic(code);
            }
        });
    }

    private void performAutomatic(int code) {
        Logger.d(TAG, "performAutomatic: " + code);

        handler.removeCallbacks(cancelAutomatic);

        application.getApi().doRpcCall(new TLRequestAuthSignIn(phone, phoneCodeHash, "" + code), 30000,
                new RpcCallback<TLAuthorization>() {
                    @Override
                    public void onResult(TLAuthorization result) {
                        Logger.d(TAG, "sign in completed");
                        application.getKernel().logIn(result);
                        doChangeState(STATE_ACTIVATED);
                    }

                    @Override
                    public void onError(int errorCode, String message) {
                        Logger.d(TAG, "sign in error");
                    }
                });
    }

    private void cancelAutomatic() {
        Logger.d(TAG, "cancelAutomatic");

        doChangeState(STATE_ACTIVATION_ERROR_UNABLE);
    }

    public int getCurrentState() {
        return currentState;
    }

    public ActivationListener getListener() {
        return listener;
    }

    public void setListener(ActivationListener listener) {
        this.listener = listener;
    }

    private void doChangeState(final int nState) {
        if (this.currentState != nState) {
            this.currentState = nState;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (listener != null) {
                        listener.onStateChanged(nState);
                    }
                }
            });
        }

    }
}