package org.telegram.android.fragments.sec;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import org.telegram.android.R;
import org.telegram.android.StartActivity;
import org.telegram.android.StelsActivity;
import org.telegram.android.StelsFragment;

/**
 * Created by ex3ndr on 05.01.14.
 */
public class PinFragment extends StelsFragment {
    public static final String ACTION_CREATE = "sec.pin.create";
    public static final String ACTION_VALIDATE = "sec.pin.check";
    public static final String ACTION_UNLOCK = "sec.pin.unlock";

    public static PinFragment buildCreate() {
        return new PinFragment(ACTION_CREATE);
    }

    public static PinFragment buildValidate() {
        return new PinFragment(ACTION_VALIDATE);
    }

    public static PinFragment buildUnlock() {
        return new PinFragment(ACTION_UNLOCK);
    }

    private String action;

    private PinFragment(String action) {
        this.action = action;
    }

    private PinFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View res = inflater.inflate(R.layout.sec_pin, container, false);
        res.findViewById(R.id.unlockApp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((StartActivity) getActivity()).unlock();
            }
        });
        return res;
    }
}
