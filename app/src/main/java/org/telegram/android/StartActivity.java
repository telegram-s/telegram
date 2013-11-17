package org.telegram.android;

import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.*;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.TypedValue;
import android.view.*;
import android.widget.Toast;
import com.actionbarsherlock.view.MenuItem;
import org.telegram.android.core.model.PeerType;
import org.telegram.android.core.model.media.TLLocalFileLocation;
import org.telegram.android.fragments.*;
import org.telegram.android.fragments.interfaces.FragmentResultController;
import org.telegram.android.fragments.interfaces.RootController;
import org.telegram.android.screens.FragmentScreenController;
import org.telegram.android.screens.RootControllerHolder;
import org.telegram.android.screens.ScreenLogicType;
import org.telegram.api.TLUserSelf;
import org.telegram.api.auth.TLAuthorization;

import java.util.ArrayList;

/**
 * Author: Korshakov Stepan
 * Created: 28.07.13 19:09
 */
public class StartActivity extends StelsSmileyActivity implements FragmentResultController, RootControllerHolder {

    private static final int STATE_GENERAL = -1;
    private static final int STATE_TOUR = 0;
    private static final int STATE_LOGIN = 1;
    private static final int STATE_LOGIN_CODE = 2;
    private static final int STATE_SIGNUP = 3;

    private static final int REQUEST_OPEN_IMAGE = 300;

    public static final String ACTION_OPEN_SETTINGS = "org.telegram.android.OPEN_SETTINGS";
    public static final String ACTION_OPEN_CHAT = "org.telegram.android.OPEN";
    public static final String ACTION_LOGIN = "org.telegram.android.LOGIN";

    private boolean barVisible;

    private BroadcastReceiver logoutReceiver;

    private int lastResultCode = -1;
    private Object lastResultData;

    private FragmentScreenController controller;

    private int currentState = STATE_LOGIN;

    @Override
    public RootController getRootController() {
        return controller;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle savedState = null;
        if (savedInstanceState != null && savedInstanceState.containsKey("screen_controller")) {
            savedState = savedInstanceState.getBundle("screen_controller");
        }
        controller = new FragmentScreenController(this, savedState);

        getWindow().setBackgroundDrawableResource(R.drawable.transparent);

        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.st_bar_bg));
        getSupportActionBar().setLogo(R.drawable.st_bar_logo);
        getSupportActionBar().setIcon(R.drawable.st_bar_ic_search);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        setContentView(R.layout.dialogs_container);
        updateHeaderHeight();

        if (savedInstanceState == null) {
            if (application.getVersionHolder().isWasUpgraded()) {
                onInitUpdated();
            } else {
                onInitApp(true);
            }
        } else {
            currentState = savedInstanceState.getInt("currentState", STATE_LOGIN);
            barVisible = savedInstanceState.getBoolean("barVisible");
            if (barVisible) {
                showBar();
            } else {
                hideBar();
            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            getWindow().setFormat(PixelFormat.RGB_565);
        }

        if (getIntent().getAction() != null) {
            onIntent(getIntent());
        }
    }

    private void doInitApp() {

    }

    private void onInitUpdated() {
        ArrayList<WhatsNewFragment.Definition> definitions = new ArrayList<WhatsNewFragment.Definition>();

        int prevVersionCode = application.getVersionHolder().getPrevVersionInstalled();

        // Current version

        if (prevVersionCode < 517) {
            definitions.add(new WhatsNewFragment.Definition(getString(R.string.whats_new_arabic_title),
                    new String[]{
                            getString(R.string.whats_new_arabic_0),
                            getString(R.string.whats_new_arabic_1),
                            getString(R.string.whats_new_arabic_2),
                    },
                    getString(R.string.whats_new_arabic_hint)));

            definitions.add(new WhatsNewFragment.Definition(getString(R.string.whats_new_secret_title),
                    new String[]{
                            getString(R.string.whats_new_secret_0),
                            getString(R.string.whats_new_secret_1),
                            getString(R.string.whats_new_secret_2),
                            getString(R.string.whats_new_secret_3)
                    },
                    getString(R.string.whats_new_secret_hint)));
        }


        if (definitions.size() == 0) {
            onInitApp(true);
        } else {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragmentContainer, new WhatsNewFragment(definitions.toArray(new WhatsNewFragment.Definition[0])), "whatsNewFragment")
                    .commit();
            hideBar();
        }
    }

    public void closeWhatsNew() {
        onInitApp(false);
    }

    public void onInitApp(boolean first) {
        if (application.isLoggedIn()) {
            if (application.getEngine().getUser(application.getCurrentUid()) == null) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                if (first) {
                    transaction.add(R.id.fragmentContainer, new RecoverFragment(), "recoverFragment");
                } else {
                    transaction.replace(R.id.fragmentContainer, new RecoverFragment(), "recoverFragment");
                }
                transaction.commit();
                hideBar();
            } else {
                controller.openDialogs(true);
                showBar();
            }
            currentState = STATE_GENERAL;
        } else {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            if (first) {
                transaction.add(R.id.fragmentContainer, new TourFragment(), "tourFragment");
            } else {
                transaction.replace(R.id.fragmentContainer, new LoginFragment(), "loginFragment");
            }
            transaction.commit();
            currentState = STATE_LOGIN;
            hideBar();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        application.getUiKernel().onConfigurationChanged();
        updateHeaderHeight();
    }

    private void updateHeaderHeight() {
    }

    protected int getBarHeight() {
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            TypedValue tv = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true);
            return getResources().getDimensionPixelSize(tv.resourceId);
        } else {
            TypedValue tv = new TypedValue();
            getTheme().resolveAttribute(R.attr.actionBarSize, tv, true);
            return getResources().getDimensionPixelSize(tv.resourceId);
        }*/
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(R.attr.actionBarSize, tv, true);
        return getResources().getDimensionPixelSize(tv.resourceId);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        onIntent(intent);
    }

    private void onIntent(Intent intent) {
        if (ACTION_OPEN_CHAT.equals(intent.getAction())) {
            int peerId = intent.getIntExtra("peerId", 0);
            int peerType = intent.getIntExtra("peerType", 0);
            getRootController().openDialog(peerType, peerId);
        }

        if (ACTION_LOGIN.equals(intent.getAction())) {
            if (application.isLoggedIn()) {
                new AlertDialog.Builder(this).setMessage("Only one account allowed do you want to login to another one?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // TODO: Fix drop login
                                // application.dropLogin();
                            }
                        })
                        .setNegativeButton("No", null).show();
            }
        }

        if (ACTION_OPEN_SETTINGS.equals(intent.getAction())) {
            getRootController().openSettings();
        }

        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            if (intent.getData() != null) {
                if ("http".equals(intent.getData().getScheme())) {
                    if (currentState == STATE_LOGIN_CODE) {
                        try {
                            String segment = intent.getData().getLastPathSegment();
                            if (segment != null) {
                                int code = Integer.parseInt(segment);
                                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
                                if (fragment instanceof LoginCodeFragment) {
                                    ((LoginCodeFragment) fragment).onCodeArrived(code);
                                }
                            }
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }
                } else {
                    try {
                        Cursor c1 = getContentResolver().query(intent.getData(), null, null, null, null);
                        if (c1.moveToFirst()) {
                            int uid = c1.getInt(c1.getColumnIndex("DATA4"));
                            getRootController().openDialog(PeerType.PEER_USER, uid);
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
        }
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            if (intent.getType() != null) {
                if (intent.getType().startsWith("image/")) {
                    getRootController().sendImage(intent.getParcelableExtra(Intent.EXTRA_STREAM).toString());
                } else if (intent.getType().equals("text/plain")) {
                    getRootController().sendText(intent.getStringExtra(Intent.EXTRA_TEXT));
                } else if (intent.getType().startsWith("video/")) {
                    getRootController().sendVideo(intent.getParcelableExtra(Intent.EXTRA_STREAM).toString());
                } else {
                    Toast.makeText(this, "Unsupported content", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Unsupported content", Toast.LENGTH_SHORT).show();
            }
        }
        if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
            if (intent.getType() != null) {
                if (intent.getType().equals("image/*")) {
                    ArrayList<Parcelable> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                    String[] uris2 = new String[uris.size()];
                    for (int i = 0; i < uris2.length; i++) {
                        uris2[i] = uris.get(i).toString();
                    }
                    getRootController().sendImages(uris2);
                } else {
                    Toast.makeText(this, "Unsupported content", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Unsupported content", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("barVisible", barVisible);
        outState.putInt("currentState", currentState);
        outState.putBundle("screen_controller", controller.saveState());
    }

    public void openImage(int mid, int peerType, int peerId) {
        startActivityForResult(ViewImagesActivity.createIntent(mid, peerType, peerId, this), REQUEST_OPEN_IMAGE);
    }

    public void openImage(TLLocalFileLocation location) {
        startActivity(ViewImageActivity.createIntent(location, this));
    }

    public void openImageAnimated(int mid, int peerType, int peerId, View view, Bitmap preview, int x, int y) {
        if (Build.VERSION.SDK_INT >= 16) {
            Bundle bundle = ActivityOptions.makeThumbnailScaleUpAnimation(view, preview, x, y).toBundle();
            startActivityForResult(ViewImagesActivity.createIntent(mid, peerType, peerId, this), REQUEST_OPEN_IMAGE, bundle);
        } else {
            startActivityForResult(ViewImagesActivity.createIntent(mid, peerType, peerId, this), REQUEST_OPEN_IMAGE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OPEN_IMAGE && resultCode == RESULT_OK) {
            if (data != null && data.getIntExtra("forward_mid", 0) != 0) {
                getRootController().forwardMessage(data.getIntExtra("forward_mid", 0));
            }
        }
    }

    private FragmentTransaction prepareTransaction() {
        if (application.getScreenLogicType() == ScreenLogicType.SINGLE_ANIMATED) {
            return getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.fragment_open_enter, R.anim.fragment_open_exit, R.anim.fragment_close_enter, R.anim.fragment_close_exit);
        } else {
            return getSupportFragmentManager().beginTransaction();
        }
    }

    private void checkLogout() {
        if (!application.isLoggedIn()) {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
            if (!(fragment instanceof LoginFragment) && !(fragment instanceof LoginSignupFragment) &&
                    !(fragment instanceof LoginCodeFragment) && !(fragment instanceof TourFragment)) {
                getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, new LoginFragment())
                        .commit();
                getSupportFragmentManager().executePendingTransactions();
                currentState = STATE_LOGIN;
                hideBar();
            }
        }
    }

    public void doShowCode(String phone, String phoneHash) {
        prepareTransaction()
                .replace(R.id.fragmentContainer, new LoginCodeFragment(phone, phoneHash))
                .commit();
        getSupportFragmentManager().executePendingTransactions();
        currentState = STATE_LOGIN_CODE;
        hideBar();
    }

    public void doPerformSignup(String phone, String phoneHash, String code) {
        Fragment loginCodeFragment = getSupportFragmentManager().findFragmentByTag("loginCodeFragment");

        FragmentTransaction transaction = prepareTransaction();
        if (loginCodeFragment != null) {
            transaction.remove(loginCodeFragment);
        }
        transaction.replace(R.id.fragmentContainer, new LoginSignupFragment(phone, phoneHash, code));
        transaction.commit();
        currentState = STATE_SIGNUP;
        hideBar();
    }

    public void onSuccessAuth(TLAuthorization authorization) {
        if (ACTION_LOGIN.equals(getIntent().getAction())) {
            AccountAuthenticatorResponse response = getIntent().getExtras().getParcelable(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
            Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, ((TLUserSelf) authorization.getUser()).getPhone());
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, "org.telegram.android.account");
            response.onResult(result);
            finish();
            return;
        }

        controller.openDialogs(false);
        currentState = STATE_GENERAL;
        showBar();
    }

    public void openApp() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new LoginFragment(), "loginFragment")
                .commit();
        currentState = STATE_LOGIN;
    }

    @Override
    public void onBackPressed() {
        if (currentState == STATE_LOGIN_CODE || currentState == STATE_SIGNUP) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new LoginFragment())
                    .commit();
            currentState = STATE_LOGIN;
        } else {
            if (!((FragmentScreenController) controller).doSystemBack()) {
                finish();
            }
        }
    }

    public void showBar() {
        getSupportActionBar().show();
        barVisible = true;
    }

    public void hideBar() {
        getSupportActionBar().hide();
        barVisible = false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            controller.doUp();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        application.getUiKernel().onAppResume(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("org.telegram.android.ACTION_LOGOUT");
        logoutReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                checkLogout();
            }
        };
        registerReceiver(logoutReceiver, intentFilter);
        checkLogout();
        // getWindow().setBackgroundDrawableResource(R.drawable.transparent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        application.getUiKernel().onAppPause();
        unregisterReceiver(logoutReceiver);
        // getWindow().setBackgroundDrawableResource(R.drawable.smileys_bg);
    }

    @Override
    public void setResult(int resultCode, Object data) {
        this.lastResultCode = resultCode;
        this.lastResultData = data;
    }

    @Override
    public int getResultCode() {
        return lastResultCode;
    }

    @Override
    public Object getResultData() {
        return lastResultData;
    }
}