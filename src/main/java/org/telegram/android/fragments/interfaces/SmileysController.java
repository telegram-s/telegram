package org.telegram.android.fragments.interfaces;

import android.widget.EditText;

/**
 * Author: Korshakov Stepan
 * Created: 30.07.13 9:48
 */
public interface SmileysController {
    public boolean areSmileysVisible();

    public void showSmileys(EditText dest);

    public void hideSmileys();
}
