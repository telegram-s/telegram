package org.telegram.android.kernel;

import android.text.SpannableString;
import org.telegram.android.TelegramApplication;
import org.telegram.android.core.ContactsSource;
import org.telegram.android.core.model.DialogDescription;
import org.telegram.android.core.model.PeerType;
import org.telegram.android.core.model.SearchWireframe;
import org.telegram.android.core.model.User;
import org.telegram.android.core.wireframes.ContactWireframe;
import org.telegram.android.core.wireframes.DialogWireframe;
import org.telegram.android.ui.FilterMatcher;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Author: Korshakov Stepan
 * Created: 24.08.13 22:35
 */
public class SearchKernel {
    private TelegramApplication application;
    private DialogDescription[] descriptions;

    public SearchKernel(TelegramApplication application) {
        this.application = application;
    }

    public SearchWireframe[] doSearch(FilterMatcher matcher) {
        if (descriptions == null) {
            descriptions = application.getEngine().getDialogsEngine().getAll();
        }

        HashSet<Long> founded = new HashSet<Long>();

        ArrayList<SearchWireframe> wireframes = new ArrayList<SearchWireframe>();

        for (DialogWireframe description : application.getDialogSource().getViewSource().getCurrentWorkingSet()) {
            long id = description.getPeerId() * 2 + description.getPeerType();
            if (founded.contains(id)) {
                continue;
            }
            if (matcher.isMatched(description.getDialogTitle())) {
                founded.add(id);
                SpannableString spannableString = new SpannableString(description.getDialogTitle());
                if (description.getPeerType() == PeerType.PEER_USER) {
                    User usr = application.getEngine().getUser(description.getPeerId());
                    wireframes.add(new SearchWireframe(description.getPeerId(), description.getPeerType(), description.getDialogTitle(), spannableString, description.getDialogAvatar(), usr.getStatus(), 0));
                } else if (description.getPeerType() == PeerType.PEER_CHAT) {
                    wireframes.add(new SearchWireframe(description.getPeerId(), description.getPeerType(), description.getDialogTitle(), spannableString, description.getDialogAvatar(), null, 0));
                }
            }
        }

        ContactWireframe[] contacts = application.getContactsSource().getTelegramContacts();

        if (contacts != null) {
            for (ContactWireframe c : contacts) {
                User u = c.getRelatedUsers()[0];
                long id = u.getUid() * 2 + PeerType.PEER_USER;
                if (founded.contains(id)) {
                    continue;
                }
                if (matcher.isMatched(u.getDisplayName())) {
                    founded.add(id);
                    SpannableString spannableString = new SpannableString(u.getDisplayName());
                    wireframes.add(new SearchWireframe(u.getUid(), PeerType.PEER_USER, u.getDisplayName(), spannableString, u.getPhoto(), u.getStatus(), 0));
                }
            }
        }

        return wireframes.toArray(new SearchWireframe[0]);
    }
}