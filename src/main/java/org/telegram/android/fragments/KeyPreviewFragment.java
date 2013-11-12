package org.telegram.android.fragments;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import org.telegram.android.R;
import org.telegram.android.StelsFragment;
import org.telegram.android.core.model.User;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 16.10.13
 * Time: 17:58
 */
public class KeyPreviewFragment extends StelsFragment {
    private byte[] hash;
    private int uid;

    public KeyPreviewFragment(int uid, byte[] hash) {
        this.hash = hash;
        this.uid = uid;
    }

    public KeyPreviewFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.key_fragment, container, false);
        Bitmap bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888);
        int[] colors = new int[]{
                0xffffffff,
                0xffd5e6f3,
                0xff2d5775,
                0xff2f99c9};
        for (int i = 0; i < 64; i++) {
            int index = (hash[i / 4] >> (2 * (i % 4))) & 0x3;
            bitmap.setPixel(i % 8, i / 8, colors[index]);
        }

        int width = getResources().getDisplayMetrics().widthPixels - getPx(64);

        ((ImageView) view.findViewById(R.id.keyImage)).setImageBitmap(Bitmap.createScaledBitmap(bitmap, width, width, false));

        User user = application.getEngine().getUser(uid);

        ((TextView) view.findViewById(R.id.infoMessage1)).setText(html(getStringSafe(R.string.st_secret_key_info1).replace("{name}", "<b>" + user.getFirstName() + "</b>")));

        ((TextView) view.findViewById(R.id.infoMessage2)).setText(html(getStringSafe(R.string.st_secret_key_info2).replace("{name}", "<b>" + user.getFirstName() + "</b>")));

        Linkify.addLinks((TextView) view.findViewById(R.id.infoMessage3), Linkify.WEB_URLS);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu, com.actionbarsherlock.view.MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText(R.string.st_secret_key_title));
        getSherlockActivity().getSupportActionBar().setSubtitle(null);
        getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSherlockActivity().getSupportActionBar().setDisplayShowHomeEnabled(false);
    }
}
