package org.telegram.android.fragments;

import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.text.*;
import android.text.style.ReplacementSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.telegram.android.R;
import org.telegram.android.core.ContactsSource;
import org.telegram.android.core.model.User;
import org.telegram.android.core.wireframes.ContactWireframe;
import org.telegram.android.fragments.common.BaseContactsFragment;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Author: Korshakov Stepan
 * Created: 05.08.13 14:02
 */
public class CreateChatFragment extends BaseContactsFragment {

    private EditText inputEdit;
    private CopyOnWriteArrayList<Integer> selected = new CopyOnWriteArrayList<Integer>();
    private TextView counterView;
    private TextView doneButton;
    private View headerContainer;

    private TextWatcher textWatcher;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getSherlockActivity().invalidateOptionsMenu();
    }

    @Override
    protected boolean showOnlyTelegramContacts() {
        return true;
    }

    @Override
    protected boolean useMultipleSelection() {
        return true;
    }

    @Override
    protected boolean isSelected(int index) {
        ContactWireframe contact = getContactAt(index);
        return selected.contains(contact.getRelatedUsers()[0].getUid());
    }

    @Override
    protected int getLayout() {
        return R.layout.create_chat_fragment;
    }

    @Override
    protected void hideContent() {
        if (headerContainer != null) {
            headerContainer.setVisibility(View.GONE);
        }
    }

    @Override
    protected void showContent() {
        if (headerContainer != null) {
            headerContainer.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onCreateView(View view, LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        inputEdit = (EditText) view.findViewById(R.id.inputEdit);
        headerContainer = view.findViewById(R.id.header);
        counterView = (TextView) view.findViewById(R.id.counter);
        textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                checkForDeletions(editable);
                String filter = editable.toString().trim();
                while (filter.length() > 0 && filter.charAt(0) == '!') {
                    filter = filter.substring(1);
                }
                doFilter(filter);
                if (getContactsCount() == 1) {
                    addUser(getContactAt(0).getRelatedUsers()[0]);
                }
            }
        };
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        ContactWireframe contact = (ContactWireframe) adapterView.getItemAtPosition(i);
        if (selected.contains(contact.getRelatedUsers()[0].getUid())) {
            removeUser(contact.getRelatedUsers()[0]);
        } else {
            if (selected.size() < application.getTechKernel().getSystemConfig().getMaxChatSize()) {
                addUser(contact.getRelatedUsers()[0]);
            } else {
                Toast.makeText(getActivity(), R.string.st_new_group_maximum, Toast.LENGTH_SHORT).show();
            }
        }

        getListView().invalidateViews();
    }

    private void updateCounter() {
        counterView.setText(selected.size() + "/" + application.getTechKernel().getSystemConfig().getMaxChatSize());
        if (doneButton != null) {
            doneButton.setText(getStringSafe(R.string.st_new_group_next) + " (" + selected.size() + ")");
        }
    }

    private void addUser(User user) {
        if (selected.size() > application.getTechKernel().getSystemConfig().getMaxChatSize()) {
            Toast.makeText(getActivity(), R.string.st_new_group_maximum, Toast.LENGTH_SHORT).show();
            return;
        }
        selected.add(user.getUid());
        updateEditText();
        updateCounter();
    }

    private void removeUser(User user) {
        selected.remove((Object) user.getUid());
        updateEditText();
        updateCounter();
    }

    private void checkForDeletions(Editable editable) {
        boolean hasDeletions = false;
        UserSpan[] spans = editable.getSpans(0, editable.length(), UserSpan.class);
        for (Integer u : selected) {
            boolean founded = false;
            for (UserSpan span : spans) {
                if (span.getUser().getUid() == u) {
                    if (editable.getSpanStart(span) == editable.getSpanEnd(span)) {
                        break;
                    } else {
                        founded = true;
                        break;
                    }
                }
            }

            if (!founded) {
                hasDeletions = true;
                selected.remove(u);
            }
        }
        if (hasDeletions) {
            updateCounter();
            reloadData();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        inputEdit.addTextChangedListener(textWatcher);
        updateCounter();
    }

    private void updateEditText() {
        String src = "";
        for (int i = 0; i < selected.size(); i++) {
            src += "!";
        }
        Spannable spannable = new SpannableString(src);
        for (int i = 0; i < selected.size(); i++) {
            spannable.setSpan(new UserSpan(application.getEngine().getUser(selected.get(i)), getPx(200)), i, i + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        }
        inputEdit.removeTextChangedListener(textWatcher);
        inputEdit.setText(spannable);
        inputEdit.setSelection(spannable.length());
        inputEdit.addTextChangedListener(textWatcher);
    }

    private static TextPaint textPaint;

    private class UserSpan extends ReplacementSpan {

        private User user;
        private int maxW;
        private String userText;

        private User getUser() {
            return user;
        }

        public UserSpan(User user, int maxW) {
            this.user = user;
            this.maxW = maxW;
            if (textPaint == null) {
                textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
                textPaint.setTextSize(getSp(16));
                textPaint.setColor(0xff035EA8);
            }

            int padding = getPx(36);
            int maxWidth = maxW - padding;
            userText = TextUtils.ellipsize(user.getDisplayName(), textPaint, maxWidth, TextUtils.TruncateAt.END).toString();
        }

        @Override
        public int getSize(Paint paint, CharSequence charSequence, int start, int end, Paint.FontMetricsInt fm) {
            if (fm != null) {
                fm.ascent = -getPx(21 + 3);
                fm.descent = getPx(10 + 3);

                fm.top = fm.ascent;
                fm.bottom = fm.descent;
            }
            return (int) textPaint.measureText(userText) + getPx(36);
        }

        @Override
        public void draw(Canvas canvas, CharSequence charSequence, int start, int end,
                         float x, int top, int y, int bottom, Paint paint) {
            int size = (int) textPaint.measureText(userText);
            Paint debug = new Paint();
            debug.setColor(0xffCFEAFF);
            debug.setStyle(Paint.Style.FILL_AND_STROKE);
            canvas.drawRoundRect(new RectF(x, y - getPx(21), x + size + getPx(24), y + getPx(10)), getPx(1), getPx(1), debug);
            canvas.drawText(userText, x + getPx(12), y, textPaint);
        }
    }

    @Override
    public void onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu, com.actionbarsherlock.view.MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSherlockActivity().getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText(R.string.st_new_group_title));
        getSherlockActivity().getSupportActionBar().setSubtitle(null);

        if (!isLoaded()) {
            return;
        }

        inflater.inflate(R.menu.create_chat_menu, menu);
        doneButton = (TextView) menu.findItem(R.id.done).getActionView().findViewById(R.id.doneButton);
        doneButton.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, getBarHeight()));
        doneButton.setOnClickListener(secure(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doCreateChat();
            }
        }));
    }

    private void doCreateChat() {
        if (selected.size() > 0) {
            Integer[] bUids = selected.toArray(new Integer[selected.size()]);
            int[] uids = new int[bUids.length];
            for (int i = 0; i < uids.length; i++) {
                uids[i] = bUids[i];
            }
            getRootController().completeCreateChat(uids);
        }
    }

    @Override
    public boolean filterItem(ContactWireframe contact) {
        if (contact.getRelatedUsers()[0].getUid() == application.getCurrentUid()) {
            return false;
        }
        if (selected.contains(contact.getRelatedUsers()[0].getUid()) && isFiltering()) {
            return false;
        }
        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        hideKeyboard(inputEdit);
        inputEdit = null;
        counterView = null;
        doneButton = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        inputEdit.removeTextChangedListener(textWatcher);
    }
}
