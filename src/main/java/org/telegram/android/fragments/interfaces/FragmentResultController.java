package org.telegram.android.fragments.interfaces;

/**
 * Author: Korshakov Stepan
 * Created: 10.08.13 19:34
 */
public interface FragmentResultController {
    void setResult(int resultCode, Object data);

    int getResultCode();

    Object getResultData();
}
