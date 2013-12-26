package org.telegram.android.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;

/**
 * Created by ex3ndr on 27.12.13.
 */
public class FixedEditText extends EditText {
    public FixedEditText(Context context) {
        super(context);
    }

    public FixedEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FixedEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection conn = super.onCreateInputConnection(outAttrs);
        outAttrs.imeOptions &= ~EditorInfo.IME_FLAG_NO_ENTER_ACTION;
        return conn;
    }
}
