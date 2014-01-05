/*
 *   Copyright 2012 Hai Bison
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package group.pals.android.lib.ui.lockpattern;

import group.pals.android.lib.ui.lockpattern.prefs.DisplayPrefs;
import group.pals.android.lib.ui.lockpattern.prefs.SecurityPrefs;
import group.pals.android.lib.ui.lockpattern.util.IEncrypter;
import group.pals.android.lib.ui.lockpattern.util.InvalidEncrypterException;
import group.pals.android.lib.ui.lockpattern.util.UI;
import group.pals.android.lib.ui.lockpattern.widget.LockPatternUtils;
import group.pals.android.lib.ui.lockpattern.widget.LockPatternView;
import group.pals.android.lib.ui.lockpattern.widget.LockPatternView.Cell;
import group.pals.android.lib.ui.lockpattern.widget.LockPatternView.DisplayMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.TextView;

/**
 * Main activity for this library.
 * <p>
 * You can deliver result to {@link PendingIntent}'s and/ or
 * {@link ResultReceiver} too. See {@link #EXTRA_PENDING_INTENT_OK},
 * {@link #EXTRA_PENDING_INTENT_CANCELLED} and {@link #EXTRA_RESULT_RECEIVER}
 * for more details.
 * </p>
 * 
 * <h1>NOTES</h1>
 * <ul>
 * <li>
 * You must use one of built-in actions when calling this activity. They start
 * with {@code ACTION_*}. Otherwise the library might behave strangely (we don't
 * cover those cases).</li>
 * <li>You must use one of the themes that this library provides. They start
 * with {@code R.style.Alp_Theme_*}. The reason is the themes contain resources
 * that the library needs.</li>
 * <li>With {@link #ACTION_COMPARE_PATTERN}, there are <b><i>4 possible result
 * codes</i></b>: {@link Activity#RESULT_OK}, {@link Activity#RESULT_CANCELED},
 * {@link #RESULT_FAILED} and {@link #RESULT_FORGOT_PATTERN}.</li>
 * <li>With {@link #ACTION_VERIFY_CAPTCHA}, there are <b><i>3 possible result
 * codes</i></b>: {@link Activity#RESULT_OK}, {@link Activity#RESULT_CANCELED},
 * and {@link #RESULT_FAILED}.</li>
 * </ul>
 * 
 * @author Hai Bison
 * @since v1.0
 */
public class LockPatternActivity extends Activity {

    private static final String CLASSNAME = LockPatternActivity.class.getName();

    /**
     * Use this action to create new pattern. You can provide an
     * {@link IEncrypter} with
     * {@link SecurityPrefs#setEncrypterClass(android.content.Context, Class)}
     * to improve security.
     * <p>
     * If the user created a pattern, {@link Activity#RESULT_OK} returns with
     * the pattern ({@link #EXTRA_PATTERN}). Otherwise
     * {@link Activity#RESULT_CANCELED} returns.
     * </p>
     * 
     * @see #EXTRA_PENDING_INTENT_OK
     * @see #EXTRA_PENDING_INTENT_CANCELLED
     * @since v2.4 beta
     */
    public static final String ACTION_CREATE_PATTERN = CLASSNAME
            + ".create_pattern";

    /**
     * Use this action to compare pattern. You provide the pattern to be
     * compared with {@link #EXTRA_PATTERN}.
     * <p>
     * If you enabled feature auto-save pattern before (with
     * {@link SecurityPrefs#setAutoSavePattern(android.content.Context, boolean)}
     * ), then you don't need {@link #EXTRA_PATTERN} at this time. But if you
     * use this extra, its priority is higher than the one stored in shared
     * preferences.
     * </p>
     * <p>
     * You can use {@link #EXTRA_INTENT_ACTIVITY_FORGOT_PATTERN} to help your
     * users in case they forgot the patterns.
     * </p>
     * <p>
     * If the user passes, {@link Activity#RESULT_OK} returns. If not,
     * {@link #RESULT_FAILED} returns.
     * </p>
     * <p>
     * If the user cancels the task, {@link Activity#RESULT_CANCELED} returns.
     * </p>
     * <p>
     * In any case, there will be key {@link #EXTRA_RETRY_COUNT} available in
     * the intent result.
     * </p>
     * 
     * @see #EXTRA_PATTERN
     * @see #EXTRA_PENDING_INTENT_OK
     * @see #EXTRA_PENDING_INTENT_CANCELLED
     * @see #RESULT_FAILED
     * @see #EXTRA_RETRY_COUNT
     * @since v2.4 beta
     */
    public static final String ACTION_COMPARE_PATTERN = CLASSNAME
            + ".compare_pattern";

    /**
     * Use this action to let the activity generate a random pattern and ask the
     * user to re-draw it to verify.
     * <p>
     * The default length of the auto-generated pattern is {@code 4}. You can
     * change it with
     * {@link DisplayPrefs#setCaptchaWiredDots(android.content.Context, int)}.
     * </p>
     * 
     * @since v2.7 beta
     */
    public static final String ACTION_VERIFY_CAPTCHA = CLASSNAME
            + ".verify_captcha";

    /**
     * If you use {@link #ACTION_COMPARE_PATTERN} and the user fails to "login"
     * after a number of tries, this activity will finish with this result code.
     * 
     * @see #ACTION_COMPARE_PATTERN
     * @see #EXTRA_RETRY_COUNT
     */
    public static final int RESULT_FAILED = RESULT_FIRST_USER + 1;

    /**
     * If you use {@link #ACTION_COMPARE_PATTERN} and the user forgot his/ her
     * pattern and decided to ask for your help with recovering the pattern (
     * {@link #EXTRA_INTENT_ACTIVITY_FORGOT_PATTERN}), this activity will finish
     * with this result code.
     * 
     * @see #ACTION_COMPARE_PATTERN
     * @see #EXTRA_RETRY_COUNT
     * @see #EXTRA_INTENT_ACTIVITY_FORGOT_PATTERN
     * @since v2.8 beta
     */
    public static final int RESULT_FORGOT_PATTERN = RESULT_FIRST_USER + 2;

    /**
     * If you use {@link #ACTION_COMPARE_PATTERN}, and the user fails to "login"
     * after a number of tries, this key holds that number.
     * 
     * @see #ACTION_COMPARE_PATTERN
     * @see DisplayPrefs#setMaxRetry(android.content.Context, int)
     */
    public static final String EXTRA_RETRY_COUNT = CLASSNAME + ".retry_count";

    /**
     * Sets value of this key to a theme in {@code R.style.Alp_Theme_*}. Default
     * is the one you set in your {@code AndroidManifest.xml}. Note that theme
     * {@link R.style#Alp_Theme_Light_DarkActionBar} is available in API 4+, but
     * it only works in API 14+.
     * 
     * @since v1.5.3 beta
     */
    public static final String EXTRA_THEME = CLASSNAME + ".theme";

    /**
     * Key to hold the pattern. It must be a {@code char[]} array.
     * <p/>
     * <ul>
     * <li>If you use encrypter, it should be an encrypted array.</li>
     * <li>If you don't use encrypter, it should be the SHA-1 value of the
     * actual pattern. You can generate the value by
     * {@link LockPatternUtils#patternToSha1(List)}.</li>
     * </ul>
     * 
     * @since v2 beta
     */
    public static final String EXTRA_PATTERN = CLASSNAME + ".pattern";

    /**
     * You can provide an {@link ResultReceiver} with this key. The activity
     * will notify your receiver the same result code and intent data as you
     * will receive them in {@link #onActivityResult(int, int, Intent)}.
     * 
     * @since v2.4 beta
     */
    public static final String EXTRA_RESULT_RECEIVER = CLASSNAME
            + ".result_receiver";

    /**
     * Put a {@link PendingIntent} into this key. It will be sent before
     * {@link Activity#RESULT_OK} will be returning. If you were calling this
     * activity with {@link #ACTION_CREATE_PATTERN}, key {@link #EXTRA_PATTERN}
     * will be attached to the original intent which the pending intent holds.
     * 
     * <h1>Notes</h1>
     * <ul>
     * <li>If you're going to use an activity, you don't need
     * {@link Intent#FLAG_ACTIVITY_NEW_TASK} for the intent, since the library
     * will call it inside {@link LockPatternActivity} .</li>
     * </ul>
     */
    public static final String EXTRA_PENDING_INTENT_OK = CLASSNAME
            + ".pending_intent_ok";

    /**
     * Put a {@link PendingIntent} into this key. It will be sent before
     * {@link Activity#RESULT_CANCELED} will be returning.
     * 
     * <h1>Notes</h1>
     * <ul>
     * <li>If you're going to use an activity, you don't need
     * {@link Intent#FLAG_ACTIVITY_NEW_TASK} for the intent, since the library
     * will call it inside {@link LockPatternActivity} .</li>
     * </ul>
     */
    public static final String EXTRA_PENDING_INTENT_CANCELLED = CLASSNAME
            + ".pending_intent_cancelled";

    /**
     * You put a {@link Intent} of <i>{@link Activity}</i> into this extra. The
     * library will show a button <i>"Forgot pattern?"</i> and call your intent
     * later when the user taps it.
     * 
     * <h1>Notes</h1>
     * <ul>
     * <li>You don't need {@link Intent#FLAG_ACTIVITY_NEW_TASK} for the intent,
     * since the library will call it inside {@link LockPatternActivity} .</li>
     * <li>{@link LockPatternActivity} will finish with
     * {@link #RESULT_FORGOT_PATTERN} <i><b>after</b> making a call</i> to start
     * your activity.</li>
     * <li>It is your responsibility to make sure the Intent is good. The
     * library doesn't cover any errors when calling your intent.</li>
     * </ul>
     * 
     * @see #ACTION_COMPARE_PATTERN
     * @since v2.8 beta
     * @author Thanks to Yan Cheng Cheok for his idea.
     */
    public static final String EXTRA_INTENT_ACTIVITY_FORGOT_PATTERN = CLASSNAME
            + ".intent_activity_forgot_pattern";

    /**
     * Helper enum for button OK commands. (Because we use only one "OK" button
     * for different commands).
     * 
     * @author Hai Bison
     */
    private static enum ButtonOkCommand {
        CONTINUE, FORGOT_PATTERN, DONE
    }// ButtonOkCommand

    /**
     * Delay time to reload the lock pattern view after a wrong pattern.
     */
    private static final long DELAY_TIME_TO_RELOAD_LOCK_PATTERN_VIEW = DateUtils.SECOND_IN_MILLIS;

    /*
     * FIELDS
     */
    private int mMaxRetry;
    private boolean mAutoSave;
    private IEncrypter mEncrypter;
    private int mMinWiredDots;
    private ButtonOkCommand mBtnOkCmd;
    private Intent mIntentResult;
    private int mRetryCount = 0;

    /*
     * CONTROLS
     */
    private TextView mTextInfo;
    private LockPatternView mLockPatternView;
    private View mFooter;
    private Button mBtnCancel;
    private Button mBtnConfirm;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (BuildConfig.DEBUG)
            Log.d(CLASSNAME, "ClassName = " + CLASSNAME);

        /*
         * EXTRA_THEME
         */

        if (getIntent().hasExtra(EXTRA_THEME))
            setTheme(getIntent().getIntExtra(EXTRA_THEME,
                    R.style.Alp_Theme_Dark));

        super.onCreate(savedInstanceState);

        mMinWiredDots = DisplayPrefs.getMinWiredDots(this);
        mMaxRetry = DisplayPrefs.getMaxRetry(this);
        mAutoSave = SecurityPrefs.isAutoSavePattern(this);

        /*
         * Encrypter.
         */
        char[] encrypterClass = SecurityPrefs.getEncrypterClass(this);
        if (encrypterClass != null) {
            try {
                mEncrypter = (IEncrypter) Class.forName(
                        new String(encrypterClass), false, getClassLoader())
                        .newInstance();
            } catch (Throwable t) {
                throw new InvalidEncrypterException();
            }
        }

        mIntentResult = new Intent();
        setResult(RESULT_CANCELED, mIntentResult);

        initContentView();
    }// onCreate()

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(CLASSNAME, "onConfigurationChanged()");
        super.onConfigurationChanged(newConfig);
        initContentView();
    }// onConfigurationChanged()

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && ACTION_COMPARE_PATTERN.equals(getIntent().getAction())) {
            /*
             * Use this hook instead of onBackPressed(), because onBackPressed()
             * is not available in API 4.
             */
            finishWithNegativeResult(RESULT_CANCELED);
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }// onKeyDown()

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (BuildConfig.DEBUG)
            Log.d(CLASSNAME, "onDestroy()");
    }// onDestroy()

    /**
     * Initializes UI...
     */
    private void initContentView() {
        /*
         * Save all controls' state to restore later.
         */
        CharSequence infoText = mTextInfo != null ? mTextInfo.getText() : null;
        Boolean btnOkEnabled = mBtnConfirm != null ? mBtnConfirm.isEnabled()
                : null;
        LockPatternView.DisplayMode lastDisplayMode = mLockPatternView != null ? mLockPatternView
                .getDisplayMode() : null;
        List<Cell> lastPattern = mLockPatternView != null ? mLockPatternView
                .getPattern() : null;

        setContentView(R.layout.alp_lock_pattern_activity);
        UI.adjustDialogSizeForLargeScreens(getWindow());

        mTextInfo = (TextView) findViewById(R.id.alp_textview_info);
        mLockPatternView = (LockPatternView) findViewById(R.id.alp_view_lock_pattern);

        mFooter = findViewById(R.id.alp_viewgroup_footer);
        mBtnCancel = (Button) findViewById(R.id.alp_button_cancel);
        mBtnConfirm = (Button) findViewById(R.id.alp_button_confirm);

        /*
         * LOCK PATTERN VIEW
         */

        switch (getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) {
        case Configuration.SCREENLAYOUT_SIZE_LARGE:
        case Configuration.SCREENLAYOUT_SIZE_XLARGE: {
            final int size = getResources().getDimensionPixelSize(
                    R.dimen.alp_lockpatternview_size);
            LayoutParams lp = mLockPatternView.getLayoutParams();
            lp.width = size;
            lp.height = size;
            mLockPatternView.setLayoutParams(lp);

            break;
        }// LARGE / XLARGE
        }

        /*
         * Haptic feedback.
         */
        boolean hapticFeedbackEnabled = false;
        try {
            hapticFeedbackEnabled = Settings.System.getInt(
                    getContentResolver(),
                    Settings.System.HAPTIC_FEEDBACK_ENABLED, 0) != 0;
        } catch (Throwable t) {
            /*
             * Ignore it.
             */
        }
        mLockPatternView.setTactileFeedbackEnabled(hapticFeedbackEnabled);

        mLockPatternView.setInStealthMode(DisplayPrefs.isStealthMode(this)
                && !ACTION_VERIFY_CAPTCHA.equals(getIntent().getAction()));
        mLockPatternView.setOnPatternListener(mLockPatternViewListener);
        if (lastPattern != null && lastDisplayMode != null
                && !ACTION_VERIFY_CAPTCHA.equals(getIntent().getAction()))
            mLockPatternView.setPattern(lastDisplayMode, lastPattern);

        /*
         * COMMAND BUTTONS
         */

        if (ACTION_CREATE_PATTERN.equals(getIntent().getAction())) {
            mBtnCancel.setOnClickListener(mBtnCancelOnClickListener);
            mBtnConfirm.setOnClickListener(mBtnConfirmOnClickListener);

            mBtnCancel.setVisibility(View.VISIBLE);
            mFooter.setVisibility(View.VISIBLE);

            if (infoText != null)
                mTextInfo.setText(infoText);
            else
                mTextInfo.setText(R.string.alp_msg_draw_an_unlock_pattern);

            /*
             * BUTTON OK
             */
            if (mBtnOkCmd == null)
                mBtnOkCmd = ButtonOkCommand.CONTINUE;
            switch (mBtnOkCmd) {
            case CONTINUE:
                mBtnConfirm.setText(R.string.alp_cmd_continue);
                break;
            case DONE:
                mBtnConfirm.setText(R.string.alp_cmd_confirm);
                break;
            default:
                /*
                 * Do nothing.
                 */
                break;
            }
            if (btnOkEnabled != null)
                mBtnConfirm.setEnabled(btnOkEnabled);
        }// ACTION_CREATE_PATTERN
        else if (ACTION_COMPARE_PATTERN.equals(getIntent().getAction())) {
            if (TextUtils.isEmpty(infoText))
                mTextInfo.setText(R.string.alp_msg_draw_pattern_to_unlock);
            else
                mTextInfo.setText(infoText);
            if (getIntent().hasExtra(EXTRA_INTENT_ACTIVITY_FORGOT_PATTERN)) {
                mBtnConfirm.setOnClickListener(mBtnConfirmOnClickListener);
                mBtnConfirm.setText(R.string.alp_cmd_forgot_pattern);
                mBtnConfirm.setEnabled(true);
                mFooter.setVisibility(View.VISIBLE);
            }
        }// ACTION_COMPARE_PATTERN
        else if (ACTION_VERIFY_CAPTCHA.equals(getIntent().getAction())) {
            mTextInfo.setText(R.string.alp_msg_redraw_pattern_to_confirm);

            /*
             * NOTE: EXTRA_PATTERN should hold a char[] array. In this case we
             * use it as a temporary variable to hold a list of Cell.
             */

            final ArrayList<Cell> pattern;
            if (getIntent().hasExtra(EXTRA_PATTERN))
                pattern = getIntent()
                        .getParcelableArrayListExtra(EXTRA_PATTERN);
            else
                getIntent().putParcelableArrayListExtra(
                        EXTRA_PATTERN,
                        pattern = LockPatternUtils
                                .genCaptchaPattern(DisplayPrefs
                                        .getCaptchaWiredDots(this)));

            mLockPatternView.setPattern(DisplayMode.Animate, pattern);
        }// ACTION_VERIFY_CAPTCHA
    }// initContentView()

    /**
     * Compares {@code pattern} to the given pattern (
     * {@link #ACTION_COMPARE_PATTERN}) or to the generated "CAPTCHA" pattern (
     * {@link #ACTION_VERIFY_CAPTCHA}). Then finishes the activity if they
     * match.
     * 
     * @param pattern
     *            the pattern to be compared.
     */
    private void doComparePattern(List<Cell> pattern) {
        if (pattern == null)
            return;

        boolean okey = false;

        if (ACTION_COMPARE_PATTERN.equals(getIntent().getAction())) {
            char[] currentPattern = getIntent()
                    .getCharArrayExtra(EXTRA_PATTERN);
            if (currentPattern == null)
                currentPattern = SecurityPrefs.getPattern(this);
            if (currentPattern != null) {
                if (mEncrypter != null)
                    okey = pattern.equals(mEncrypter.decrypt(this,
                            currentPattern));
                else
                    okey = Arrays.equals(currentPattern, LockPatternUtils
                            .patternToSha1(pattern).toCharArray());
            } else
                okey = false;
        }// ACTION_COMPARE_PATTERN
        else if (ACTION_VERIFY_CAPTCHA.equals(getIntent().getAction())) {
            okey = pattern.equals(getIntent().getParcelableArrayListExtra(
                    EXTRA_PATTERN));
        }// ACTION_VERIFY_CAPTCHA

        if (okey)
            finishWithResultOk(null);
        else {
            mRetryCount++;
            mIntentResult.putExtra(EXTRA_RETRY_COUNT, mRetryCount);

            if (mRetryCount >= mMaxRetry)
                finishWithNegativeResult(RESULT_FAILED);
            else {
                mLockPatternView.setDisplayMode(DisplayMode.Wrong);
                mTextInfo.setText(R.string.alp_msg_try_again);
                mLockPatternView.postDelayed(mLockPatternViewReloader,
                        DELAY_TIME_TO_RELOAD_LOCK_PATTERN_VIEW);
            }
        }
    }// doComparePattern()

    /**
     * Checks and creates the pattern.
     * 
     * @param pattern
     *            the current pattern of lock pattern view.
     */
    private void doCheckAndCreatePattern(List<Cell> pattern) {
        if (pattern.size() < mMinWiredDots) {
            mLockPatternView.setDisplayMode(DisplayMode.Wrong);
            mTextInfo.setText(getResources().getQuantityString(
                    R.plurals.alp_pmsg_connect_x_dots, mMinWiredDots,
                    mMinWiredDots));
            mLockPatternView.postDelayed(mLockPatternViewReloader,
                    DELAY_TIME_TO_RELOAD_LOCK_PATTERN_VIEW);
            return;
        }

        if (getIntent().hasExtra(EXTRA_PATTERN)) {
            final boolean okey;
            if (mEncrypter != null)
                okey = pattern.equals(mEncrypter.decrypt(this, getIntent()
                        .getCharArrayExtra(EXTRA_PATTERN)));
            else
                okey = Arrays.equals(
                        getIntent().getCharArrayExtra(EXTRA_PATTERN),
                        LockPatternUtils.patternToSha1(pattern).toCharArray());
            if (okey) {
                mTextInfo.setText(R.string.alp_msg_your_new_unlock_pattern);
                mBtnConfirm.setEnabled(true);
            } else {
                mTextInfo.setText(R.string.alp_msg_redraw_pattern_to_confirm);
                mBtnConfirm.setEnabled(false);
                mLockPatternView.setDisplayMode(DisplayMode.Wrong);
                mLockPatternView.postDelayed(mLockPatternViewReloader,
                        DELAY_TIME_TO_RELOAD_LOCK_PATTERN_VIEW);
            }
        } else {
            getIntent().putExtra(
                    EXTRA_PATTERN,
                    mEncrypter != null ? mEncrypter.encrypt(this, pattern)
                            : LockPatternUtils.patternToSha1(pattern)
                                    .toCharArray());
            mTextInfo.setText(R.string.alp_msg_pattern_recorded);
            mBtnConfirm.setEnabled(true);
        }
    }// doCheckAndCreatePattern()

    /**
     * Finishes activity with {@link Activity#RESULT_OK}.
     * 
     * @param pattern
     *            the pattern, if this is in mode creating pattern. In any
     *            cases, it can be set to {@code null}.
     */
    private void finishWithResultOk(char[] pattern) {
        if (ACTION_CREATE_PATTERN.equals(getIntent().getAction()))
            mIntentResult.putExtra(EXTRA_PATTERN, pattern);
        else {
            /*
             * If the user was "logging in", minimum try count can not be zero.
             */
            mIntentResult.putExtra(EXTRA_RETRY_COUNT, mRetryCount + 1);
        }

        setResult(RESULT_OK, mIntentResult);

        /*
         * ResultReceiver
         */
        ResultReceiver receiver = getIntent().getParcelableExtra(
                EXTRA_RESULT_RECEIVER);
        if (receiver != null) {
            Bundle bundle = new Bundle();
            if (ACTION_CREATE_PATTERN.equals(getIntent().getAction()))
                bundle.putCharArray(EXTRA_PATTERN, pattern);
            else {
                /*
                 * If the user was "logging in", minimum try count can not be
                 * zero.
                 */
                bundle.putInt(EXTRA_RETRY_COUNT, mRetryCount + 1);
            }
            receiver.send(RESULT_OK, bundle);
        }

        /*
         * PendingIntent
         */
        PendingIntent pi = getIntent().getParcelableExtra(
                EXTRA_PENDING_INTENT_OK);
        if (pi != null) {
            try {
                pi.send(this, RESULT_OK, mIntentResult);
            } catch (Throwable t) {
                if (BuildConfig.DEBUG) {
                    Log.e(CLASSNAME, "Error sending PendingIntent: " + pi);
                    Log.e(CLASSNAME, ">>> " + t);
                    t.printStackTrace();
                }
            }
        }

        finish();
    }// finishWithResultOk()

    /**
     * Finishes the activity with negative result (
     * {@link Activity#RESULT_CANCELED}, {@link #RESULT_FAILED} or
     * {@link #RESULT_FORGOT_PATTERN}).
     */
    private void finishWithNegativeResult(int resultCode) {
        if (ACTION_COMPARE_PATTERN.equals(getIntent().getAction()))
            mIntentResult.putExtra(EXTRA_RETRY_COUNT, mRetryCount);

        setResult(resultCode, mIntentResult);

        /*
         * ResultReceiver
         */
        ResultReceiver receiver = getIntent().getParcelableExtra(
                EXTRA_RESULT_RECEIVER);
        if (receiver != null) {
            Bundle resultBundle = null;
            if (ACTION_COMPARE_PATTERN.equals(getIntent().getAction())) {
                resultBundle = new Bundle();
                resultBundle.putInt(EXTRA_RETRY_COUNT, mRetryCount);
            }
            receiver.send(resultCode, resultBundle);
        }

        /*
         * PendingIntent
         */
        PendingIntent pi = getIntent().getParcelableExtra(
                EXTRA_PENDING_INTENT_CANCELLED);
        if (pi != null) {
            try {
                pi.send(this, resultCode, mIntentResult);
            } catch (Throwable t) {
                if (BuildConfig.DEBUG) {
                    Log.e(CLASSNAME, "Error sending PendingIntent: " + pi);
                    Log.e(CLASSNAME, ">>> " + t);
                    t.printStackTrace();
                }
            }
        }

        finish();
    }// finishWithNegativeResult()

    /*
     * LISTENERS
     */

    private final LockPatternView.OnPatternListener mLockPatternViewListener = new LockPatternView.OnPatternListener() {

        @Override
        public void onPatternStart() {
            mLockPatternView.removeCallbacks(mLockPatternViewReloader);
            mLockPatternView.setDisplayMode(DisplayMode.Correct);

            if (ACTION_CREATE_PATTERN.equals(getIntent().getAction())) {
                mTextInfo.setText(R.string.alp_msg_release_finger_when_done);
                mBtnConfirm.setEnabled(false);
                if (mBtnOkCmd == ButtonOkCommand.CONTINUE)
                    getIntent().removeExtra(EXTRA_PATTERN);
            }// ACTION_CREATE_PATTERN
            else if (ACTION_COMPARE_PATTERN.equals(getIntent().getAction())) {
                mTextInfo.setText(R.string.alp_msg_draw_pattern_to_unlock);
            }// ACTION_COMPARE_PATTERN
            else if (ACTION_VERIFY_CAPTCHA.equals(getIntent().getAction())) {
                mTextInfo.setText(R.string.alp_msg_redraw_pattern_to_confirm);
            }// ACTION_VERIFY_CAPTCHA
        }// onPatternStart()

        @Override
        public void onPatternDetected(List<Cell> pattern) {
            if (ACTION_CREATE_PATTERN.equals(getIntent().getAction())) {
                doCheckAndCreatePattern(pattern);
            }// ACTION_CREATE_PATTERN
            else if (ACTION_COMPARE_PATTERN.equals(getIntent().getAction())) {
                doComparePattern(pattern);
            }// ACTION_COMPARE_PATTERN
            else if (ACTION_VERIFY_CAPTCHA.equals(getIntent().getAction())) {
                if (!DisplayMode.Animate.equals(mLockPatternView
                        .getDisplayMode()))
                    doComparePattern(pattern);
            }// ACTION_VERIFY_CAPTCHA
        }// onPatternDetected()

        @Override
        public void onPatternCleared() {
            mLockPatternView.removeCallbacks(mLockPatternViewReloader);

            if (ACTION_CREATE_PATTERN.equals(getIntent().getAction())) {
                mLockPatternView.setDisplayMode(DisplayMode.Correct);
                mBtnConfirm.setEnabled(false);
                if (mBtnOkCmd == ButtonOkCommand.CONTINUE) {
                    getIntent().removeExtra(EXTRA_PATTERN);
                    mTextInfo.setText(R.string.alp_msg_draw_an_unlock_pattern);
                } else
                    mTextInfo
                            .setText(R.string.alp_msg_redraw_pattern_to_confirm);
            }// ACTION_CREATE_PATTERN
            else if (ACTION_COMPARE_PATTERN.equals(getIntent().getAction())) {
                mLockPatternView.setDisplayMode(DisplayMode.Correct);
                mTextInfo.setText(R.string.alp_msg_draw_pattern_to_unlock);
            }// ACTION_COMPARE_PATTERN
            else if (ACTION_VERIFY_CAPTCHA.equals(getIntent().getAction())) {
                mTextInfo.setText(R.string.alp_msg_redraw_pattern_to_confirm);
                List<Cell> pattern = getIntent().getParcelableArrayListExtra(
                        EXTRA_PATTERN);
                mLockPatternView.setPattern(DisplayMode.Animate, pattern);
            }// ACTION_VERIFY_CAPTCHA
        }// onPatternCleared()

        @Override
        public void onPatternCellAdded(List<Cell> pattern) {
            // TODO Auto-generated method stub
        }// onPatternCellAdded()
    };// mLockPatternViewListener

    private final View.OnClickListener mBtnCancelOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            finishWithNegativeResult(RESULT_CANCELED);
        }// onClick()
    };// mBtnCancelOnClickListener

    private final View.OnClickListener mBtnConfirmOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (ACTION_CREATE_PATTERN.equals(getIntent().getAction())) {
                if (mBtnOkCmd == ButtonOkCommand.CONTINUE) {
                    mBtnOkCmd = ButtonOkCommand.DONE;
                    mLockPatternView.clearPattern();
                    mTextInfo
                            .setText(R.string.alp_msg_redraw_pattern_to_confirm);
                    mBtnConfirm.setText(R.string.alp_cmd_confirm);
                    mBtnConfirm.setEnabled(false);
                } else {
                    final char[] pattern = getIntent().getCharArrayExtra(
                            EXTRA_PATTERN);
                    if (mAutoSave)
                        SecurityPrefs.setPattern(LockPatternActivity.this,
                                pattern);
                    finishWithResultOk(pattern);
                }
            }// ACTION_CREATE_PATTERN
            else if (ACTION_COMPARE_PATTERN.equals(getIntent().getAction())) {
                /*
                 * We don't need to verify the extra. First, this button is only
                 * visible if there is this extra in the intent. Second, it is
                 * the responsibility of the caller to make sure the extra is an
                 * Intent of Activity.
                 */
                startActivity((Intent) getIntent().getParcelableExtra(
                        EXTRA_INTENT_ACTIVITY_FORGOT_PATTERN));
                finishWithNegativeResult(RESULT_FORGOT_PATTERN);
            }// ACTION_COMPARE_PATTERN
        }// onClick()
    };// mBtnConfirmOnClickListener

    /**
     * This reloads the {@link #mLockPatternView} after a wrong pattern.
     */
    private final Runnable mLockPatternViewReloader = new Runnable() {

        @Override
        public void run() {
            mLockPatternView.clearPattern();
            mLockPatternViewListener.onPatternCleared();
        }// run()
    };// mLockPatternViewReloader

}
