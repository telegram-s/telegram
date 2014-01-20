package org.telegram.android.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import com.actionbarsherlock.view.MenuItem;
import org.telegram.android.R;
import org.telegram.android.base.TelegramFragment;
import org.telegram.android.core.model.Group;
import org.telegram.android.core.model.update.TLLocalEditChatTitle;
import org.telegram.android.tasks.AsyncAction;
import org.telegram.android.tasks.AsyncException;
import org.telegram.api.TLMessageActionChatEditTitle;
import org.telegram.api.TLMessageService;
import org.telegram.api.engine.RpcException;
import org.telegram.api.messages.TLAbsStatedMessage;
import org.telegram.api.requests.TLRequestMessagesEditChatTitle;

/**
 * Author: Korshakov Stepan
 * Created: 10.08.13 18:19
 */
public class EditChatTitleFragment extends TelegramFragment {

    private EditText chatTitleView;
    private int chatId;

    public EditChatTitleFragment(int chatId) {
        this.chatId = chatId;
    }

    public EditChatTitleFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View res = inflater.inflate(R.layout.edit_chat_title, container, false);
        chatTitleView = (EditText) res.findViewById(R.id.title);
        fixEditText(chatTitleView);
        if (savedInstanceState == null) {
            Group description = application.getEngine().getGroupsEngine().getGroup(chatId);
            if (description != null) {
                chatTitleView.setText(description.getTitle());
            }
        }
        return res;
    }

    @Override
    public void onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu, com.actionbarsherlock.view.MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.create_chat_complete_menu, menu);
        menu.findItem(R.id.done).setTitle(highlightMenuText(R.string.st_edit_title_done));
        getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSherlockActivity().getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText(R.string.st_edit_title_title));
        getSherlockActivity().getSupportActionBar().setSubtitle(null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.done) {
            doEdit();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        showKeyboard(chatTitleView);
    }

    @Override
    public void onPause() {
        super.onPause();
        hideKeyboard(chatTitleView);
    }

    private void doEdit() {
        final String title = chatTitleView.getText().toString().trim();
        if (title.length() > 0) {
            runUiTask(new AsyncAction() {
                @Override
                public void execute() throws AsyncException {
                    try {
                        TLAbsStatedMessage message = rpcRaw(new TLRequestMessagesEditChatTitle(chatId, title));
                        TLMessageService service = (TLMessageService) message.getMessage();
                        TLMessageActionChatEditTitle editTitle = (TLMessageActionChatEditTitle) service.getAction();
                        application.getEngine().onUsers(message.getUsers());
                        application.getEngine().getGroupsEngine().onGroupsUpdated(message.getChats());
                        application.getEngine().onUpdatedMessage(message.getMessage());
                        application.getEngine().onChatTitleChanges(chatId, editTitle.getTitle());
                        application.getUpdateProcessor().onMessage(new TLLocalEditChatTitle(message));
                        application.notifyUIUpdate();
                    } catch (RpcException e) {
                        if (!"CHAT_TITLE_NOT_MODIFIED".equals(e.getErrorTag())) {
                            throw new AsyncException(e);
                        }
                    }
                }

                @Override
                public void afterExecute() {
                    getActivity().onBackPressed();
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        chatTitleView = null;
    }
}