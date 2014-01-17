package org.telegram.android.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.extradea.framework.images.tasks.FileSystemImageTask;
import com.extradea.framework.images.tasks.UriImageTask;
import com.extradea.framework.images.ui.FastWebImageView;
import org.telegram.android.MediaReceiverFragment;
import org.telegram.android.R;
import org.telegram.android.core.UserSourceListener;
import org.telegram.android.core.background.AvatarUploader;
import org.telegram.android.core.model.PeerType;
import org.telegram.android.core.model.file.AbsFileSource;
import org.telegram.android.core.model.file.FileSource;
import org.telegram.android.core.model.file.FileUriSource;
import org.telegram.android.core.model.media.TLLocalAvatarEmpty;
import org.telegram.android.core.model.media.TLLocalAvatarPhoto;
import org.telegram.android.core.model.media.TLLocalFileLocation;
import org.telegram.android.log.Logger;
import org.telegram.android.core.model.User;
import org.telegram.android.media.StelsImageTask;
import org.telegram.android.tasks.AsyncAction;
import org.telegram.android.tasks.AsyncException;
import org.telegram.android.ui.Placeholders;
import org.telegram.android.ui.TextUtil;
import org.telegram.api.*;
import org.telegram.api.requests.TLRequestAuthLogOut;
import org.telegram.api.requests.TLRequestAuthResetAuthorizations;
import org.telegram.api.requests.TLRequestPhotosUpdateProfilePhoto;

import java.io.*;

/**
 * Author: Korshakov Stepan
 * Created: 31.07.13 4:48
 */
public class SettingsFragment extends MediaReceiverFragment implements UserSourceListener, AvatarUploader.AvatarUserUploadListener {

    private FastWebImageView avatar;
    private TextView nameView;
    private TextView phoneView;
    private View avatarUploadView;
    private View avatarUploadError;
    private View avatarUploadProgress;

    private int debugClickCount = 0;
    private long lastDebugClickTime = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View res = inflater.inflate(R.layout.settings_main, container, false);
        avatarUploadView = res.findViewById(R.id.avatarUploadProgress);
        avatarUploadProgress = res.findViewById(R.id.uploadProgressBar);
        avatarUploadError = res.findViewById(R.id.uploadError);

        avatarUploadView.setVisibility(View.GONE);
        avatarUploadProgress.setVisibility(View.GONE);
        avatarUploadError.setVisibility(View.GONE);

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

        res.findViewById(R.id.logoutButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runUiTask(new AsyncAction() {
                    @Override
                    public void execute() throws AsyncException {
                        rpc(new TLRequestAuthLogOut());
                        application.getKernel().logOut();
                    }

                    @Override
                    public void afterExecute() {
                        getRootController().onLogout();
                    }
                });
            }
        });
        res.findViewById(R.id.deleteAccount).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://telegram.org/deactivate")));
            }
        });
        res.findViewById(R.id.changeAvatar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int state = application.getSyncKernel().getAvatarUploader().getAvatarUploadState();
                if (state == AvatarUploader.STATE_ERROR) {
                    AlertDialog dialog = new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.st_avatar_change_error_title)
                            .setMessage(R.string.st_avatar_change_error_message)
                            .setPositiveButton(R.string.st_try_again, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    application.getSyncKernel().getAvatarUploader().tryAgainUploadAvatar();
                                }
                            })
                            .setNegativeButton(R.string.st_cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    application.getSyncKernel().getAvatarUploader().cancelUploadAvatar();
                                }
                            }).create();
                    dialog.setCanceledOnTouchOutside(true);
                    dialog.show();
                } else {
                    User user = application.getEngine().getUser(application.getCurrentUid());
                    if (user != null && user.getPhoto() instanceof TLLocalAvatarPhoto) {
                        requestPhotoChooserWithDelete(0);
                    } else {
                        requestPhotoChooser(0);
                    }
                }
            }
        });
        res.findViewById(R.id.chatSettings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getRootController().openChatSettings();
            }
        });
        res.findViewById(R.id.resetSessions).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runUiTask(new AsyncAction() {
                    @Override
                    public void execute() throws AsyncException {
                        rpc(new TLRequestAuthResetAuthorizations());
                    }

                    @Override
                    public void afterExecute() {
                        Toast.makeText(getActivity(), R.string.st_settings_reset_toast, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        res.findViewById(R.id.support).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getRootController().openDialog(PeerType.PEER_USER, 333000);
            }
        });
        res.findViewById(R.id.googlePlay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://play.google.com/store/apps/details?id=" + application.getPackageName())));
            }
        });
        res.findViewById(R.id.twitter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getStringSafe(R.string.st_lang).equals("ar")) {
                    startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://twitter.com/telegram_arabic")));
                } else {
                    startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://twitter.com/telegram")));
                }
            }
        });
        res.findViewById(R.id.faq).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://telegram.org/faq")));
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

        avatar = (FastWebImageView) res.findViewById(R.id.avatar);
        avatar.setLoadingDrawable(Placeholders.getUserPlaceholder(application.getCurrentUid()));

        nameView = (TextView) res.findViewById(R.id.userName);
        phoneView = (TextView) res.findViewById(R.id.phone);

        if (application.isRTL()) {
            nameView.setGravity(Gravity.RIGHT);
            phoneView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        } else {
            nameView.setGravity(Gravity.LEFT);
            phoneView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        }

        return res;
    }

    @Override
    public void onResume() {
        super.onResume();
        application.getUserSource().registerListener(this);
        application.getSyncKernel().getAvatarUploader().setListener(this);
        updateUser();
    }

    private void updateUser() {
        User user = application.getEngine().getUser(application.getCurrentUid());
        if (user != null) {

            boolean isLoaded = false;

            int state = application.getSyncKernel().getAvatarUploader().getAvatarUploadState();
            if (state != AvatarUploader.STATE_NONE) {
                AbsFileSource fileSource = application.getSyncKernel().getAvatarUploader().getAvatarUploadingSource();
                if (fileSource != null) {
                    if (fileSource instanceof FileSource) {
                        avatar.requestTaskSwitch(new FileSystemImageTask(((FileSource) fileSource).getFileName()));
                        showView(avatarUploadView);
                        isLoaded = true;
                    } else if (fileSource instanceof FileUriSource) {
                        avatar.requestTaskSwitch(new UriImageTask(((FileUriSource) fileSource).getUri()));
                        showView(avatarUploadView);
                        isLoaded = true;
                    }
                }
                if (isLoaded) {
                    if (state == AvatarUploader.STATE_ERROR) {
                        showView(avatarUploadError, false);
                        hideView(avatarUploadProgress, false);
                    } else {
                        hideView(avatarUploadError, false);
                        showView(avatarUploadProgress, false);
                    }
                }
            }

            if (!isLoaded) {
                hideView(avatarUploadView);
                if (user.getPhoto() instanceof TLLocalAvatarPhoto) {
                    TLLocalAvatarPhoto photo = (TLLocalAvatarPhoto) user.getPhoto();
                    if (photo.getPreviewLocation() instanceof TLLocalFileLocation) {
                        avatar.requestTaskSwitch(new StelsImageTask((TLLocalFileLocation) photo.getPreviewLocation()));
                    } else {
                        avatar.requestTaskSwitch(null);
                    }
                } else {
                    avatar.requestTaskSwitch(null);
                }
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
        application.getSyncKernel().getAvatarUploader().setListener(null);
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
        if (requestId == 0) {
            if (cropSupported(uri)) {
                requestCrop(uri, 200, 200, 0);
            } else {
                doUploadAvatar(null, uri);
            }
        }
    }

    @Override
    protected void onPhotoArrived(final String fileName, int width, int height, int requestId) {
        if (requestId == 0) {
            if (cropSupported(Uri.fromFile(new File(fileName)))) {
                requestCrop(fileName, 200, 200, 0);
            } else {
                doUploadAvatar(fileName, null);
            }
        }
    }

    @Override
    protected void onPhotoCropped(Uri uri, int requestId) {
        if (requestId == 0) {
            doUploadAvatar(null, uri);
        }
    }

    @Override
    protected void onPhotoDeleted(int requestId) {
        if (requestId == 0) {
            doUploadAvatar(null, null);
        }
    }

    private void doUploadAvatar(final String fileName, final Uri uri) {
        if (fileName != null || uri != null) {
            if (uri != null) {
                application.getSyncKernel().getAvatarUploader().uploadAvatar(new FileUriSource(uri.toString()));
            } else {
                application.getSyncKernel().getAvatarUploader().uploadAvatar(new FileSource(fileName));
            }
        } else {
            runUiTask(new AsyncAction() {
                @Override
                public void execute() throws AsyncException {
                    TLAbsUserProfilePhoto photo = rpc(new TLRequestPhotosUpdateProfilePhoto(new TLInputPhotoEmpty(), new TLInputPhotoCropAuto()));
                    if (photo instanceof TLUserProfilePhoto) {
                        TLUserProfilePhoto profilePhoto = (TLUserProfilePhoto) photo;
                        TLFileLocation smallLocation = (TLFileLocation) profilePhoto.getPhotoSmall();
                        TLFileLocation largeLocation = (TLFileLocation) profilePhoto.getPhotoBig();
                        TLLocalAvatarPhoto localProfilePhoto = new TLLocalAvatarPhoto();
                        localProfilePhoto.setPreviewLocation(new TLLocalFileLocation(smallLocation.getDcId(), smallLocation.getVolumeId(), smallLocation.getLocalId(), smallLocation.getSecret(), 0));
                        localProfilePhoto.setFullLocation(new TLLocalFileLocation(largeLocation.getDcId(), largeLocation.getVolumeId(), largeLocation.getLocalId(), largeLocation.getSecret(), 0));
                        application.getEngine().getUsersEngine().onUserPhotoChanges(application.getCurrentUid(), localProfilePhoto);
                    } else {
                        application.getEngine().getUsersEngine().onUserPhotoChanges(application.getCurrentUid(), new TLLocalAvatarEmpty());
                    }
                }

                @Override
                public void afterExecute() {
                    Toast.makeText(getActivity(), R.string.st_avatar_removed, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSherlockActivity().getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText(R.string.st_settings_title));
        getSherlockActivity().getSupportActionBar().setSubtitle(null);
    }

    @Override
    public void onUsersChanged(User[] users) {
        for (User u : users) {
            if (u.getUid() == application.getCurrentUid()) {
                updateUser();
                return;
            }
        }
    }

    @Override
    public void onAvatarUploadingStateChanged() {
        secureCallback(new Runnable() {
            @Override
            public void run() {
                updateUser();
            }
        });
    }
}

