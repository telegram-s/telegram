package org.telegram.android;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.text.BidiFormatter;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import org.telegram.android.core.ModelEngine;
import org.telegram.android.core.model.local.TLAbsLocalUserStatus;
import org.telegram.android.core.model.local.TLLocalUserStatusOffline;
import org.telegram.android.core.model.local.TLLocalUserStatusOnline;
import org.telegram.android.fragments.PickUserFragment;
import org.telegram.android.fragments.interfaces.FragmentResultController;
import org.telegram.android.core.ApiUtils;
import org.telegram.android.ui.StelsTypefaceSpan;
import org.telegram.api.*;
import org.telegram.mtproto.time.TimeOverlord;
import org.xml.sax.XMLReader;

/**
 * Author: Korshakov Stepan
 * Created: 05.08.13 15:50
 */
public class StelsFragment extends StelsBaseFragment {

    private boolean requestedPick;
    private boolean saveInStack = true;

    public boolean isSaveInStack() {
        return saveInStack;
    }

    public void setSaveInStack(boolean saveInStack) {
        this.saveInStack = saveInStack;
    }

    protected TLAbsPhotoSize findLargest(org.telegram.api.TLPhoto photo) {
        return ApiUtils.findLargest(photo);
    }

    protected TLAbsPhotoSize findSmallest(org.telegram.api.TLPhoto photo) {
        return ApiUtils.findSmallest(photo);
    }

    protected TLPhotoCachedSize findCachedSize(org.telegram.api.TLPhoto photo) {
        return ApiUtils.findCachedSize(photo);
    }

    protected TLPhotoSize findDownloadSize(org.telegram.api.TLPhoto photo) {
        return ApiUtils.findDownloadSize(photo);
    }

    protected int getUserState(TLAbsLocalUserStatus status) {
        if (status != null) {
            if (status instanceof TLLocalUserStatusOnline) {
                TLLocalUserStatusOnline online = (TLLocalUserStatusOnline) status;
                if (getServerTime() > online.getExpires()) {
                    return online.getExpires(); // Last seen
                } else {
                    return 0; // Online
                }
            } else if (status instanceof TLLocalUserStatusOffline) {
                TLLocalUserStatusOffline offline = (TLLocalUserStatusOffline) status;
                return offline.getWasOnline(); // Last seen
            } else {
                return -1; // Offline
            }
        } else {
            return -1; // Offline
        }
    }

    protected CharSequence highlightMenuText(int resId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return html("<font color='#010101'>" + getStringSafe(resId) + "</font>");
        } else {
            return getStringSafe(resId);
        }
    }

    protected CharSequence highlightTitleDisabledText(int resId) {
        return getStringSafe(resId);
        // return html("<font color='#ffffff'>" + getStringSafe(resId) + "</font>");
    }

    protected CharSequence highlightTitleDisabledText(String src) {
        return src;
        // return html("<font color='#ffffff'>" + src + "</font>");
    }

    protected CharSequence highlightTitleText(int resId) {
        //return html("<font color='#006FC8'>" + getStringSafe(resId) + "</font>");
        return html("<font color='#ffffff'>" + getStringSafe(resId) + "</font>");
    }

    protected CharSequence highlightTitleText(String src) {
        // return html("<font color='#006FC8'>" + src + "</font>");
        return html("<font color='#ffffff'>" + src + "</font>");
    }

    protected CharSequence highlightSubtitleText(int resId) {
        //return html("<font color='#006FC8'>" + getStringSafe(resId) + "</font>");
        return html("<font color='#d0dbe7'>" + getStringSafe(resId) + "</font>");
    }

    protected CharSequence highlightSubtitleText(String src) {
        // return html("<font color='#006FC8'>" + src + "</font>");
        return html("<font color='#d0dbe7'>" + src + "</font>");
    }

    protected CharSequence highlightText(int resId) {
        return html("<font color='#006FC8'>" + getStringSafe(resId) + "</font>");
    }

    protected CharSequence highlightText(String src) {
        return html("<font color='#006FC8'>" + src + "</font>");
    }

    protected String unicodeWrap(String src) {
        return BidiFormatter.getInstance().unicodeWrap(src);
    }

    protected CharSequence highlightSecureTitleText(String src) {
        SpannableString res = SpannableString.valueOf("#" + src);
        Drawable srcImage = application.getResources().getDrawable(R.drawable.st_bar_lock);
        srcImage.setBounds(0, 0, srcImage.getIntrinsicWidth(), srcImage.getIntrinsicHeight());
        Drawable inset = new InsetDrawable(srcImage, 0, 0, getPx(4), 0);
        inset.setBounds(0, 0, srcImage.getIntrinsicWidth() + getPx(4), srcImage.getIntrinsicHeight());
        res.setSpan(new ImageSpan(inset, ImageSpan.ALIGN_BASELINE), 0, 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        //res.setSpan(new ForegroundColorSpan(0xff67b540), 1, src.length() + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        res.setSpan(new ForegroundColorSpan(0xffffffff), 1, src.length() + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return res;
    }

    protected Spanned html(String src) {
        return Html.fromHtml(src, null, new Html.TagHandler() {
            int lightTagStart;

            @Override
            public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
                if (tag.equals("light")) {
                    if (opening) {
                        lightTagStart = output.length();
                    } else {
                        output.setSpan(new StelsTypefaceSpan("light", getActivity(), false), lightTagStart, output.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }
        });
    }

    protected int getServerTime() {
        return (int) (TimeOverlord.getInstance().getServerTime() / 1000);
    }

    protected ModelEngine getEngine() {
        return application.getEngine();
    }

    protected void copyToPastebin(String data) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard =
                    (android.text.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(data);
        } else {
            android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Stels messenger", data);
            clipboard.setPrimaryClip(clip);
        }
    }

    public void pickUser() {
        requestedPick = true;
        getRootController().pickUser();
    }

    public void pickUserAll() {
        requestedPick = true;
        getRootController().pickUser();
    }

    public void pickLocation() {
        requestedPick = true;
        getRootController().pickLocations();
    }

    protected void onFragmentResult(int resultCode, Object data) {

    }

    protected void setResult(int resultCode, Object data) {
        ((FragmentResultController) getActivity()).setResult(resultCode, data);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (requestedPick) {
            requestedPick = false;
            onFragmentResult(((FragmentResultController) getActivity()).getResultCode(),
                    ((FragmentResultController) getActivity()).getResultData());
        }

        sendEvent("#resume");

        // getActivity().getWindow().setBackgroundDrawable(getBackgroundDrawable());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        getSherlockActivity().getSupportActionBar().setDisplayShowCustomEnabled(false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("stels:requestedPick", requestedPick);
        outState.putBoolean("stels:saveInStack", saveInStack);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        setHasOptionsMenu(false);
    }

    public Drawable getBackgroundDrawable() {
        return new ColorDrawable(Color.WHITE);
    }

    public boolean isParentFragment(StelsFragment fragment) {
        return true;
    }

    protected void sendEvent(String type) {
        application.getKernel().sendEvent(getClass().getSimpleName() + ":" + type);
    }

    protected void sendEvent(String type, String message) {
        application.getKernel().sendEvent(getClass().getSimpleName() + ":" +type, message);
    }

    @Override
    public void onPause() {
        super.onPause();
        sendEvent("#resume");
    }
}
