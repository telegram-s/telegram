package org.telegram.android.core;

import android.content.Context;
import android.content.SharedPreferences;
import org.telegram.android.StelsApplication;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 18.10.13
 * Time: 22:29
 */
public class TextSaver {

    private SharedPreferences preferences;

    private HashMap<String, String> savedText = new HashMap<String, String>();

    public TextSaver(StelsApplication application) {
        preferences = application.getSharedPreferences("org.telegram.android.TextSaver.pref", Context.MODE_PRIVATE);
        preferences.getAll();
    }

    private String getKey(int peerType, int peerId) {
        return (peerId * 10L + peerType) + "";
    }

    public void saveText(String text, int peerType, int peerId) {
        String key = getKey(peerType, peerId);
        savedText.put(key, text);
        preferences.edit().putString(key, text).commit();
    }

    public void clearText(int peerType, int peerId) {
        String key = getKey(peerType, peerId);
        if (savedText.containsKey(key)) {
            preferences.edit().remove(key).commit();
            savedText.remove(key);
        }
    }

    public String getText(int peerType, int peerId) {
        String key = getKey(peerType, peerId);
        return savedText.get(key);
    }

    public void reset() {
        savedText.clear();
        SharedPreferences.Editor editor = preferences.edit();
        for (String key : savedText.keySet()) {
            editor.remove(key);
        }
        editor.commit();
    }
}
