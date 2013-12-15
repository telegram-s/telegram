package org.telegram.android.fragments.interfaces;

import android.graphics.Bitmap;
import android.view.View;
import org.telegram.android.core.model.media.TLLocalFileLocation;

/**
 * Author: Korshakov Stepan
 * Created: 29.07.13 5:46
 */
public interface RootController {

    /* Send to chat */

    public void shareContact(int uid);

    public void forwardMessage(int mid);

    public void forwardMessages(Integer[] mid);

    public void sendText(String text);

    public void sendImage(String uri);

    public void sendImages(String[] uris);

    public void sendVideo(String uri);

    /* General */

    public void doBack();

    public void openDialogs(boolean fromRoot);

    public void popFragment(int count);

    public void openDialog(int peerType, int peerId);

    public void openMedia(int peerType, int peerId);

    public void openImageAnimated(int mid, int peerType, int peerId, View view, Bitmap preview, int x, int y);

    public void openImage(int mid, int peerType, int peerId);

    public void openImage(TLLocalFileLocation fileLocation);

    public void openUser(int uid);

    public void openSecretChatInfo(int chatId);

    public void openKeyPreview(byte[] hash, int uid);

    public void openContacts();

    public void openSettings();

    public void openNameSettings();

    public void openDebugSettings();

    public void openNotificationSettings();

    public void openWallpaperSettings();

    public void openBlocked();

    public void onLogout();

    public void pickUser();

    public void pickFile();

    public void pickLocations();

    public void viewLocation(double lat, double lon, int uid);

    public void onRecovered();

    public void onCloseWhatsNew();

    public void showPanel();

    public void hidePanel();

    public void openApp();

    /* Chats */

    public void openCreateNewChat();

    public void completeCreateChat(int[] uids);

    public void onChatCreated(int chatId);

    public void openChatEdit(int chatId);

    public void openChatEditTitle(int chatId);

    public void addContact(int uid);

    public void editContactName(int uid);
}
