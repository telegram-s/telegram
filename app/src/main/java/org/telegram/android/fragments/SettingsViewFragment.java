package org.telegram.android.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import org.telegram.android.R;
import org.telegram.android.StelsFragment;
import org.telegram.android.config.UserSettings;
import org.telegram.android.views.MessageView;

/**
 * Created by ex3ndr on 18.12.13.
 */
public class SettingsViewFragment extends StelsFragment {

    private TextView fontSizeValue;

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View res = inflater.inflate(R.layout.settings_view, container, false);
        fontSizeValue = (TextView) res.findViewById(R.id.fontSizeValue);
        res.findViewById(R.id.fontSelect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                final int[] titles = new int[]{
                        R.string.st_appearance_font_size_tiny,
                        R.string.st_appearance_font_size_small,
                        R.string.st_appearance_font_size_normal,
                        R.string.st_appearance_font_size_large,
                        R.string.st_appearance_font_size_huge,
                };
                final int[] sizes = new int[]{
                        UserSettings.BUBBLE_FONT_TINY_VALUE,
                        UserSettings.BUBBLE_FONT_SMALL_VALUE,
                        UserSettings.BUBBLE_FONT_NORMAL_VALUE,
                        UserSettings.BUBBLE_FONT_LARGE_VALUE,
                        UserSettings.BUBBLE_FONT_HUGE_VALUE,
                };
                final int[] sizeIds = new int[]{
                        UserSettings.BUBBLE_FONT_TINY,
                        UserSettings.BUBBLE_FONT_SMALL,
                        UserSettings.BUBBLE_FONT_NORMAL,
                        UserSettings.BUBBLE_FONT_LARGE,
                        UserSettings.BUBBLE_FONT_HUGE,
                };

                builder.setAdapter(new BaseAdapter() {
                                       @Override
                                       public int getCount() {
                                           return sizes.length;
                                       }

                                       @Override
                                       public Integer getItem(int i) {
                                           return sizes[i];
                                       }

                                       @Override
                                       public long getItemId(int i) {
                                           return 0;
                                       }

                                       @Override
                                       public View getView(int i, View view, ViewGroup viewGroup) {
                                           View res = inflater.inflate(android.R.layout.simple_dropdown_item_1line, viewGroup, false);
                                           TextView text = (TextView) res.findViewById(android.R.id.text1);
                                           text.setText(titles[i]);
                                           text.setTextSize(getItem(i));
                                           return res;
                                       }
                                   }, new DialogInterface.OnClickListener() {
                                       @Override
                                       public void onClick(DialogInterface dialogInterface, int i) {
                                           application.getUserSettings().setBubbleFontSizeId(sizeIds[i]);
                                           application.getDataSourceKernel().onFontChanged();
                                           MessageView.resetSettings();
                                           bindUi();
                                       }
                                   }
                );
                AlertDialog dialog = builder.create();
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
            }
        });
        bindUi();
        return res;
    }

    private void bindUi() {
        switch (application.getUserSettings().getBubbleFontSizeId()) {
            default:
            case UserSettings.BUBBLE_FONT_NORMAL:
                fontSizeValue.setText(R.string.st_appearance_font_size_normal);
                break;
            case UserSettings.BUBBLE_FONT_HUGE:
                fontSizeValue.setText(R.string.st_appearance_font_size_huge);
                break;
            case UserSettings.BUBBLE_FONT_LARGE:
                fontSizeValue.setText(R.string.st_appearance_font_size_large);
                break;
            case UserSettings.BUBBLE_FONT_SMALL:
                fontSizeValue.setText(R.string.st_appearance_font_size_small);
                break;
            case UserSettings.BUBBLE_FONT_TINY:
                fontSizeValue.setText(R.string.st_appearance_font_size_tiny);
                break;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSherlockActivity().getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText(R.string.st_appearance_title));
        getSherlockActivity().getSupportActionBar().setSubtitle(null);
    }
}
