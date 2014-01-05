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
 * Security preferences.
 * 
 * @author Hai Bison
 */
public class SecurityPrefs extends Prefs {

    /**
     * Checks if the library is using auto-save pattern mode.
     * 
     * @param context
     *            the context.
     * @return {@code true} or {@code false}. Default is {@code false}.
     */
    public static boolean isAutoSavePattern(Context context) {
        return p(context).getBoolean(
                context.getString(R.string.alp_pkey_sys_auto_save_pattern),
                context.getResources().getBoolean(
                        R.bool.alp_pkey_sys_auto_save_pattern_default));
    }// isAutoSavePattern()

    /**
     * Sets auto-save pattern mode.
     * 
     * @param context
     *            the context.
     * @param v
     *            the auto-save mode.
     */
    public static void setAutoSavePattern(Context context, boolean v) {
        p(context)
                .edit()
                .putBoolean(
                        context.getString(R.string.alp_pkey_sys_auto_save_pattern),
                        v).commit();
        if (!v)
            setPattern(context, null);
    }// setAutoSavePattern()

    /**
     * Gets the pattern.
     * 
     * @param context
     *            the context.
     * @return the pattern. Default is {@code null}.
     */
    public static char[] getPattern(Context context) {
        String pattern = p(context).getString(
                context.getString(R.string.alp_pkey_sys_pattern), null);
        return pattern == null ? null : pattern.toCharArray();
    }// getPattern()

    /**
     * Sets the pattern.
     * 
     * @param context
     *            the context.
     * @param pattern
     *            the pattern, can be {@code null} to reset it.
     */
    public static void setPattern(Context context, char[] pattern) {
        p(context)
                .edit()
                .putString(context.getString(R.string.alp_pkey_sys_pattern),
                        pattern != null ? new String(pattern) : null).commit();
    }// setPattern()

    /**
     * Gets encrypter class.
     * 
     * @param context
     *            the context.
     * @return the full name of encrypter class. Default is {@code null}.
     */
    public static char[] getEncrypterClass(Context context) {
        String clazz = p(context).getString(
                context.getString(R.string.alp_pkey_sys_encrypter_class), null);
        return clazz == null ? null : clazz.toCharArray();
    }// getEncrypterClass()

    /**
     * Sets encrypter class.
     * 
     * @param context
     *            the context.
     * @param clazz
     *            the encrypter class, can be {@code null} if you don't want to
     *            use it.
     */
    public static void setEncrypterClass(Context context, Class<?> clazz) {
        setEncrypterClass(context, clazz != null ? clazz.getName()
                .toCharArray() : null);
    }// setEncrypterClass()

    /**
     * Sets encrypter class.
     * 
     * @param context
     *            the context.
     * @param clazz
     *            the full name of encrypter class, can be {@code null} if you
     *            don't want to use it.
     */
    public static void setEncrypterClass(Context context, char[] clazz) {
        p(context)
                .edit()
                .putString(
                        context.getString(R.string.alp_pkey_sys_encrypter_class),
                        clazz != null ? new String(clazz) : null).commit();
    }// setEncrypterClass()
}
