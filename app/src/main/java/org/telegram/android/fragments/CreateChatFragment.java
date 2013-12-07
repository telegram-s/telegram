package org.telegram.android.fragments;

import android.content.Context;
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
import com.extradea.framework.images.ui.FastWebImageView;
import org.telegram.android.R;
import org.telegram.android.StelsFragment;
import org.telegram.android.core.model.User;
import org.telegram.android.core.model.media.TLLocalAvatarPhoto;
import org.telegram.android.core.model.media.TLLocalFileLocation;
import org.telegram.android.media.StelsImageTask;
import org.telegram.android.tasks.AsyncAction;
import org.telegram.android.tasks.AsyncException;
import org.telegram.android.ui.FilterMatcher;
import org.telegram.android.ui.Placeholders;
import org.telegram.android.ui.TextUtil;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Author: Korshakov Stepan
 * Created: 05.08.13 14:02
 */
public class CreateChatFragment extends StelsFragment {

    private EditText inputEdit;
    private ListView contactsList;
    private User[] originalUsers;
    private User[] filteredUsers;
    private BaseAdapter contactsAdapter;
    private CopyOnWriteArrayList<User> selected = new CopyOnWriteArrayList<User>();
    private FilterMatcher matcher;
    private View empty;
    private TextView counterView;
    private TextView doneButton;
    private View mainContainer;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getSherlockActivity().invalidateOptionsMenu();
        updateHeaderPadding();
    }

    private void updateHeaderPadding() {
        if (mainContainer == null) {
            return;
        }
        mainContainer.setPadding(0, getBarHeight(), 0, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View res = inflater.inflate(R.layout.create_chat_fragment, container, false);
        mainContainer = res.findViewById(R.id.mainContainer);
        updateHeaderPadding();
        inputEdit = (EditText) res.findViewById(R.id.inputEdit);
        contactsList = (ListView) res.findViewById(R.id.contacts);
        counterView = (TextView) res.findViewById(R.id.counter);
        empty = res.findViewById(R.id.empty);
        inputEdit.addTextChangedListener(new TextWatcher() {
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
                if (matcher != null && matcher.getQuery().equals(filter)) {
                    return;
                }
                matcher = new FilterMatcher(filter);
                updateFilter();
            }
        });
//        if (originalUsers == null) {
//            if (application.getContactsSource().isCacheAlive()) {
//                originalUsers = application.getContactsSource().getSortedByNameUsers();
//                filteredUsers = originalUsers;
//                bindUi();
//            } else {
//                runUiTask(new AsyncAction() {
//                    @Override
//                    public void execute() throws AsyncException {
//                        originalUsers = application.getContactsSource().getSortedByNameUsers();
//                        filteredUsers = originalUsers;
//                    }
//
//                    @Override
//                    public void afterExecute() {
//                        bindUi();
//                    }
//                });
//            }
//        }
        return res;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateHeaderPadding();
    }

    private void bindUi() {
        final Context context = getActivity();
        contactsAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return filteredUsers.length;
            }

            @Override
            public User getItem(int i) {
                return filteredUsers[i];
            }

            @Override
            public long getItemId(int i) {
                return getItem(i).getUid();
            }

            @Override
            public boolean areAllItemsEnabled() {
                return false;
            }

            @Override
            public boolean isEnabled(int position) {
                return false;
            }

            @Override
            public View getView(int index, View view, ViewGroup viewGroup) {
                if (view == null) {
                    view = View.inflate(context, R.layout.contacts_item_select, null);
                }
                final User object = getItem(index);
                boolean showHeader = index == 0;
                if (index > 0) {
                    User prev = getItem(index - 1);
                    showHeader = !object.getSortingName().substring(0, 1).equalsIgnoreCase(prev.getSortingName().substring(0, 1));
                }

                if (showHeader) {
                    view.findViewById(R.id.separatorContainer).setVisibility(View.VISIBLE);
                    if (object.getSortingName().length() == 0) {
                        ((TextView) view.findViewById(R.id.separator)).setText("#");
                    } else {
                        ((TextView) view.findViewById(R.id.separator)).setText(object.getSortingName().substring(0, 1));
                    }
                } else {
                    view.findViewById(R.id.separatorContainer).setVisibility(View.GONE);
                }

                if (matcher != null) {
                    SpannableString string = new SpannableString(object.getDisplayName());
                    matcher.highlight(context, string);
                    ((TextView) view.findViewById(R.id.name)).setText(string);
                } else {
                    ((TextView) view.findViewById(R.id.name)).setText(object.getDisplayName());
                }

                FastWebImageView imageView = (FastWebImageView) view.findViewById(R.id.avatar);
                imageView.setLoadingDrawable(Placeholders.USER_PLACEHOLDERS[object.getUid() % Placeholders.USER_PLACEHOLDERS.length]);

                if (object.getPhoto() instanceof TLLocalAvatarPhoto) {
                    TLLocalAvatarPhoto userPhoto = (TLLocalAvatarPhoto) object.getPhoto();
                    if (userPhoto.getPreviewLocation() instanceof TLLocalFileLocation) {
                        imageView.requestTask(new StelsImageTask((TLLocalFileLocation) userPhoto.getPreviewLocation()));
                    } else {
                        imageView.requestTask(null);
                    }
                } else {
                    imageView.requestTask(null);
                }

                int status = getUserState(object.getStatus());
                if (status < 0) {
                    ((TextView) view.findViewById(R.id.status)).setText(R.string.st_offline);
                    ((TextView) view.findViewById(R.id.status)).setTextColor(context.getResources().getColor(R.color.st_grey_text));
                } else if (status == 0) {
                    ((TextView) view.findViewById(R.id.status)).setText(R.string.st_online);
                    ((TextView) view.findViewById(R.id.status)).setTextColor(context.getResources().getColor(R.color.st_blue_bright));
                } else {
                    ((TextView) view.findViewById(R.id.status)).setTextColor(context.getResources().getColor(R.color.st_grey_text));
                    ((TextView) view.findViewById(R.id.status)).setText(
                            TextUtil.formatHumanReadableLastSeen(status, getStringSafe(R.string.st_lang)));
                }

                final ImageView checkView = (ImageView) view.findViewById(R.id.contactSelected);
                if (selected.contains(object)) {
                    checkView.setImageResource(R.drawable.holo_btn_check_on);
                } else {
                    checkView.setImageResource(R.drawable.holo_btn_check_off);
                }

                view.findViewById(R.id.contactContainer).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (selected.contains(object)) {
                            removeUser(object);
                            checkView.setImageResource(R.drawable.holo_btn_check_off);
                        } else {
                            if (selected.size() < 100) {
                                addUser(object);
                                checkView.setImageResource(R.drawable.holo_btn_check_on);
                            } else {
                                Toast.makeText(getActivity(), R.string.st_new_group_maximum, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });

                return view;
            }
        };
        contactsList.setAdapter(contactsAdapter);
        updateFilter();
    }

    private void updateCounter() {
        counterView.setText(selected.size() + "/100");
        if (doneButton != null) {
            doneButton.setText(getStringSafe(R.string.st_new_group_next) + " (" + selected.size() + ")");
        }
    }

    private void updateFilter() {
        if (contactsAdapter == null) {
            return;
        }
        if (matcher != null) {
            ArrayList<User> filtered = new ArrayList<User>();
            for (User u : originalUsers) {
                if (matcher.isMatched(u.getDisplayName())) {
                    filtered.add(u);
                }
            }
            if (filtered.size() == 1) {
                addUser(filtered.get(0));
                return;
            }
            filteredUsers = filtered.toArray(new User[0]);
            contactsAdapter.notifyDataSetChanged();
        } else {
            filteredUsers = originalUsers;
            contactsAdapter.notifyDataSetChanged();
        }

        if (empty != null) {
            if (filteredUsers.length == 0) {
                empty.setVisibility(View.VISIBLE);
            } else {
                empty.setVisibility(View.GONE);
            }
        }
    }

    private void addUser(User user) {
        if (selected.size() > 100) {
            Toast.makeText(getActivity(), R.string.st_new_group_maximum, Toast.LENGTH_SHORT).show();
            return;
        }
        selected.add(user);
        updateEditText();
        updateCounter();
    }

    private void removeUser(User user) {
        selected.remove(user);
        updateEditText();
        updateCounter();
    }

    private void checkForDeletions(Editable editable) {
        boolean hasDeletions = false;
        UserSpan[] spans = editable.getSpans(0, editable.length(), UserSpan.class);
        for (User u : selected) {
            boolean founded = false;
            for (UserSpan span : spans) {
                if (span.getUser() == u) {
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
            contactsList.invalidateViews();
        }
    }

    private void updateEditText() {
        String src = "";
        for (int i = 0; i < selected.size(); i++) {
            src += "!";
        }
        Spannable spannable = new SpannableString(src);
        for (int i = 0; i < selected.size(); i++) {
            spannable.setSpan(new UserSpan(selected.get(i), getPx(200)), i, i + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        }
        inputEdit.setText(spannable);
        inputEdit.setSelection(spannable.length());
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
        inflater.inflate(R.menu.create_chat_menu, menu);
        doneButton = (TextView) menu.findItem(R.id.done).getActionView().findViewById(R.id.doneButton);
        doneButton.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, getBarHeight()));
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doCreateChat();
            }
        });

        getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSherlockActivity().getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText(R.string.st_new_group_title));
        getSherlockActivity().getSupportActionBar().setSubtitle(null);
    }

    private void doCreateChat() {
        if (selected.size() > 0) {
            User[] bUids = selected.toArray(new User[selected.size()]);
            int[] uids = new int[bUids.length];
            for (int i = 0; i < uids.length; i++) {
                uids[i] = bUids[i].getUid();
            }
            getRootController().completeCreateChat(uids);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        hideKeyboard(inputEdit);
        contactsList = null;
        inputEdit = null;
        counterView = null;
        empty = null;
        doneButton = null;
    }
}
