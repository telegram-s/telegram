package org.telegram.android.base;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.text.BidiFormatter;
import android.text.*;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import org.telegram.android.R;
import org.telegram.android.core.engines.ModelEngine;
import org.telegram.android.core.model.local.TLAbsLocalUserStatus;
import org.telegram.android.core.model.local.TLLocalUserStatusOffline;
import org.telegram.android.core.model.local.TLLocalUserStatusOnline;
import org.telegram.android.fragments.interfaces.FragmentResultController;
import org.telegram.android.core.ApiUtils;
import org.telegram.android.ui.StelsTypefaceSpan;
import org.telegram.android.ui.TelegramContextWrapper;
import org.telegram.android.ui.TextUtil;
import org.telegram.api.*;
import org.telegram.mtproto.time.TimeOverlord;
import org.telegram.notifications.Notifications;
import org.xml.sax.XMLReader;

import java.util.Random;

/**
 * Author: Korshakov Stepan
 * Created: 05.08.13 15:50
 */
public class TelegramFragment extends TelegramBaseFragment {

    private boolean requestedPick;
    private boolean saveInStack = true;

    private Random rnd = new Random();

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

    protected String formatLastSeen(int lastSeen) {
        return TextUtil.formatHumanReadableLastSeen(lastSeen, getStringSafe(R.string.st_lang));
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

    protected CharSequence highlightTitleText(Spannable src) {
        src.setSpan(new ForegroundColorSpan(Color.WHITE), 0, src.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        return src;
    }

    protected CharSequence highlightSubtitleText(int resId) {
        return html("<font_a>" + getStringSafe(resId) + "</font_a>");
    }

    protected CharSequence highlightSubtitleText(String src) {
        return html("<font_a>" + src + "</font_a>");
    }

    protected CharSequence highlightSubtitle2Text(int resId) {
        return html("<font_b>" + getStringSafe(resId) + "</font_b>");
    }

    protected CharSequence highlightSubtitle2Text(String src) {
        return html("<font_b>" + src + "</font_b>");
    }

    protected String unicodeWrap(String src) {
        return BidiFormatter.getInstance().unicodeWrap(src);
    }

    protected CharSequence highlightSecureTitleText(String src) {
        SpannableString res = SpannableString.valueOf("#" + src);
        Drawable srcImage = application.getResources().getDrawable(R.drawable.st_bar_ic_lock);
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
            int fontATagStart = -1;
            int fontBTagStart = -1;

            @Override
            public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
                if (tag.equals("font_a")) {
                    if (opening) {
                        fontATagStart = output.length();
                        if (fontBTagStart >= 0) {
                            output.setSpan(new ForegroundColorSpan(0xd8ffffff), fontBTagStart, output.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            fontBTagStart = -1;
                        }
                    } else {
                        if (fontATagStart >= 0) {
                            output.setSpan(new ForegroundColorSpan(0xB2ffffff), fontATagStart, output.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            fontATagStart = -1;
                        }
                    }
                } else if (tag.equals("font_b")) {
                    if (opening) {
                        fontBTagStart = output.length();
                        if (fontATagStart >= 0) {
                            output.setSpan(new ForegroundColorSpan(0xB2ffffff), fontATagStart, output.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            fontATagStart = -1;
                        }
                    } else {
                        if (fontBTagStart >= 0) {
                            output.setSpan(new ForegroundColorSpan(0xd8ffffff), fontBTagStart, output.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            fontBTagStart = -1;
                        }
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

    public void pickFile() {
        requestedPick = true;
        getRootController().pickFile();
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

    public boolean isParentFragment(TelegramFragment fragment) {
        return true;
    }

    protected void sendEvent(String type) {
        application.getKernel().sendEvent(getClass().getSimpleName() + ":" + type);
    }

    protected void sendEvent(String type, String message) {
        application.getKernel().sendEvent(getClass().getSimpleName() + ":" + type, message);
    }

    public boolean onBackPressed() {
        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
        sendEvent("#resume");
    }

    protected String getUploadTempFile(String fileName) {
        return application.getCacheDir().getAbsolutePath() + "/u_" + rnd.nextInt() + fileName;
    }

    protected String getTempExternalFile(String fileName) {
        if (application.getExternalCacheDir() != null) {
            return application.getExternalCacheDir().getAbsolutePath() + "/u_" + rnd.nextInt() + fileName;
        } else {
            return "/sdcard/u_" + rnd.nextInt() + fileName;
        }
    }

    protected Notifications getNotifications() {
        return application.getUiKernel().getUiNotifications();
    }

    protected LayoutInflater wrap(LayoutInflater inflater) {
        return inflater.cloneInContext(getWrappedContext());
    }

    protected Context getWrappedContext() {
        return new TelegramContextWrapper(getActivity());
    }
}
