package org.telegram.android.fragments;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.extradea.framework.images.ui.FastWebImageView;
import org.telegram.android.MediaReceiverFragment;
import org.telegram.android.R;
import org.telegram.android.core.UserSourceListener;
import org.telegram.android.core.files.UploadResult;
import org.telegram.android.core.model.PeerType;
import org.telegram.android.core.model.media.TLLocalAvatarEmpty;
import org.telegram.android.core.model.media.TLLocalAvatarPhoto;
import org.telegram.android.core.model.media.TLLocalFileLocation;
import org.telegram.android.log.Logger;
import org.telegram.android.media.Optimizer;
import org.telegram.android.core.model.User;
import org.telegram.android.media.StelsImageTask;
import org.telegram.android.tasks.AsyncAction;
import org.telegram.android.tasks.AsyncException;
import org.telegram.android.ui.TextUtil;
import org.telegram.api.*;
import org.telegram.api.photos.TLPhoto;
import org.telegram.api.requests.TLRequestAuthLogOut;
import org.telegram.api.requests.TLRequestPhotosUpdateProfilePhoto;
import org.telegram.api.requests.TLRequestPhotosUploadProfilePhoto;

import java.io.*;
import java.util.Random;

/**
 * Author: Korshakov Stepan
 * Created: 31.07.13 4:48
 */
public class SettingsFragment extends MediaReceiverFragment implements UserSourceListener {

    private FastWebImageView avatar;
    private TextView nameView;
    private TextView phoneView;
    private ImageView galleryCheck;
    private View mainContainer;

    private int debugClickCount = 0;
    private long lastDebugClickTime = 0;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
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
        View res = inflater.inflate(R.layout.settings_main, container, false);
        mainContainer = res.findViewById(R.id.mainContainer);
        updateHeaderPadding();
        galleryCheck = (ImageView) res.findViewById(R.id.saveToGalleryCheck);

        if (application.getUserSettings().isSaveToGalleryEnabled()) {
            galleryCheck.setImageResource(R.drawable.holo_btn_check_on);
        } else {
            galleryCheck.setImageResource(R.drawable.holo_btn_check_off);
        }
        TextView textView = (TextView) res.findViewById(R.id.version);
        PackageInfo pInfo = null;
        try {
            pInfo = application.getPackageManager().getPackageInfo(application.getPackageName(), 0);
            textView.setText(unicodeWrap(pInfo.versionName));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            textView.setText("Unknown");
        }

        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (debugClickCount == 0 || SystemClock.uptimeMillis() > lastDebugClickTime + 1000) {
                    lastDebugClickTime = SystemClock.uptimeMillis();
                    debugClickCount = 1;
                } else {
                    lastDebugClickTime = SystemClock.uptimeMillis();
                    debugClickCount++;
                    if (debugClickCount > 6) {
                        debugClickCount = 0;

                        application.getTechKernel().getDebugSettings().setDeveloperMode(true);
                        application.getTechKernel().getDebugSettings().setSaveLogs(true);
                        Logger.enableDiskLog();
                        Toast.makeText(application, "Enabling developer settings", Toast.LENGTH_SHORT).show();
                        getView().findViewById(R.id.developmentButton).setVisibility(View.VISIBLE);
                        getView().findViewById(R.id.developmentDiv).setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        res.findViewById(R.id.savePhotos).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (application.getUserSettings().isSaveToGalleryEnabled()) {
                    application.getUserSettings().setSaveToGalleryEnabled(false);
                    galleryCheck.setImageResource(R.drawable.holo_btn_check_off);
                } else {
                    application.getUserSettings().setSaveToGalleryEnabled(true);
                    galleryCheck.setImageResource(R.drawable.holo_btn_check_on);
                }
            }
        });

        res.findViewById(R.id.logoutButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runUiTask(new AsyncAction() {
                    @Override
                    public void execute() throws AsyncException {
                        rpc(new TLRequestAuthLogOut());
                        application.clearLoginState();
                    }

                    @Override
                    public void afterExecute() {
                        getRootController().onLogout();
                    }
                });
            }
        });
        res.findViewById(R.id.changeAvatar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                User user = application.getEngine().getUser(application.getCurrentUid());
                if (user != null && user.getPhoto() instanceof TLLocalAvatarPhoto) {
                    requestPhotoChooserWithDelete(0);
                } else {
                    requestPhotoChooser(0);
                }
            }
        });
        res.findViewById(R.id.support).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getRootController().openDialog(PeerType.PEER_USER, 333000);
            }
        });
        res.findViewById(R.id.notifications).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getRootController().openNotificationSettings();
            }
        });
        res.findViewById(R.id.blocked).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getRootController().openBlocked();
            }
        });
        res.findViewById(R.id.editName).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getRootController().openNameSettings();
            }
        });
        res.findViewById(R.id.wallpapers).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getRootController().openWallpaperSettings();
            }
        });

        res.findViewById(R.id.developmentButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getRootController().openDebugSettings();
            }
        });

        if (application.getTechKernel().getDebugSettings().isDeveloperMode()) {
            res.findViewById(R.id.developmentButton).setVisibility(View.VISIBLE);
            res.findViewById(R.id.developmentDiv).setVisibility(View.VISIBLE);
        } else {
            res.findViewById(R.id.developmentButton).setVisibility(View.GONE);
            res.findViewById(R.id.developmentDiv).setVisibility(View.GONE);
        }

        if (isSlow()) {
            res.findViewById(R.id.wallpapers).setVisibility(View.GONE);
            res.findViewById(R.id.wallpapersDiv).setVisibility(View.GONE);
        } else {
            res.findViewById(R.id.wallpapers).setVisibility(View.VISIBLE);
            res.findViewById(R.id.wallpapersDiv).setVisibility(View.VISIBLE);
        }

        avatar = (FastWebImageView) res.findViewById(R.id.avatar);
        avatar.setLoadingDrawable(R.drawable.st_user_placeholder);

        nameView = (TextView) res.findViewById(R.id.userName);
        phoneView = (TextView) res.findViewById(R.id.phone);

        if (application.isRTL()) {
            nameView.setGravity(Gravity.RIGHT);
            phoneView.setGravity(Gravity.RIGHT);
        } else {
            nameView.setGravity(Gravity.LEFT);
            phoneView.setGravity(Gravity.LEFT);
        }

        return res;
    }

    @Override
    public void onResume() {
        super.onResume();
        application.getUserSource().registerListener(this);
        updateHeaderPadding();
        updateUser();
    }

    @Override
    public void onUserChanged(int uid, User user) {
        if (uid == application.getCurrentUid()) {
            updateUser();
        }
    }

    private void updateUser() {
        User user = application.getEngine().getUser(application.getCurrentUid());
        if (user != null) {
            if (user.getPhoto() instanceof TLLocalAvatarPhoto) {
                TLLocalAvatarPhoto photo = (TLLocalAvatarPhoto) user.getPhoto();
                if (photo.getPreviewLocation() instanceof TLLocalFileLocation) {
                    avatar.requestTask(new StelsImageTask((TLLocalFileLocation) photo.getPreviewLocation()));
                } else {
                    avatar.requestTask(null);
                }
            } else {
                avatar.requestTask(null);
            }

            nameView.setText(unicodeWrap(user.getDisplayName()));

            phoneView.setText(unicodeWrap(TextUtil.formatPhone(user.getPhone())));
        } else {
            avatar.requestTask(null);
            nameView.setText("Loading...");
            phoneView.setText("Loading...");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        application.getUserSource().unregisterListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        nameView = null;
        phoneView = null;
        avatar = null;
    }

    @Override
    protected void onPhotoArrived(Uri uri, int width, int height, int requestId) {
        if (cropSupported(uri)) {
            requestCrop(uri, 200, 200, 0);
        } else {
            doUploadAvatar(null, uri);
        }
    }

    @Override
    protected void onPhotoArrived(final String fileName, int width, int height, int requestId) {
        if (cropSupported(Uri.fromFile(new File(fileName)))) {
            requestCrop(fileName, 200, 200, 0);
        } else {
            doUploadAvatar(fileName, null);
        }
    }

    @Override
    protected void onPhotoCropped(Uri uri, int requestId) {
        doUploadAvatar(null, uri);
    }

    @Override
    protected void onPhotoDeleted(int requestId) {
        doUploadAvatar(null, null);
    }

    private void doUploadAvatar(final String fileName, final Uri uri) {
        runUiTask(new AsyncAction() {
            @Override
            public void execute() throws AsyncException {
                try {
                    if (fileName != null || uri != null) {
                        Random rnd = new Random();
                        long fileId = rnd.nextLong();

                        String destFile = getUploadTempFile();

                        if (uri != null) {
                            Optimizer.optimize(uri.toString(), getActivity(), destFile);
                        } else {
                            Optimizer.optimize(fileName, destFile);
                        }
                        File file = new File(destFile);
                        FileInputStream fis = new FileInputStream(file);
                        UploadResult res = application.getUploadController().uploadFile(fis, (int) file.length(), fileId);
                        if (res == null)
                            throw new AsyncException(AsyncException.ExceptionType.CONNECTION_ERROR);

                        TLPhoto photo = rpc(new TLRequestPhotosUploadProfilePhoto(
                                new TLInputFile(fileId, res.getPartsCount(), "photo.jpg", res.getHash()),
                                "MyPhoto", new TLInputGeoPointEmpty(), new TLInputPhotoCropAuto()));

                        if (photo.getPhoto() instanceof org.telegram.api.TLPhoto) {
                            org.telegram.api.TLPhoto srcPhoto = (org.telegram.api.TLPhoto) photo.getPhoto();
                            TLPhotoSize smallPhotoSize = (TLPhotoSize) findSmallest(srcPhoto);
                            TLFileLocation smallLocation = (TLFileLocation) smallPhotoSize.getLocation();
                            TLPhotoSize largePhotoSize = (TLPhotoSize) findLargest(srcPhoto);
                            TLFileLocation largeLocation = (TLFileLocation) largePhotoSize.getLocation();
                            TLLocalAvatarPhoto profilePhoto = new TLLocalAvatarPhoto();
                            profilePhoto.setPreviewLocation(new TLLocalFileLocation(smallLocation.getDcId(), smallLocation.getVolumeId(), smallLocation.getLocalId(), smallLocation.getSecret(), smallPhotoSize.getSize()));
                            profilePhoto.setPreviewLocation(new TLLocalFileLocation(largeLocation.getDcId(), largeLocation.getVolumeId(), largeLocation.getLocalId(), largeLocation.getSecret(), largePhotoSize.getSize()));
                            application.getEngine().onUserAvatarChanges(application.getCurrentUid(), profilePhoto);
                            application.getUserSource().notifyUserChanged(application.getCurrentUid());
                        }
                    } else {
                        rpc(new TLRequestPhotosUpdateProfilePhoto(new TLInputPhotoEmpty(), new TLInputPhotoCropAuto()));
                        application.getEngine().onUserAvatarChanges(application.getCurrentUid(), new TLLocalAvatarEmpty());
                        application.getUserSource().notifyUserChanged(application.getCurrentUid());
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new AsyncException(AsyncException.ExceptionType.UNKNOWN_ERROR);
                }
            }

            @Override
            public void afterExecute() {
                if (fileName != null || uri != null) {
                    Toast.makeText(getActivity(), R.string.st_avatar_changed, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), R.string.st_avatar_removed, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSherlockActivity().getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText(R.string.st_settings_title));
        getSherlockActivity().getSupportActionBar().setSubtitle(null);
    }
}

