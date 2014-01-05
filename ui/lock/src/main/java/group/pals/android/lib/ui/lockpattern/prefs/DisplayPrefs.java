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

package group.pals.android.lib.ui.lockpattern.prefs;

import group.pals.android.lib.ui.lockpattern.R;
import android.content.Context;

/**
 * Display Preferences.
 * 
 * @author Hai Bison
 */
public class DisplayPrefs extends Prefs {

    /**
     * Checks if the library is using stealth mode or not.
     * 
     * @param context
     *            the context.
     * @return {@code true} or {@code false}. Default is {@code false}.
     */
    public static boolean isStealthMode(Context context) {
        return p(context).getBoolean(
                context.getString(R.string.alp_pkey_display_stealth_mode),
                context.getResources().getBoolean(
                        R.bool.alp_pkey_display_stealth_mode_default));
    }// isStealthMode()

    /**
     * Sets stealth mode.
     * 
     * @param context
     *            the context.
     * @param v
     *            the value.
     */
    public static void setStealthMode(Context context, boolean v) {
        p(context)
                .edit()
                .putBoolean(
                        context.getString(R.string.alp_pkey_display_stealth_mode),
                        v).commit();
    }// setStealthMode()

    /**
     * Gets minimum wired dots allowed for a pattern.
     * 
     * @param context
     *            the context.
     * @return the minimum wired dots allowed for a pattern. Default is
     *         {@code 4}.
     */
    public static int getMinWiredDots(Context context) {
        return p(context).getInt(
                context.getString(R.string.alp_pkey_display_min_wired_dots),
                context.getResources().getInteger(
                        R.integer.alp_pkey_display_min_wired_dots_default));
    }// getMinWiredDots()

    /**
     * Sets minimum wired dots allowed for a pattern.
     * 
     * @param context
     *            the context.
     * @param v
     *            the minimum wired dots allowed for a pattern.
     */
    public static void setMinWiredDots(Context context, int v) {
        if (v <= 0 || v > 9)
            v = context.getResources().getInteger(
                    R.integer.alp_pkey_display_min_wired_dots_default);
        p(context)
                .edit()
                .putInt(context
                        .getString(R.string.alp_pkey_display_min_wired_dots),
                        v).commit();
    }// setMinWiredDots()

    /**
     * Gets max retry allowed in mode comparing pattern.
     * 
     * @param context
     *            the context.
     * @return the max retry allowed in mode comparing pattern. Default is
     *         {@code 5}.
     */
    public static int getMaxRetry(Context context) {
        return p(context).getInt(
                context.getString(R.string.alp_pkey_display_max_retry),
                context.getResources().getInteger(
                        R.integer.alp_pkey_display_max_retry_default));
    }// getMaxRetry()

    /**
     * Sets max retry allowed in mode comparing pattern.
     * 
     * @param context
     *            the context.
     * @param v
     *            the max retry allowed in mode comparing pattern.
     */
    public static void setMaxRetry(Context context, int v) {
        if (v <= 0)
            v = context.getResources().getInteger(
                    R.integer.alp_pkey_display_max_retry_default);
        p(context)
                .edit()
                .putInt(context.getString(R.string.alp_pkey_display_max_retry),
                        v).commit();
    }// setMaxRetry()

    /**
     * Gets wired dots for a "CAPTCHA" pattern.
     * 
     * @param context
     *            the context.
     * @return the wired dots for a "CAPTCHA" pattern. Default is {@code 4}.
     */
    public static int getCaptchaWiredDots(Context context) {
        return p(context)
                .getInt(context
                        .getString(R.string.alp_pkey_display_captcha_wired_dots),
                        context.getResources()
                                .getInteger(
                                        R.integer.alp_pkey_display_captcha_wired_dots_default));
    }// getCaptchaWiredDots()

    /**
     * Sets wired dots for a "CAPTCHA" pattern.
     * 
     * @param context
     *            the context.
     * @param v
     *            the wired dots for a "CAPTCHA" pattern.
     */
    public static void setCaptchaWiredDots(Context context, int v) {
        if (v <= 0 || v > 9)
            v = context.getResources().getInteger(
                    R.integer.alp_pkey_display_captcha_wired_dots_default);
        p(context)
                .edit()
                .putInt(context
                        .getString(R.string.alp_pkey_display_captcha_wired_dots),
                        v).commit();
    }// setCaptchaWiredDots()

}
