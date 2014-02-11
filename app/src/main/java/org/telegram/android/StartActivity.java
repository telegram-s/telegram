package org.telegram.android;

import android.app.ActivityOptions;
import android.app.FragmentManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.*;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.TypedValue;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.Toast;
import com.actionbarsherlock.view.MenuItem;
import com.google.analytics.tracking.android.EasyTracker;
import org.telegram.android.activity.ViewImageActivity;
import org.telegram.android.activity.ViewImagesActivity;
import org.telegram.android.base.SmileyActivity;
import org.telegram.android.core.model.media.TLLocalFileLocation;
import org.telegram.android.fragments.*;
import org.telegram.android.fragments.interfaces.FragmentResultController;
import org.telegram.android.fragments.interfaces.RootController;
import org.telegram.android.log.Logger;
import org.telegram.android.screens.FragmentScreenController;
import org.telegram.android.screens.RootControllerHolder;
import org.telegram.android.ui.pick.PickIntentClickListener;
import org.telegram.android.ui.pick.PickIntentDialog;
import org.telegram.android.ui.pick.PickIntentItem;
import org.telegram.android.ui.pick.PickPriority;
import org.telegram.integration.TestIntegration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Author: Korshakov Stepan
 * Created: 28.07.13 19:09
 */
public class StartActivity extends SmileyActivity implements FragmentResultController, RootControllerHolder {

    public static boolean isGuideShown = false;

    private static final String TAG = "StartActivity";

    private static final int REQUEST_OPEN_IMAGE = 300;

    public static final String ACTION_OPEN_SETTINGS = "org.telegram.android.OPEN_SETTINGS";
    public static final String ACTION_OPEN_CHAT = "org.telegram.android.OPEN";

    private boolean barVisible;

    private BroadcastReceiver logoutReceiver;

    private int lastResultCode = -1;
    private Object lastResultData;

    private FragmentScreenController controller;

    private boolean isStarted = false;

    @Override
    public RootController getRootController() {
        return controller;
    }

    public void onCreate(Bundle savedInstanceState) {
        long start = SystemClock.uptimeMillis();
        super.onCreate(savedInstanceState);

        getWindow().setBackgroundDrawableResource(R.drawable.transparent);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            getWindow().setFormat(PixelFormat.RGB_565);
        }

        setBarBg(true);
        getSupportActionBar().setLogo(R.drawable.st_bar_logo);
        getSupportActionBar().setIcon(R.drawable.st_bar_logo);
        getSupportActionBar().setDisplayUseLogoEnabled(true);

        setWindowContentOverlayCompat();

        setContentView(R.layout.activity_main);

        Bundle savedState = null;
        if (savedInstanceState != null && savedInstanceState.containsKey("screen_controller")) {
            savedState = savedInstanceState.getBundle("screen_controller");
        }
        controller = new FragmentScreenController(this, savedState);

        isStarted = false;

        if (savedInstanceState != null) {
            barVisible = savedInstanceState.getBoolean("barVisible");
            if (barVisible) {
                showBar();
            } else {
                hideBar();
            }
        } else {
            doInitApp(true);
        }

        Logger.d(TAG, "Kernel: Activity loaded in " + (SystemClock.uptimeMillis() - start) + " ms");
    }

    private void setWindowContentOverlayCompat() {
        if (Build.VERSION.SDK_INT == 18) {
            // Get the content view
            View contentView = findViewById(android.R.id.content);

            // Make sure it's a valid instance of a FrameLayout
            if (contentView instanceof FrameLayout) {
                TypedValue tv = new TypedValue();

                // Get the windowContentOverlay value of the current theme
                if (getTheme().resolveAttribute(
                        android.R.attr.windowContentOverlay, tv, true)) {

                    // If it's a valid resource, set it as the foreground drawable
                    // for the content view
                    if (tv.resourceId != 0) {
                        ((FrameLayout) contentView).setForeground(
                                getResources().getDrawable(tv.resourceId));
                    }
                }
            }
        }
    }

    public void doInitApp(boolean firstAttempt) {
        if (!application.getKernelsLoader().isLoaded()) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            if (firstAttempt) {
                transaction.add(R.id.fragmentContainer, new UpgradeFragment(), "recoverFragment");
            } else {
                transaction.replace(R.id.fragmentContainer, new UpgradeFragment(), "recoverFragment");
            }
            transaction.commit();
            hideBar();
            return;
        }

        if (application.getKernel().getAuthKernel().isLoggedIn()) {
            if (application.getEngine().getUser(application.getCurrentUid()) == null) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                if (firstAttempt) {
                    transaction.add(R.id.fragmentContainer, new RecoverFragment(), "recoverFragment");
                } else {
                    transaction.replace(R.id.fragmentContainer, new RecoverFragment(), "recoverFragment");
                }
                transaction.commit();
                hideBar();
                return;
            }
        }

        WhatsNewFragment.Definition[] definitions = prepareWhatsNew();
        if (definitions.length != 0) {
            application.getKernel().sendEvent("show_whats_new");
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragmentContainer, new WhatsNewFragment(definitions), "whatsNewFragment")
                    .commit();
            hideBar();
            return;
        }

        if (!application.isLoggedIn()) {
            if (!isGuideShown) {
                isGuideShown = true;
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                if (firstAttempt) {
                    transaction.add(R.id.fragmentContainer, new TourFragment(), "tourFragment");
                } else {
                    transaction.replace(R.id.fragmentContainer, new TourFragment(), "tourFragment");
                }
                transaction.commit();
                hideBar();
                return;
            }

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            if (firstAttempt) {
                transaction.add(R.id.fragmentContainer, new AuthFragment(), "loginFragment");
            } else {
                transaction.replace(R.id.fragmentContainer, new AuthFragment(), "loginFragment");
            }
            transaction.commit();
            showBar();
            return;
        }

        controller.openDialogs(true);
        onNewIntent(getIntent());

        showBar();
    }

    private WhatsNewFragment.Definition[] prepareWhatsNew() {
        ArrayList<WhatsNewFragment.Definition> definitions = new ArrayList<WhatsNewFragment.Definition>();

        int prevVersionCode = application.getVersionHolder().getPrevVersionInstalled();

        if (prevVersionCode == 0) {
            return new WhatsNewFragment.Definition[0];
        }

        // Current version
        if (prevVersionCode < 1040) {
            definitions.add(new WhatsNewFragment.Definition(getString(R.string.whats_new_spain_title),
                    new String[]{
                            getString(R.string.whats_new_spain_0),
                            getString(R.string.whats_new_spain_1),
                    },
                    getString(R.string.whats_new_spain_hint)));
        }

        if (prevVersionCode < 997) {
            definitions.add(new WhatsNewFragment.Definition(getString(R.string.whats_new_gif_title),
                    new String[]{
                            getString(R.string.whats_new_gif_0),
                            getString(R.string.whats_new_gif_1),
                            getString(R.string.whats_new_gif_2),
                            getString(R.string.whats_new_gif_3),
                            getString(R.string.whats_new_gif_4),
                            getString(R.string.whats_new_gif_5),
                    },
                    getString(R.string.whats_new_gif_hint)));
        }

        if (prevVersionCode < 732) {
            definitions.add(new WhatsNewFragment.Definition(getString(R.string.whats_contacts_title),
                    new String[]{
                            getString(R.string.whats_contacts_0),
                            getString(R.string.whats_contacts_1),
                            getString(R.string.whats_contacts_2),
                    }, null));
        }

        if (prevVersionCode < 672) {
            definitions.add(new WhatsNewFragment.Definition(getString(R.string.whats_new_design_title),
                    new String[]{
                            getString(R.string.whats_new_design_0),
                            getString(R.string.whats_new_design_1),
                            getString(R.string.whats_new_design_2),
                            getString(R.string.whats_new_design_3),
                    }, null));
        }
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

        return definitions.toArray(new WhatsNewFragment.Definition[0]);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (application.getKernelsLoader().isLoaded()) {
            application.getUiKernel().onConfigurationChanged();
        }
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

        if (ACTION_OPEN_SETTINGS.equals(intent.getAction())) {
            getRootController().openSettings();
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
                    if (intent.hasExtra(Intent.EXTRA_STREAM)) {
                        getRootController().sendDoc(intent.getParcelableExtra(Intent.EXTRA_STREAM).toString());
                    } else {
                        Toast.makeText(this, "Unsupported content", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                if (intent.hasExtra(Intent.EXTRA_STREAM)) {
                    getRootController().sendDoc(intent.getParcelableExtra(Intent.EXTRA_STREAM).toString());
                } else {
                    Toast.makeText(this, "Unsupported content", Toast.LENGTH_SHORT).show();
                }
            }
        }
        if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
            if (intent.getType() != null) {
                if (intent.getType().startsWith("image/")) {
                    ArrayList<Parcelable> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                    String[] uris2 = new String[uris.size()];
                    for (int i = 0; i < uris2.length; i++) {
                        uris2[i] = uris.get(i).toString();
                    }
                    getRootController().sendImages(uris2);
                } else {
                    if (intent.hasExtra(Intent.EXTRA_STREAM)) {
                        ArrayList<Parcelable> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                        String[] uris2 = new String[uris.size()];
                        for (int i = 0; i < uris2.length; i++) {
                            uris2[i] = uris.get(i).toString();
                        }
                        getRootController().sendDocs(uris2);
                    } else {
                        Toast.makeText(this, "Unsupported content", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                if (intent.hasExtra(Intent.EXTRA_STREAM)) {
                    ArrayList<Parcelable> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                    String[] uris2 = new String[uris.size()];
                    for (int i = 0; i < uris2.length; i++) {
                        uris2[i] = uris.get(i).toString();
                    }
                    getRootController().sendDocs(uris2);
                } else {
                    Toast.makeText(this, "Unsupported content", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("barVisible", barVisible);
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

    private void checkLogout() {
        if (!application.getKernelsLoader().isLoaded()) {
            return;
        }

        if (!application.isLoggedIn()) {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
            if (!(fragment instanceof AuthFragment) && !(fragment instanceof TourFragment)) {
                getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, new TourFragment())
                        .commit();
                getSupportFragmentManager().executePendingTransactions();
                hideBar();
            }
        }
    }

    public void onSuccessAuth() {
        controller.openDialogs(false);
        showBar();
    }

    public void openApp() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new AuthFragment(), "loginFragment")
                .commit();
        showBar();
    }

    @Override
    public void onBackPressed() {
        if (application.getKernelsLoader().isLoaded()) {
            application.getKernel().sendEvent("app_back");
        }

        if (controller != null) {
            if (!controller.doSystemBack()) {
                finish();
            }
        } else {
            finish();
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
    protected void onStart() {
        super.onStart();
        EasyTracker.getInstance(this).activityStart(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (application.getKernelsLoader().isLoaded()) {
            application.getUiKernel().onAppResume(this);
        }

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

        setBarBg(!isStarted);

        isStarted = true;

        TestIntegration.initActivity(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (application.getKernelsLoader().isLoaded()) {
            application.getUiKernel().onAppPause();
        }
        unregisterReceiver(logoutReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);
    }

    @Override
    public void setResult(int resultCode, Object data) {
        this.lastResultCode = resultCode;
        this.lastResultData = data;
    }

//    private void superStartActivityForResult(Intent intent, int requestCode) {
//        super.startActivityForResult(intent, requestCode);
//    }
//
//    private void superStartActivity(Intent intent) {
//        super.startActivity(intent);
//    }
//
//    @Override
//    public void startActivity(Intent intent) {
//        PickIntentItem[] items = createPickIntents(intent);
//        if (items.length == 0) {
//            Toast.makeText(this, R.string.st_error_no_app_for_file, Toast.LENGTH_SHORT).show();
//            return;
//        } else if (items.length == 1) {
//            superStartActivity(intent);
//        } else {
//            PickIntentItem[] pickIntentItems = createPickIntents(intent);
//            PickIntentDialog dialog = new PickIntentDialog(this, pickIntentItems, new PickIntentClickListener() {
//                @Override
//                public void onItemClicked(int index, PickIntentItem item) {
//                    superStartActivity(item.getIntent());
//                }
//            });
//            dialog.setTitle("Select?");
//            dialog.show();
//        }
//    }
//
//    @Override
//    public void startActivityForResult(Intent intent, final int requestCode) {
//        PickIntentItem[] items = createPickIntents(intent);
//        if (items.length == 0) {
//            Toast.makeText(this, R.string.st_error_no_app_for_file, Toast.LENGTH_SHORT).show();
//            return;
//        } else if (items.length == 1) {
//            superStartActivityForResult(intent, requestCode);
//        } else {
//            PickIntentItem[] pickIntentItems = createPickIntents(intent);
//            PickIntentDialog dialog = new PickIntentDialog(this, pickIntentItems, new PickIntentClickListener() {
//                @Override
//                public void onItemClicked(int index, PickIntentItem item) {
//                    superStartActivityForResult(item.getIntent(), requestCode);
//                }
//            });
//            dialog.setTitle("Select?");
//            dialog.show();
//        }
//    }
//
//    protected PickIntentItem[] createPickIntents(Intent intent) {
//        PackageManager pm = application.getPackageManager();
//        List<ResolveInfo> rList = pm.queryIntentActivities(intent, 0);
//
//        ArrayList<PickIntentItem> defaultRes = new ArrayList<PickIntentItem>();
//        ArrayList<PickIntentItem> prioritizedRes = new ArrayList<PickIntentItem>();
//
//        for (ResolveInfo info : rList) {
//
//            boolean isPrioritized = false;
//            for (int i = 0; i < PickPriority.PACKAGES_PRIORITY.length; i++) {
//                if (info.activityInfo.packageName.equals(PickPriority.PACKAGES_PRIORITY[i])) {
//                    isPrioritized = true;
//                    break;
//                }
//            }
//
//            PickIntentItem item = new PickIntentItem(info.loadIcon(pm), info.loadLabel(pm).toString());
//            Intent activityIntent = (Intent) intent.clone();
//            activityIntent.setClassName(info.activityInfo.packageName, info.activityInfo.name);
//            item.setIntent(activityIntent);
//            item.setTag(info);
//
//            if (isPrioritized) {
//                prioritizedRes.add(item);
//            } else {
//                defaultRes.add(item);
//            }
//        }
//
//        Collections.sort(prioritizedRes, new Comparator<PickIntentItem>() {
//            @Override
//            public int compare(PickIntentItem item, PickIntentItem item2) {
//                int index1 = 0, index2 = 0;
//                for (int i = 0; i < PickPriority.PACKAGES_PRIORITY.length; i++) {
//                    if (item.getIntent().getComponent().getPackageName().equals(PickPriority.PACKAGES_PRIORITY[i])) {
//                        index1 = i;
//                        break;
//                    }
//                }
//                for (int i = 0; i < PickPriority.PACKAGES_PRIORITY.length; i++) {
//                    if (item2.getIntent().getComponent().getPackageName().equals(PickPriority.PACKAGES_PRIORITY[i])) {
//                        index2 = i;
//                        break;
//                    }
//                }
//                return index1 - index2;
//            }
//        });
//
//        Collections.sort(defaultRes, new Comparator<PickIntentItem>() {
//            @Override
//            public int compare(PickIntentItem item, PickIntentItem item2) {
//                return item.getTitle().compareTo(item2.getTitle());
//            }
//        });
//
//
//        ArrayList<PickIntentItem> res = new ArrayList<PickIntentItem>();
//        res.addAll(prioritizedRes);
//        res.addAll(defaultRes);
//        return res.toArray(new PickIntentItem[res.size()]);
//    }

    @Override
    public int getResultCode() {
        return lastResultCode;
    }

    @Override
    public Object getResultData() {
        return lastResultData;
    }
}