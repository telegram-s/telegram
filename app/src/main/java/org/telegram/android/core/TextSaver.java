package org.telegram.android.core;

import android.content.SharedPreferences;
import com.extradea.framework.persistence.ContextPersistence;
import org.telegram.android.StelsApplication;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 18.10.13
 * Time: 22:29
 */
public class TextSaver extends ContextPersistence {

    static final long serialVersionUID = 1L;

    private HashMap<Long, String> savedText = new HashMap<Long, String>();

    public TextSaver(StelsApplication application) {
        super(application);
        tryLoad();
    }

    public void saveText(String text, int peerType, int peerId) {
        savedText.put(peerId * 10L + peerType, text);
        trySave();
    }

    public void clearText(int peerType, int peerId) {
        if (savedText.containsKey(peerId * 10L + peerType)) {
            savedText.remove(peerId * 10L + peerType);
            trySave();
        }
    }

    public String getText(int peerType, int peerId) {
        return savedText.get(peerId * 10L + peerType);
    }

    public void reset() {
        savedText.clear();
        trySave();
    }
}
