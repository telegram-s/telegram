package org.telegram.android.fragments;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.os.Bundle;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import org.telegram.android.R;
import org.telegram.android.StartActivity;
import org.telegram.android.StelsFragment;
import org.telegram.android.countries.Countries;
import org.telegram.android.countries.CountryRecord;
import org.telegram.android.tasks.AsyncAction;
import org.telegram.android.tasks.AsyncException;
import org.telegram.android.tasks.ProgressInterface;
import org.telegram.api.TLConfig;
import org.telegram.api.auth.TLSentCode;
import org.telegram.api.engine.RpcException;
import org.telegram.api.requests.TLRequestAuthSendCode;
import org.telegram.api.requests.TLRequestHelpGetConfig;
import org.telegram.config.ApiConfig;

/**
 * Author: Korshakov Stepan
 * Created: 28.07.13 1:57
 */
public class LoginFragment extends StelsFragment {

    private EditText phoneCode;
    private EditText phoneName;
    private View progressView;
    private View contentView;
    private View focus;
    private Button nextButton;

    private int currentId = -1;

    private String tryToFindPhone(TelephonyManager manager) {
        AccountManager am = AccountManager.get(getActivity());
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
                sendEvent("phone_both_eq", foundedWaPhone);
            } else {
                sendEvent("phone_both_neq", foundedWaPhone + ", " + foundedSysPhone);
            }
        } else if (foundedWaPhone != null) {
            sendEvent("phone_wa", foundedWaPhone);
        } else if (foundedSysPhone != null) {
            sendEvent("phone_sys", foundedSysPhone);
        } else {
            sendEvent("phone_none");
        }

        if (foundedWaPhone != null) {
            return foundedWaPhone;
        } else if (foundedSysPhone != null) {
            return foundedSysPhone;
        }

        return null;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View res = inflater.inflate(R.layout.login_start, container, false);

        phoneCode = (EditText) res.findViewById(R.id.phoneCode);
        phoneName = (EditText) res.findViewById(R.id.phoneName);
        progressView = res.findViewById(R.id.progress);
        contentView = res.findViewById(R.id.content);
        focus = res.findViewById(R.id.focuser);
        nextButton = (Button) res.findViewById(R.id.doNext);
        if (application.isRTL()) {
            nextButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.st_auth_next, 0, 0, 0);
        } else {
            nextButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.st_auth_next, 0);
        }

        fixEditText(phoneCode);
        fixEditText(phoneName);

        phoneName.addTextChangedListener(new PhoneNumberFormattingTextWatcher());

        setDefaultProgressInterface(new ProgressInterface() {
            @Override
            public void showContent() {
            }

            @Override
            public void hideContent() {

            }

            @Override
            public void showProgress() {
                if (progressView != null) {
                    progressView.setVisibility(View.VISIBLE);
                }
                if (contentView != null) {
                    contentView.setVisibility(View.INVISIBLE);
                }
                if (nextButton != null) {
                    nextButton.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void hideProgress() {
                if (progressView != null) {
                    progressView.setVisibility(View.GONE);
                }
                if (contentView != null) {
                    contentView.setVisibility(View.VISIBLE);
                }
                if (nextButton != null) {
                    nextButton.setVisibility(View.VISIBLE);
                }
            }
        });

        phoneCode.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    if (phoneName != null) {
                        phoneName.requestFocus();
                    }
                    return true;
                }
                return false;
            }
        });

        phoneName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (phoneCode != null) {
                        doLogin();
                    }
                    return true;
                }
                return false;
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doLogin();
            }
        });

        final Spinner countriesSpinner = ((Spinner) res.findViewById(R.id.countrySpinner));
        final ArrayAdapter<CountryRecord> adapter = new ArrayAdapter<CountryRecord>(getActivity(),
                R.layout.login_spinner_item, Countries.COUNTRIES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        countriesSpinner.setAdapter(adapter);
        countriesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i != currentId && i != adapter.getCount() - 1) {
                    phoneCode.setText("+" + Countries.COUNTRIES[i].getCallPrefix());
                    // phoneName.requestFocus();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        phoneCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String code = editable.toString().trim().replace(" ", "");
                if (code.startsWith("+")) {
                    code = code.substring(1).trim();
                }

                if (currentId >= 0 && currentId < Countries.COUNTRIES.length) {
                    if (code.equals(Countries.COUNTRIES[currentId].getCallPrefix() + "")) {
                        return;
                    }
                }

                try {
                    int codeNum = Integer.parseInt(code);
                    for (int i = 0; i < Countries.COUNTRIES.length; i++) {
                        // Try to resolve issue with same prefix code in kz
                        if (!Countries.COUNTRIES[i].isSearchByCode()) {
                            continue;
                        }

                        if (codeNum == Countries.COUNTRIES[i].getCallPrefix()) {
                            currentId = i;
                            countriesSpinner.setSelection(i);
                            return;
                        }
                    }
                } catch (Exception e) {

                }

                currentId = adapter.getCount() - 1;
                countriesSpinner.setSelection(adapter.getCount() - 1);
            }
        });


        TelephonyManager manager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);

        String number = tryToFindPhone(manager);

        boolean isInstalled = false;

        if (number != null) {
            if (number.startsWith("+")) {
                number = number.substring(1);
            }

            boolean founded = false;
            String countryNumber = number;
            for (int i = 0; i < Countries.COUNTRIES.length; i++) {
                // Try to resolve issue with same prefix code in kz
                if (Countries.COUNTRIES[i].isSearchByCode()) {
                    continue;
                }

                if (number.startsWith(Countries.COUNTRIES[i].getCallPrefix() + "")) {
                    countriesSpinner.setSelection(i);
                    phoneCode.setText("+" + Countries.COUNTRIES[i].getCallPrefix());
                    founded = true;
                    isInstalled = true;
                    countryNumber = number.substring((Countries.COUNTRIES[i].getCallPrefix() + "").length());
                    sendEvent("pre_phone_code", Countries.COUNTRIES[i].getIso() + ":" + Countries.COUNTRIES[i].getCallPrefix());
                    break;
                }
            }

            if (founded) {
                phoneName.setText(countryNumber);
            }
        }

        if (!isInstalled) {
            boolean founded = false;
            String country = null;
            if (manager.getSimCountryIso() != null) {
                country = manager.getSimCountryIso();
                sendEvent("sim_iso", country);
            } else if (manager.getNetworkCountryIso() != null) {
                country = manager.getNetworkCountryIso();
                sendEvent("network_iso", country);
            } else {
                sendEvent("no_iso");
            }

            if (country != null) {
                for (int i = 0; i < Countries.COUNTRIES.length; i++) {
                    if (!Countries.COUNTRIES[i].isSearchByName()) {
                        continue;
                    }
                    if (Countries.COUNTRIES[i].getIso().equals(country)) {
                        countriesSpinner.setSelection(i);
                        phoneCode.setText("+" + Countries.COUNTRIES[i].getCallPrefix());

                        sendEvent("pre_device_code", Countries.COUNTRIES[i].getIso() + ":" + Countries.COUNTRIES[i].getCallPrefix());

                        founded = true;
                        break;
                    }
                }
            }

            if (!founded) {
                for (int i = 0; i < Countries.COUNTRIES.length; i++) {
                    if (Countries.COUNTRIES[i].getIso().equals("us")) {
                        countriesSpinner.setSelection(i);
                        phoneCode.setText("+" + Countries.COUNTRIES[i].getCallPrefix());
                        break;
                    }
                }

                sendEvent("pre_us_code");
            }

            phoneName.setText("");
        }
        return res;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isLargeDisplay()) {
            showKeyboard(phoneName);
        }
    }

    protected void doLogin() {
        String countryCode = phoneCode.getText().toString();
        String localNumber = phoneName.getText().toString();
        sendEvent("doLogin", countryCode + " , " + localNumber);
        doLogin(countryCode + localNumber);
    }

    public void doLogin(final String _phone) {
        final String phone = _phone.startsWith("+") ? _phone.substring(1) : _phone;
        hideKeyboard(phoneName);
        hideKeyboard(phoneCode);
        focus.requestFocus();
        runUiTask(new AsyncAction() {

            private TLSentCode code;

            private void performAttempt() throws AsyncException {
                application.getAuthController().waitAuth(10000);
                try {
                    code = rpcRaw(
                            new TLRequestAuthSendCode(phone, 0, ApiConfig.API_ID, ApiConfig.API_HASH, getStringSafe(R.string.st_lang)));
                } catch (RpcException e) {
                    if (e.getErrorCode() == 303) {
                        int destDC;
                        if (e.getErrorTag().startsWith("NETWORK_MIGRATE_")) {
                            destDC = Integer.parseInt(e.getErrorTag().substring("NETWORK_MIGRATE_".length()));
                        } else if (e.getErrorTag().startsWith("PHONE_MIGRATE_")) {
                            destDC = Integer.parseInt(e.getErrorTag().substring("PHONE_MIGRATE_".length()));
                        } else if (e.getErrorTag().startsWith("USER_MIGRATE_")) {
                            destDC = Integer.parseInt(e.getErrorTag().substring("USER_MIGRATE_".length()));
                        } else {
                            throw new AsyncException(e);
                        }

                        if (application.getApiStorage().getAvailableConnections(destDC).length == 0) {
                            TLConfig config = rpc(new TLRequestHelpGetConfig());
                            application.getApi().resetConnectionInfo();
                            application.getApiStorage().updateSettings(config);
                        }

                        application.getKernel().getApiKernel().switchToDc(destDC);

                        performAttempt();
                        return;
                    }
                    throw new AsyncException(e);
                }
            }

            @Override
            public void onCanceled() {
                super.onCanceled();
                if (isLargeDisplay()) {
                    showKeyboard(phoneName);
                }
            }

            @Override
            public void execute() throws AsyncException {
                performAttempt();
            }

            @Override
            public void afterExecute() {
                ((StartActivity) getActivity()).doShowCode(phone, code.getPhoneCodeHash());
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!isLargeDisplay()) {
            hideKeyboard(phoneName);
            hideKeyboard(phoneCode);
            focus.requestFocus();
        }

        sendEvent("paused", phoneCode.getText().toString() + ", " + phoneName.getText().toString());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        phoneName = null;
        phoneCode = null;
        progressView = null;
        contentView = null;
        focus = null;
        nextButton = null;
    }

    private boolean isLargeDisplay() {
        return application.getResources().getDisplayMetrics().heightPixels > getPx(540);
    }
}