package org.telegram.android.kernel;

import android.text.SpannableString;
import org.telegram.android.StelsApplication;
import org.telegram.android.core.model.DialogDescription;
import org.telegram.android.core.model.PeerType;
import org.telegram.android.core.model.SearchWireframe;
import org.telegram.android.core.model.User;
import org.telegram.android.ui.FilterMatcher;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Author: Korshakov Stepan
 * Created: 24.08.13 22:35
 */
public class SearchKernel {
    private StelsApplication application;
    private DialogDescription[] descriptions;

    public SearchKernel(StelsApplication application) {
        this.application = application;
    }

    public SearchWireframe[] doSearch(FilterMatcher matcher) {
        if (descriptions == null) {
            descriptions = application.getEngine().getDialogsDao().queryForAll().toArray(new DialogDescription[0]);
        }

        HashSet<Long> founded = new HashSet<Long>();

        ArrayList<SearchWireframe> wireframes = new ArrayList<SearchWireframe>();

        for (DialogDescription description : application.getDialogSource().getViewSource().getCurrentWorkingSet()) {
            long id = description.getPeerId() * 2 + description.getPeerType();
            if (founded.contains(id)) {
                continue;
            }
            if (matcher.isMatched(description.getTitle())) {
                founded.add(id);
                SpannableString spannableString = new SpannableString(description.getTitle());
                if (description.getPeerType() == PeerType.PEER_USER) {
                    User usr = application.getEngine().getUser(description.getPeerId());
                    wireframes.add(new SearchWireframe(description.getPeerId(), description.getPeerType(), description.getTitle(), spannableString, description.getPhoto(), usr.getStatus(), 0));
                } else {
                    wireframes.add(new SearchWireframe(description.getPeerId(), description.getPeerType(), description.getTitle(), spannableString, description.getPhoto(), null, description.getParticipantsCount()));
                }
            }
        }

        for (DialogDescription description : descriptions) {
            long id = description.getPeerId() * 2 + description.getPeerType();
            if (founded.contains(id)) {
                continue;
            }
            if (matcher.isMatched(description.getTitle())) {
                founded.add(id);
                SpannableString spannableString = new SpannableString(description.getTitle());
                if (description.getPeerType() == PeerType.PEER_USER) {
                    User usr = application.getEngine().getUser(description.getPeerId());
                    wireframes.add(new SearchWireframe(description.getPeerId(), description.getPeerType(), description.getTitle(), spannableString, description.getPhoto(), usr.getStatus(), 0));
                } else {
                    wireframes.add(new SearchWireframe(description.getPeerId(), description.getPeerType(), description.getTitle(), spannableString, description.getPhoto(), null, description.getParticipantsCount()));
                }
            }
        }

//        User[] contacts = application.getContactsSource().getSortedUsers();
//
//        for (User u : contacts) {
//            long id = u.getUid() * 2 + PeerType.PEER_USER;
//            if (founded.contains(id)) {
//                continue;
//            }
//            if (matcher.isMatched(u.getDisplayName())) {
//                founded.add(id);
//                SpannableString spannableString = new SpannableString(u.getDisplayName());
//                wireframes.add(new SearchWireframe(u.getUid(), PeerType.PEER_USER, u.getDisplayName(), spannableString, u.getPhoto(), u.getStatus(), 0));
//            }
//        }

        return wireframes.toArray(new SearchWireframe[0]);
    }
}