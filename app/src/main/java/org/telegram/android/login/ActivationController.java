package org.telegram.android.login;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import org.telegram.android.R;
import org.telegram.android.StartActivity;
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
import org.telegram.api.requests.TLRequestAuthSignUp;
import org.telegram.config.ApiConfig;
import org.telegram.tl.TLBool;

import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ex3ndr on 22.12.13.
 */
public class ActivationController {
    private static final String TAG = "ActivationController";
    private static final int NOTIFICATION_ID = 1001;

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
    public static final int STATE_REQUEST_CODE = 4;
    public static final int STATE_ACTIVATION = 5;
    public static final int STATE_ERROR_NETWORK = 6;
    public static final int STATE_ERROR_UNKNOWN = 7;
    public static final int STATE_ERROR_TOO_OFTEN = 8;
    public static final int STATE_ERROR_WRONG_PHONE = 9;
    public static final int STATE_ERROR_WRONG_CODE = 10;
    public static final int STATE_ERROR_EXPIRED = 11;
    public static final int STATE_MANUAL_ACTIVATION = 12;
    public static final int STATE_MANUAL_ACTIVATION_SEND = 13;
    public static final int STATE_MANUAL_ACTIVATION_REQUEST = 14;
    public static final int STATE_SIGNUP = 15;
    public static final int STATE_SIGNUP_REQUEST = 16;
    public static final int STATE_ACTIVATED = 17;

    public static final int ERROR_REQUEST_SMS = 0;
    public static final int ERROR_REQUEST_CALL = 1;
    public static final int ERROR_REQUEST_SEND = 2;
    public static final int ERROR_REQUEST_SIGNUP = 3;

    private static final int REQUEST_TIMEOUT = 30000;
    private static final int AUTO_TIMEOUT = 60000;

    private ActivationListener listener;

    private StelsApplication application;
    private int currentState;
    private CountryRecord currentCountry;
    private String autoPhone;
    private String currentPhone;
    private String manualPhone;
    private String phoneCodeHash;
    private String autoFirstname;
    private String autoLastname;

    private String manualFirstname;
    private String manualLastname;
    private String manualAvatarUri;
    private String manualCode;

    private int sentTime;
    private boolean isManualPhone;
    private boolean isManualActivation;
    private int networkError;
    private int lastActivationCode;

    private NotificationManager notificationManager;

    private Handler handler = new Handler(Looper.getMainLooper());

    private AutoActivationReceiver receiver;

    private Random rnd = new Random();

    private boolean isPageVisible = true;

    private Runnable cancelAutomatic = new Runnable() {
        @Override
        public void run() {
            Logger.d(TAG,"notify");
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
                foundedWaPhone = acname.replaceAll("[^\\d]", "");
                break;
            }
        }

        String telephonyNum = manager.getLine1Number();
        if (telephonyNum != null && telephonyNum.trim().length() > 0) {
            foundedSysPhone = telephonyNum.replaceAll("[^\\d]", "");
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
        } else if (Locale.getDefault().getCountry() != null) {
            country = Locale.getDefault().getCountry();
        } else {
            country = "us";
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

        notificationManager = (NotificationManager) application.getSystemService(Context.NOTIFICATION_SERVICE);


        if (Build.VERSION.SDK_INT >= 14) {
            try {
                Cursor c = application.getContentResolver().query(ContactsContract.Profile.CONTENT_URI, new String[]
                        {
                                "display_name_alt", "photo_uri"
                        }, null, null, null);

                if (c != null && c.moveToFirst()) {
                    String displayNameAlt = c.getString(0);
                    String pUri = c.getString(1);

                    if (displayNameAlt != null) {
                        String[] names = displayNameAlt.split(",", 2);
                        if (names.length == 1) {
                            autoFirstname = displayNameAlt.trim();
                            autoLastname = "";
                        } else if (names.length == 2) {
                            autoFirstname = names[0].trim();
                            autoLastname = names[1].trim();
                        }
                    }

                    if (pUri != null && pUri.length() > 0) {
                        manualAvatarUri = pUri;
                    }
                }
                c.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public boolean isManualActivation() {
        return isManualActivation;
    }

    public void onPageShown() {
        hideNotification();
        isPageVisible = true;
    }

    public void onPageHidden() {
        showNotification();
        isPageVisible = false;
    }

    private void showNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(application);
        builder.setSmallIcon(R.drawable.app_notify);
        builder.setContentTitle(application.getString(R.string.st_app_name));

        if (currentState == STATE_ACTIVATION) {
            builder.setTicker(application.getString(R.string.st_auth_not_activating_ticker));
            builder.setContentText(application.getString(R.string.st_auth_not_activating_content));
            builder.setProgress(0, 0, true);
            builder.setOngoing(true);
        } else if (currentState == STATE_ERROR_NETWORK) {
            builder.setTicker(application.getString(R.string.st_auth_not_connection_error_ticker));
            builder.setContentText(application.getString(R.string.st_auth_not_connection_error_content));
        } else if (currentState == STATE_ERROR_EXPIRED || currentState == STATE_ERROR_WRONG_CODE ||
                currentState == STATE_ERROR_UNKNOWN || currentState == STATE_ERROR_WRONG_PHONE ||
                currentState == STATE_ERROR_TOO_OFTEN) {
            builder.setTicker(application.getString(R.string.st_auth_not_error_ticker));
            builder.setContentText(application.getString(R.string.st_auth_not_error_content));
        } else if (currentState == STATE_MANUAL_ACTIVATION) {
            builder.setTicker(application.getString(R.string.st_auth_not_activating_code_ticker));
            builder.setContentText(application.getString(R.string.st_auth_not_activating_code_content));
        } else if (currentState == STATE_SIGNUP) {
            builder.setTicker(application.getString(R.string.st_auth_not_activating_signup_ticker));
            builder.setContentText(application.getString(R.string.st_auth_not_activating_signup_content));
        } else if (currentState == STATE_ACTIVATED) {
            builder.setTicker(application.getString(R.string.st_auth_not_activating_complete_ticker));
            builder.setContentText(application.getString(R.string.st_auth_not_activating_complete_content));
        } else {
            builder = null;
        }

        if (builder == null) {
            notificationManager.cancel(NOTIFICATION_ID);
        } else {
            builder.setContentIntent(
                    PendingIntent.getActivity(application, 0, new Intent().setClass(application, StartActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT));
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    private void hideNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
    }

    public String getManualCode() {
        return manualCode;
    }

    public void setManualCode(String manualCode) {
        this.manualCode = manualCode;
    }

    public String getAutoFirstname() {
        return autoFirstname;
    }

    public String getAutoLastname() {
        return autoLastname;
    }

    public String getManualFirstname() {
        return manualFirstname;
    }

    public void setManualFirstname(String manualFirstname) {
        this.manualFirstname = manualFirstname;
    }

    public String getManualLastname() {
        return manualLastname;
    }

    public void setManualLastname(String manualLastname) {
        this.manualLastname = manualLastname;
    }

    public String getManualAvatarUri() {
        return manualAvatarUri;
    }

    public void setManualAvatarUri(String manualAvatarUri) {
        this.manualAvatarUri = manualAvatarUri;
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
        if (this.currentState != STATE_ERROR_NETWORK && this.currentState != STATE_ERROR_UNKNOWN) {
            return;
        }

        if (networkError == ERROR_REQUEST_SMS) {
            startActivation();
        } else if (networkError == ERROR_REQUEST_CALL) {
            requestPhone();
        } else if (networkError == ERROR_REQUEST_SEND) {
            doSendCode(lastActivationCode);
        }
    }

    public void cancel() {
        if (this.currentState == STATE_ERROR_NETWORK || this.currentState == STATE_ERROR_TOO_OFTEN ||
                this.currentState == STATE_ERROR_UNKNOWN ||
                this.currentState == STATE_ERROR_WRONG_PHONE) {
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
        } else if (this.currentState == STATE_ERROR_EXPIRED) {
            if (isManualPhone) {
                doChangeState(STATE_PHONE_EDIT);
            } else {
                doChangeState(STATE_PHONE_CONFIRM);
            }
        } else if (this.currentState == STATE_ERROR_WRONG_CODE) {
            doChangeState(STATE_MANUAL_ACTIVATION);
        }
    }

    private void startActivation() {
        Logger.d(TAG, "startActivation");
        doChangeState(STATE_REQUEST_CODE);

        sentTime = (int) (System.currentTimeMillis() / 1000);
        application.getApi().doRpcCallNonAuth(new TLRequestAuthSendCode(currentPhone, 0, ApiConfig.API_ID, ApiConfig.API_HASH,
                application.getString(R.string.st_lang)), REQUEST_TIMEOUT,
                new RpcCallback<TLSentCode>() {
                    @Override
                    public void onResult(final TLSentCode result) {
                        doChangeState(STATE_ACTIVATION);
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
                            networkError = ERROR_REQUEST_SMS;
                            doChangeState(STATE_ERROR_NETWORK);
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
                                networkError = ERROR_REQUEST_SMS;
                                doChangeState(STATE_ERROR_UNKNOWN);
                                return;
                            }
                            application.getApi().switchToDc(destDC);
                            startActivation();
                            return;
                        }

                        if (tagError.equals("PHONE_NUMBER_INVALID")) {
                            networkError = ERROR_REQUEST_SMS;
                            doChangeState(STATE_ERROR_WRONG_PHONE);
                            return;
                        }

                        if (tagError.startsWith("FLOOD_WAIT")) {
                            networkError = ERROR_REQUEST_SMS;
                            doChangeState(STATE_ERROR_TOO_OFTEN);
                            return;
                        }

                        Logger.d(TAG, "onSmsSent error");
                        networkError = ERROR_REQUEST_SMS;
                        doChangeState(STATE_ERROR_UNKNOWN);
                    }
                });
    }

    public void manualCodeEnter() {
        cancelAutomatic();
        doChangeState(STATE_MANUAL_ACTIVATION);
        isManualActivation = true;
    }

    private void startCodeSearch() {
        Logger.d(TAG, "startCodeSearch");
        handler.postDelayed(cancelAutomatic, AUTO_TIMEOUT);
        receiver = new AutoActivationReceiver(application);
        receiver.startReceivingActivation(sentTime, new AutoActivationListener() {
            @Override
            public void onCodeReceived(int code) {
                performAutoActivation(code);
            }
        });
    }

    private void performAutoActivation(int code) {
        Logger.d(TAG, "performAutomatic: " + code);

        lastActivationCode = code;

        handler.removeCallbacks(cancelAutomatic);

        application.getApi().doRpcCallNonAuth(new TLRequestAuthSignIn(currentPhone, phoneCodeHash, "" + code), REQUEST_TIMEOUT,
                new RpcCallback<TLAuthorization>() {
                    @Override
                    public void onResult(TLAuthorization result) {
                        application.getKernel().logIn(result);
                        doChangeState(STATE_ACTIVATED);
                        // doChangeState(STATE_SIGNUP);
                    }

                    @Override
                    public void onError(int errorCode, String message) {
                        cancelAutomatic();
                        requestPhone();
                    }
                });
    }

    private void cancelAutomatic() {
        Logger.d(TAG, "cancelAutomatic");

        if (receiver != null) {
            receiver.stopReceivingActivation();
        }

        handler.removeCallbacks(cancelAutomatic);

//        if (currentState == STATE_ACTIVATION) {
//            requestPhone();
//        }
//        } else {
//            doChangeState(STATE_MANUAL_ACTIVATION);
//        }
    }

    public void requestPhone() {
        Logger.d(TAG, "requestPhone");

        doChangeState(STATE_MANUAL_ACTIVATION_REQUEST);

        application.getApi().doRpcCallNonAuth(new TLRequestAuthSendCall(currentPhone, phoneCodeHash), REQUEST_TIMEOUT,
                new RpcCallback<TLBool>() {
                    @Override
                    public void onResult(TLBool result) {
                        doChangeState(STATE_MANUAL_ACTIVATION);
                    }

                    @Override
                    public void onError(int errorCode, String message) {
                        if (errorCode == 0) {
                            networkError = ERROR_REQUEST_CALL;
                            doChangeState(STATE_ERROR_NETWORK);
                            return;
                        }

                        String tagError = getErrorTag(message);

                        if (tagError.equals("PHONE_CODE_EXPIRED")) {
                            doChangeState(STATE_ERROR_EXPIRED);
                            return;
                        }

                        if (tagError.equals("PHONE_NUMBER_INVALID")) {
                            doChangeState(STATE_ERROR_WRONG_PHONE);
                            return;
                        }

                        if (tagError.equals("PHONE_CODE_INVALID") || tagError.equals("PHONE_CODE_EMPTY")) {
                            doChangeState(STATE_ERROR_WRONG_CODE);
                            return;
                        }

                        networkError = ERROR_REQUEST_CALL;
                        doChangeState(STATE_ERROR_UNKNOWN);
                    }
                });
    }

    public void doCompleteSignUp(String firstName, String lastName, String uri) {
        manualAvatarUri = uri;
        manualFirstname = firstName;
        manualLastname = lastName;

        doChangeState(STATE_SIGNUP_REQUEST);

        application.getApi().doRpcCallNonAuth(new TLRequestAuthSignUp(currentPhone, phoneCodeHash, lastActivationCode + "", firstName, lastName),
                REQUEST_TIMEOUT, new RpcCallback<TLAuthorization>() {
            @Override
            public void onResult(TLAuthorization result) {
                application.getKernel().logIn(result);
                doChangeState(STATE_ACTIVATED);
            }

            @Override
            public void onError(int errorCode, String message) {
                if (errorCode == 0) {
                    networkError = ERROR_REQUEST_SIGNUP;
                    doChangeState(STATE_ERROR_NETWORK);
                    return;
                }

                String tagError = getErrorTag(message);

                if (tagError.equals("PHONE_NUMBER_OCCUPIED")) {
                    doSendCode(lastActivationCode);
                    return;
                }

                if (tagError.equals("PHONE_CODE_INVALID") || tagError.equals("PHONE_CODE_EMPTY")) {
                    doChangeState(STATE_ERROR_WRONG_CODE);
                    return;
                }

                networkError = ERROR_REQUEST_SIGNUP;
                doChangeState(STATE_ERROR_UNKNOWN);
            }
        });
    }

    public void doSendCode(int code) {
        lastActivationCode = code;
        doChangeState(STATE_MANUAL_ACTIVATION_SEND);
        application.getApi().doRpcCallNonAuth(new TLRequestAuthSignIn(currentPhone, phoneCodeHash, "" + code), REQUEST_TIMEOUT,
                new RpcCallback<TLAuthorization>() {
                    @Override
                    public void onResult(TLAuthorization result) {
                        application.getKernel().logIn(result);
                        doChangeState(STATE_ACTIVATED);
                    }

                    @Override
                    public void onError(int errorCode, String message) {
                        if (errorCode == 0) {
                            networkError = ERROR_REQUEST_SEND;
                            doChangeState(STATE_ERROR_NETWORK);
                            return;
                        }

                        String tagError = getErrorTag(message);

                        if (tagError.equals("PHONE_CODE_INVALID") || tagError.equals("PHONE_CODE_EMPTY")) {
                            doChangeState(STATE_ERROR_EXPIRED);
                            return;
                        }

                        if (tagError.equals("PHONE_CODE_INVALID")) {
                            doChangeState(STATE_ERROR_WRONG_CODE);
                            return;
                        }

                        if (tagError.equals("PHONE_NUMBER_INVALID")) {
                            doChangeState(STATE_ERROR_WRONG_PHONE);
                            return;
                        }

                        if (tagError.equals("PHONE_NUMBER_UNOCCUPIED")) {
                            doChangeState(STATE_SIGNUP);
                            return;
                        }

                        networkError = ERROR_REQUEST_SEND;
                        doChangeState(STATE_ERROR_UNKNOWN);
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
            if (!isPageVisible) {
                showNotification();
            }
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