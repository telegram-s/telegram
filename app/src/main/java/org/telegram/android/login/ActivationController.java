package org.telegram.android.login;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyManager;
import org.telegram.android.R;
import org.telegram.android.StelsApplication;
import org.telegram.android.countries.Countries;
import org.telegram.android.countries.CountryRecord;
import org.telegram.android.log.Logger;
import org.telegram.api.auth.TLAuthorization;
import org.telegram.api.auth.TLSentCode;
import org.telegram.api.engine.RpcCallback;
import org.telegram.api.requests.TLRequestAuthSendCall;
import org.telegram.api.requests.TLRequestAuthSendCode;
import org.telegram.api.requests.TLRequestAuthSignIn;
import org.telegram.config.ApiConfig;
import org.telegram.tl.TLBool;

import java.util.Locale;
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
    public static final int STATE_ERROR_NETWORK = 4;
    public static final int STATE_ACTIVATION_ERROR_UNABLE = 5;
    public static final int STATE_ACTIVATION_ERROR_WRONG_PHONE = 6;
    public static final int STATE_MANUAL_ACTIVATION_REQUEST = 7;
    public static final int STATE_MANUAL_ACTIVATION = 8;
    public static final int STATE_MANUAL_ACTIVATION_SEND = 9;
    public static final int STATE_SIGNUP = 10;
    public static final int STATE_ACTIVATED = 11;
    public static final int STATE_EXPIRED = 12;

    public static final int ERROR_REQUEST_SMS = 0;
    public static final int ERROR_REQUEST_CALL = 1;
    public static final int ERROR_REQUEST_SEND = 2;

    private ActivationListener listener;

    private StelsApplication application;
    private int currentState;
    private CountryRecord currentCountry;
    private String autoPhone;
    private String currentPhone;
    private String manualPhone;
    private String phoneCodeHash;
    private int sentTime;
    private boolean isManualPhone;
    private int networkError;
    private int lastActivationCode;

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
        this.autoPhone = findPhone(application);
        if (this.autoPhone != null) {
            this.currentState = STATE_PHONE_CONFIRM;
            Logger.d(TAG, "Founded phone: " + autoPhone);
        } else {
            this.currentState = STATE_PHONE_EDIT;
            Logger.d(TAG, "Unable to find phone");
        }

        TelephonyManager manager = (TelephonyManager) application.getSystemService(Context.TELEPHONY_SERVICE);
        String country = null;
        if (manager.getSimCountryIso() != null && manager.getSimCountryIso().length() == 2) {
            country = manager.getSimCountryIso();
        } else if (manager.getNetworkCountryIso() != null && manager.getNetworkCountryIso().length() == 2) {
            country = manager.getNetworkCountryIso();
        } else {
            country = Locale.getDefault().getCountry();
        }

        for (int i = 0; i < Countries.COUNTRIES.length; i++) {
            if (!Countries.COUNTRIES[i].isSearchByName()) {
                continue;
            }
            if (Countries.COUNTRIES[i].getIso().equals(country)) {
                currentCountry = Countries.COUNTRIES[i];
                break;
            }
        }

        if (currentCountry == null) {
            for (int i = 0; i < Countries.COUNTRIES.length; i++) {
                if (!Countries.COUNTRIES[i].isSearchByName()) {
                    continue;
                }
                if (Countries.COUNTRIES[i].getIso().equals("us")) {
                    currentCountry = Countries.COUNTRIES[i];
                    break;
                }
            }
        }
    }

    public int getNetworkError() {
        return networkError;
    }

    public String getAutoPhone() {
        return autoPhone;
    }

    public String getManualPhone() {
        return manualPhone;
    }

    public void setManualPhone(String manualPhone) {
        this.manualPhone = manualPhone;
    }

    public void setCurrentCountry(CountryRecord currentCountry) {
        this.currentCountry = currentCountry;
    }

    public CountryRecord getCurrentCountry() {
        return currentCountry;
    }

    public void doConfirmPhone() {
        Logger.d(TAG, "doConfirmPhone");
        if (this.currentState != STATE_PHONE_CONFIRM) {
            throw new RuntimeException("Invalid state for doConfirmPhone");
        }
        currentPhone = autoPhone;
        isManualPhone = false;
        startActivation();
    }

    public void doEditPhone() {
        Logger.d(TAG, "doConfirmPhone");
        if (this.currentState != STATE_PHONE_CONFIRM) {
            throw new RuntimeException("Invalid state for doEditPhone");
        }

        doChangeState(STATE_PHONE_EDIT);
    }

    public void doActivation(String phone) {
        Logger.d(TAG, "doActivation");
        manualPhone = phone;
        isManualPhone = true;
        currentPhone = currentCountry.getCallPrefix() + manualPhone;
        startActivation();
    }

    public void doTryAgain() {
        if (this.currentState != STATE_ERROR_NETWORK) {
            return;
        }

        if (networkError == ERROR_REQUEST_SMS) {
            startActivation();
        } else if (networkError == ERROR_REQUEST_CALL) {
            requestPhone();
        } else if (networkError == ERROR_REQUEST_SEND) {
            sendPhoneRequest(lastActivationCode);
        }
    }

    public void cancel() {
        if (this.currentState == STATE_ERROR_NETWORK) {

            if (networkError == ERROR_REQUEST_SMS) {
                if (isManualPhone) {
                    doChangeState(STATE_PHONE_EDIT);
                } else {
                    doChangeState(STATE_PHONE_CONFIRM);
                }
            } else if (networkError == ERROR_REQUEST_CALL) {
                doChangeState(STATE_MANUAL_ACTIVATION);
            } else if (networkError == ERROR_REQUEST_SEND) {
                doChangeState(STATE_MANUAL_ACTIVATION);
            }
        }
    }

    private void startActivation() {
        Logger.d(TAG, "startActivation");
        doChangeState(STATE_ACTIVATION);

        application.getApi().doRpcCallNonAuth(new TLRequestAuthSendCode(currentPhone, 0, ApiConfig.API_ID, ApiConfig.API_HASH,
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
                                    networkError = ERROR_REQUEST_SMS;
                                    doChangeState(STATE_ERROR_NETWORK);
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
                                        networkError = ERROR_REQUEST_SMS;
                                        doChangeState(STATE_ERROR_NETWORK);
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
                                networkError = ERROR_REQUEST_SMS;
                                doChangeState(STATE_ERROR_NETWORK);
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
                performActivation(code);
            }
        });
    }

    private void performActivation(int code) {
        Logger.d(TAG, "performAutomatic: " + code);

        handler.removeCallbacks(cancelAutomatic);

        application.getApi().doRpcCallNonAuth(new TLRequestAuthSignIn(currentPhone, phoneCodeHash, "" + code), 30000,
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
                        cancelAutomatic();
                    }
                });
    }

    private void cancelAutomatic() {
        Logger.d(TAG, "cancelAutomatic");

        if (receiver != null) {
            receiver.stopReceivingActivation();
        }

        handler.removeCallbacks(cancelAutomatic);

        doChangeState(STATE_MANUAL_ACTIVATION);
    }

    public void requestPhone() {
        Logger.d(TAG, "requestPhone");

        doChangeState(STATE_MANUAL_ACTIVATION_REQUEST);

        application.getApi().doRpcCallNonAuth(new TLRequestAuthSendCall(currentPhone, phoneCodeHash), 30000,
                new RpcCallback<TLBool>() {
                    @Override
                    public void onResult(TLBool result) {
                        doChangeState(STATE_MANUAL_ACTIVATION);
                    }

                    @Override
                    public void onError(int errorCode, String message) {
                        networkError = ERROR_REQUEST_CALL;
                        doChangeState(STATE_ERROR_NETWORK);
                    }
                });
    }

    public void sendPhoneRequest(int code) {
        lastActivationCode = code;
        doChangeState(STATE_MANUAL_ACTIVATION_SEND);
        application.getApi().doRpcCallNonAuth(new TLRequestAuthSignIn(currentPhone, phoneCodeHash, "" + code), 30000,
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
                        networkError = ERROR_REQUEST_SEND;
                        doChangeState(STATE_ERROR_NETWORK);
                    }
                });
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