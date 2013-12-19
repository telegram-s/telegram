package org.telegram.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import com.actionbarsherlock.app.SherlockFragment;
import org.telegram.android.fragments.interfaces.RootController;
import org.telegram.android.fragments.interfaces.SmileysController;
import org.telegram.android.screens.RootControllerHolder;
import org.telegram.android.tasks.*;
import org.telegram.android.ui.EmojiListener;
import org.telegram.android.ui.pick.PickIntentItem;
import org.telegram.android.ui.plurals.PluralResources;
import org.telegram.api.engine.RpcException;
import org.telegram.api.engine.TimeoutException;
import org.telegram.tl.TLMethod;
import org.telegram.tl.TLObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Author: Korshakov Stepan
 * Created: 28.07.13 2:08
 */
public class StelsBaseFragment extends SherlockFragment implements EmojiListener {
    private static final ExecutorService service = Executors.newFixedThreadPool(5, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread res = new Thread(runnable);
            res.setName("StelsFragmentService#" + res.hashCode());
            return res;
        }
    });

    private CallbackHandler callbackHandler = new CallbackHandler() {
        @Override
        public void receiveCallback(final Runnable runnable) {
            handler.post(
                    new Runnable() {
                        @Override
                        public String toString() {
                            return "handlerCallback";
                        }

                        @Override
                        public void run() {
                            if (barrier.isPaused()) {
                                barrier.sendCallback(runnable);
                            } else {
                                runnable.run();
                            }
                        }
                    });
        }
    };
    private CallBarrier barrier = new CallBarrier(callbackHandler);
    private ProgressInterface dialogProgressInterface = new ProgressInterface() {
        @Override
        public void showContent() {

        }

        @Override
        public void hideContent() {

        }

        private ProgressDialog dialog;

        @Override
        public void showProgress() {
            dialog = new ProgressDialog(getActivity());
            dialog.setMessage(getStringSafe(R.string.st_loading));
            dialog.setCancelable(false);
            dialog.show();
        }

        @Override
        public void hideProgress() {
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
        }
    };
    private ProgressInterface defaultProgressInterface = dialogProgressInterface;
    protected StelsApplication application;
    private Handler handler = new Handler(Looper.getMainLooper());
    private DisplayMetrics metrics;

    private RootController rootController;

    private SmileysController smileysController;

    private PluralResources pluralResources;

    private RecoverCallback dialogRecoverCallback = new RecoverCallback() {
        @Override
        public void onError(AsyncException e, final Runnable onRepeat, final Runnable onCancel) {
            String errorMessage = e.getMessage();
            if (e.getType() != null) {
                switch (e.getType()) {
                    default:
                    case UNKNOWN_ERROR:
                        errorMessage = getStringSafe(R.string.st_error_unknown);
                        break;
                    case CONNECTION_ERROR:
                        errorMessage = getStringSafe(R.string.st_error_connection);
                        break;
                    case INTERNAL_SERVER_ERROR:
                        errorMessage = getStringSafe(R.string.st_error_server);
                        break;
                }
            }

            if (e.isRepeatable()) {
                AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle(R.string.st_error_title).setMessage(errorMessage).setPositiveButton(R.string.st_repeat, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        onRepeat.run();
                    }
                }).setNegativeButton(R.string.st_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        onCancel.run();
                    }
                }).create();
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
            } else {
                AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle(R.string.st_error_title).setMessage(errorMessage).setPositiveButton(R.string.st_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        onCancel.run();
                    }
                }).create();
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
            }
        }
    };


    private void ensureApp() {
        if (application == null) {
            application = (StelsApplication) getActivity().getApplication();
        }

        rootController = null;
        smileysController = null;

        if (getActivity() instanceof RootController) {
            rootController = (RootController) getActivity();
        } else if (getActivity() instanceof RootControllerHolder) {
            rootController = ((RootControllerHolder) getActivity()).getRootController();
        }

        if (getActivity() instanceof SmileysController) {
            smileysController = (SmileysController) getActivity();
        }

        try {
            pluralResources = new PluralResources(getStringSafe(R.string.st_lang), getResources());
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ensureApp();
    }

    public String getQuantityString(int id, int quantity) {
        if (pluralResources == null) {
            return application.getResources().getQuantityString(id, quantity);
        } else {
            return pluralResources.getQuantityString(id, quantity);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ensureApp();
        metrics = getResources().getDisplayMetrics();
        setHasOptionsMenu(true);
    }


    protected RootController getRootController() {
        return rootController;
    }

    public SmileysController getSmileysController() {
        return smileysController;
    }

    public ProgressInterface getDefaultProgressInterface() {
        return defaultProgressInterface;
    }

    public void setDefaultProgressInterface(ProgressInterface defaultProgressInterface) {
        this.defaultProgressInterface = defaultProgressInterface;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        barrier.pause();
    }

    @Override
    public void onPause() {
        super.onPause();
        barrier.pause();
        application.getEmojiProcessor().unregiterListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        ensureApp();
        if (!application.getEmojiProcessor().isLoaded()) {
            application.getEmojiProcessor().regiterListener(this);
        }
        barrier.resume();
        if (Build.VERSION.SDK_INT >= 18) {
            postDelayerWeak(new Runnable() {
                @Override
                public void run() {
                    safeOnResume();
                }
            }, 200);
        } else {
            safeOnResume();
        }
    }

    protected void safeOnResume() {

    }

    protected int getBarHeight() {
        TypedValue tv = new TypedValue();
        getActivity().getTheme().resolveAttribute(R.attr.actionBarSize, tv, true);
        return getResources().getDimensionPixelSize(tv.resourceId);
    }

    protected boolean isSlow() {
        return application.isSlow();
    }

    public void runUiTask(final AsyncAction task) {
        asyncTask(task, dialogRecoverCallback, defaultProgressInterface);
    }

    public void runUiTask(final AsyncAction task, RecoverCallback recoverCallback) {
        asyncTask(task, recoverCallback, defaultProgressInterface);
    }

    public void runUiTask(final AsyncAction task, ProgressInterface progressInterface) {
        asyncTask(task, dialogRecoverCallback, progressInterface);
    }

    private void asyncTask(final AsyncAction action,
                           final RecoverCallback recoverCallback,
                           final ProgressInterface taskProgressInterface) {
        secureCallback(new Runnable() {

            @Override
            public String toString() {
                return "asyncTaskOuter";
            }

            @Override
            public void run() {
                if (taskProgressInterface != null) {
                    taskProgressInterface.hideContent();
                    taskProgressInterface.showProgress();
                }
                action.beforeExecute();
                service.submit(new Runnable() {

                    @Override
                    public String toString() {
                        return "asyncTaskTask";
                    }

                    @Override
                    public void run() {
                        try {
                            action.execute();
                            secureCallback(new Runnable() {

                                @Override
                                public String toString() {
                                    return "asyncTaskTask1";
                                }

                                @Override
                                public void run() {
                                    if (taskProgressInterface != null) {
                                        taskProgressInterface.showContent();
                                        taskProgressInterface.hideProgress();
                                    }
                                    action.afterExecute();
                                }
                            });
                        } catch (final Exception e) {
                            e.printStackTrace();
                            final AsyncException asyncException = e instanceof AsyncException
                                    ? (AsyncException) e :
                                    new AsyncException(AsyncException.ExceptionType.UNKNOWN_ERROR, e);

                            if (recoverCallback != null) {
                                secureCallback(new Runnable() {

                                    @Override
                                    public String toString() {
                                        return "asyncTaskTask2";
                                    }

                                    @Override
                                    public void run() {
                                        if (taskProgressInterface != null) {
                                            taskProgressInterface.hideProgress();
                                        }
                                        recoverCallback.onError(asyncException, new Runnable() {

                                                    @Override
                                                    public String toString() {
                                                        return "asyncTaskTask_error";
                                                    }

                                                    @Override
                                                    public void run() {
                                                        asyncTask(action, recoverCallback, taskProgressInterface);
                                                    }
                                                }, new Runnable() {

                                                    @Override
                                                    public String toString() {
                                                        return "asyncTaskTask_cancel";
                                                    }

                                                    @Override
                                                    public void run() {
                                                        barrier.sendCallback(new Runnable() {

                                                            @Override
                                                            public String toString() {
                                                                return "asyncTaskTask_cancel_inner";
                                                            }

                                                            @Override
                                                            public void run() {
                                                                action.onCanceled();
                                                            }
                                                        });
                                                    }
                                                }
                                        );
                                    }
                                });
                            } else {
                                secureCallback(new Runnable() {
                                    @Override
                                    public String toString() {
                                        return "asyncTaskTask_ex";
                                    }

                                    @Override
                                    public void run() {
                                        action.onException(asyncException);
                                    }
                                });
                            }
                        }
                    }
                });
            }
        });
    }

    protected String getStringSafe(int id) {
        return application.getString(id);
    }

    protected CharSequence getTextSafe(int id) {
        return application.getText(id);
    }

    public void secureCallback(Runnable runnable) {
        barrier.sendCallback(runnable);
    }

    public void postDelayerWeak(final Runnable runnable, int delta) {
        if (!barrier.isPaused()) {
            handler.postDelayed(new Runnable() {

                @Override
                public String toString() {
                    return "postDelayerWeak";
                }

                @Override
                public void run() {
                    if (!barrier.isPaused()) {
                        runnable.run();
                    }
                }
            }, delta);
        }
    }

    protected void fixEditText(EditText editText) {
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    //open keyboard
                    ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(v,
                            InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });

        //Set on click listener to clear focus
        editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View clickedView) {
                clickedView.clearFocus();
                clickedView.requestFocus();
            }
        });
    }

    protected void hideKeyboard(View editText) {
        if (editText == getActivity().getCurrentFocus()) {
            editText.clearFocus();
            ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(editText.getWindowToken(),
                    0);
        } else {
            editText.clearFocus();
        }
    }

    protected void showKeyboard(final EditText editText) {
        Activity activity = getActivity();
        if (activity != null) {
            editText.clearFocus();
            editText.requestFocus();
            ((InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(editText,
                    InputMethodManager.SHOW_IMPLICIT);
        }
    }

    protected boolean hasApplication(String packageName) {
        try {
            PackageManager pm = application.getPackageManager();
            pm.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    protected PickIntentItem[] createPickIntents(Intent intent) {
        PackageManager pm = application.getPackageManager();
        List<ResolveInfo> rList = pm.queryIntentActivities(intent, 0);

        ArrayList<PickIntentItem> res = new ArrayList<PickIntentItem>();
        for (ResolveInfo info : rList) {
            PickIntentItem item = new PickIntentItem(info.loadIcon(pm), info.loadLabel(pm).toString());
            Intent activityIntent = intent.cloneFilter();
            activityIntent.setClassName(info.activityInfo.packageName, info.activityInfo.name);
            item.setIntent(activityIntent);
            item.setTag(info);
            res.add(item);
        }

        return res.toArray(new PickIntentItem[res.size()]);
    }

    protected int getPx(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics);
    }

    protected int getSp(float sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, metrics);
    }

    protected void showView(View view) {
        showView(view, true);
    }

    protected void showView(View view, boolean isAnimating) {
        if (view == null) {
            return;
        }
        if (view.getVisibility() == View.VISIBLE) {
            return;
        }

        if (isAnimating) {
            AlphaAnimation alpha = new AlphaAnimation(0.0F, 1.0f);
            alpha.setDuration(250);
            alpha.setFillAfter(false);
            view.startAnimation(alpha);
        }
        view.setVisibility(View.VISIBLE);
    }

    protected void hideView(View view) {
        hideView(view, true);
    }

    protected void hideView(View view, boolean isAnimating) {
        if (view == null) {
            return;
        }
        if (view.getVisibility() != View.VISIBLE) {
            return;
        }
        if (isAnimating) {
            AlphaAnimation alpha = new AlphaAnimation(1.0F, 0.0f);
            alpha.setDuration(250);
            alpha.setFillAfter(false);
            view.startAnimation(alpha);
        }
        view.setVisibility(View.INVISIBLE);
    }

    protected void goneView(View view) {
        goneView(view, true);
    }

    protected void goneView(View view, boolean isAnimating) {
        if (view == null) {
            return;
        }
        if (view.getVisibility() != View.VISIBLE) {
            return;
        }
        if (isAnimating) {
            AlphaAnimation alpha = new AlphaAnimation(1.0F, 0.0f);
            alpha.setDuration(250);
            alpha.setFillAfter(false);
            view.startAnimation(alpha);
        }
        view.setVisibility(View.GONE);
    }

    public <T extends TLObject> T rpcRaw(TLMethod<T> method) throws AsyncException, RpcException {
        try {
            return application.getApi().doRpcCall(method);
        } catch (RpcException e) {
            throw e;
        } catch (TimeoutException e) {
            e.printStackTrace();
            throw new AsyncException(AsyncException.ExceptionType.CONNECTION_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            throw new AsyncException(AsyncException.ExceptionType.UNKNOWN_ERROR);
        }
    }

    public <T extends TLObject> T rpc(TLMethod<T> method) throws AsyncException {
        try {
            return application.getApi().doRpcCall(method);
        } catch (RpcException e) {
            e.printStackTrace();
            throw new AsyncException(e);
        } catch (TimeoutException e) {
            e.printStackTrace();
            throw new AsyncException(AsyncException.ExceptionType.CONNECTION_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            throw new AsyncException(AsyncException.ExceptionType.UNKNOWN_ERROR);
        }
    }

    @Override
    public void onEmojiUpdated(boolean completed) {

    }
}