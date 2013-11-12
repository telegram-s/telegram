package org.telegram.android.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import org.telegram.android.R;
import org.telegram.android.StelsFragment;

/**
 * Author: Korshakov Stepan
 * Created: 16.08.13 15:04
 */
public class NotificationSettingsFragment extends StelsFragment {

    private static final int REQUEST_PICK_SOUND_ALL = 1;
    private static final int REQUEST_PICK_SOUND_GROUP = 2;

    private View mainContainer;

    private ImageView allAlertCheck;
    private TextView allVibrationLabel;
    private ImageView allVibrationCheck;
    private View allVibrationContainer;
    private TextView allSoundLabel;
    private ImageView allSoundCheck;
    private View allSoundContainer;
    private TextView allCustomSoundLabel;
    private TextView allCustomSoundTitle;
    private View allCustomSoundContainer;

    private TextView groupAlertLabel;
    private ImageView groupAlertCheck;
    private View groupAlertContainer;
    private TextView groupVibrationLabel;
    private ImageView groupVibrationCheck;
    private View groupVibrationContainer;
    private TextView groupSoundLabel;
    private ImageView groupSoundCheck;
    private View groupSoundContainer;
    private TextView groupCustomSoundLabel;
    private TextView groupCustomSoundTitle;
    private View groupCustomSoundContainer;

    private TextView inAppSoundLabel;
    private ImageView inAppSoundsCheck;
    private View inAppSoundContainer;

    private TextView inAppVibrateLabel;
    private ImageView inAppVibrateCheck;
    private View inAppVibrateContainer;

    private TextView inAppPreviewLabel;
    private ImageView inAppPreviewCheck;
    private View inAppPreviewContainer;

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
        View res = inflater.inflate(R.layout.settings_notifications, container, false);
        mainContainer = res.findViewById(R.id.mainContainer);
        updateHeaderPadding();

        allAlertCheck = (ImageView) res.findViewById(R.id.allAlertCheck);
        allVibrationLabel = (TextView) res.findViewById(R.id.allVibrationLabel);
        allVibrationCheck = (ImageView) res.findViewById(R.id.allVibrationCheck);
        allVibrationContainer = res.findViewById(R.id.allVibrationContainer);
        allSoundLabel = (TextView) res.findViewById(R.id.allSoundLabel);
        allSoundContainer = res.findViewById(R.id.allSoundContainer);
        allSoundCheck = (ImageView) res.findViewById(R.id.allSoundCheck);
        allCustomSoundLabel = (TextView) res.findViewById(R.id.allCustomSoundLabel);
        allCustomSoundTitle = (TextView) res.findViewById(R.id.allSoundTitle);
        allCustomSoundContainer = res.findViewById(R.id.allCustomSoundContainer);

        groupAlertContainer = res.findViewById(R.id.groupAlertContainer);
        groupAlertLabel = (TextView) res.findViewById(R.id.groupAlertLabel);
        groupAlertCheck = (ImageView) res.findViewById(R.id.groupAlertCheck);
        groupVibrationLabel = (TextView) res.findViewById(R.id.groupVibrateLabel);
        groupVibrationCheck = (ImageView) res.findViewById(R.id.groupVibrateCheck);
        groupVibrationContainer = res.findViewById(R.id.groupVibrateContainer);
        groupSoundLabel = (TextView) res.findViewById(R.id.groupSoundLabel);
        groupSoundCheck = (ImageView) res.findViewById(R.id.groupSoundCheck);
        groupSoundContainer = res.findViewById(R.id.groupSoundContainer);
        groupCustomSoundLabel = (TextView) res.findViewById(R.id.groupCustomSoundLabel);
        groupCustomSoundTitle = (TextView) res.findViewById(R.id.groupCustomSoundTitle);
        groupCustomSoundContainer = res.findViewById(R.id.groupCustomSoundContainer);

        inAppSoundLabel = (TextView) res.findViewById(R.id.inAppSoundsLabel);
        inAppSoundsCheck = (ImageView) res.findViewById(R.id.inAppSoundsCheck);
        inAppSoundContainer = res.findViewById(R.id.inAppSoundsContainer);

        inAppVibrateLabel = (TextView) res.findViewById(R.id.inAppVibrateLabel);
        inAppVibrateCheck = (ImageView) res.findViewById(R.id.inAppVibrateCheck);
        inAppVibrateContainer = res.findViewById(R.id.inAppVibrateContainer);

        inAppPreviewLabel = (TextView) res.findViewById(R.id.inAppPreviewLabel);
        inAppPreviewCheck = (ImageView) res.findViewById(R.id.inAppPreviewCheck);
        inAppPreviewContainer = res.findViewById(R.id.inAppPreviewContainer);

        res.findViewById(R.id.allAlertContainer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.getNotificationSettings().setEnabled(!application.getNotificationSettings().isEnabled());
                updateUi();
            }
        });

        allVibrationContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.getNotificationSettings().setMessageVibrationEnabled(!application.getNotificationSettings().isMessageVibrationEnabled());
                updateUi();
            }
        });

        allSoundContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.getNotificationSettings().setMessageSoundEnabled(!application.getNotificationSettings().isMessageSoundEnabled());
                updateUi();
            }
        });


        allCustomSoundContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getStringSafe(R.string.st_select_tone));
                if (application.getNotificationSettings().getNotificationSound() != null) {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                            Uri.parse(application.getNotificationSettings().getNotificationSound()));
                } else {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
                }
                startActivityForResult(intent, REQUEST_PICK_SOUND_ALL);
            }
        });

        groupAlertContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.getNotificationSettings().setGroupEnabled(!application.getNotificationSettings().isGroupEnabled());
                updateUi();
            }
        });

        groupVibrationContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.getNotificationSettings().setGroupVibrateEnabled(!application.getNotificationSettings().isGroupVibrateEnabled());
                updateUi();
            }
        });

        groupSoundContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.getNotificationSettings().setGroupSoundEnabled(!application.getNotificationSettings().isGroupSoundEnabled());
                updateUi();
            }
        });

        groupCustomSoundContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getStringSafe(R.string.st_select_tone));
                if (application.getNotificationSettings().getNotificationGroupSound() != null) {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                            Uri.parse(application.getNotificationSettings().getNotificationGroupSound()));
                } else {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
                }
                startActivityForResult(intent, REQUEST_PICK_SOUND_GROUP);
            }
        });

        inAppSoundContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.getNotificationSettings().setInAppSoundsEnabled(!application.getNotificationSettings().isInAppSoundsEnabled());
                updateUi();
            }
        });

        inAppVibrateContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.getNotificationSettings().setInAppVibrateEnabled(!application.getNotificationSettings().isInAppVibrateEnabled());
                updateUi();
            }
        });

        inAppPreviewContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.getNotificationSettings().setInAppPreviewEnabled(!application.getNotificationSettings().isInAppPreviewEnabled());
                updateUi();
            }
        });

        res.findViewById(R.id.resetNotifications).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.getNotificationSettings().resetGroupSettings();
                Toast.makeText(getActivity(), R.string.st_notifications_reset_success, Toast.LENGTH_SHORT).show();
            }
        });

        updateUi();
        return res;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_SOUND_ALL && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null) {
                Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), uri);
                String title = ringtone.getTitle(getActivity());
                application.getNotificationSettings().setNotificationSound(uri.toString(), title);
            } else {
                application.getNotificationSettings().setNotificationSound(null, null);
            }
            updateUi();
        }

        if (requestCode == REQUEST_PICK_SOUND_GROUP && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null) {
                Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), uri);
                String title = ringtone.getTitle(getActivity());
                application.getNotificationSettings().setNotificationGroupSound(uri.toString(), title);
            } else {
                application.getNotificationSettings().setNotificationGroupSound(null, null);
            }
            updateUi();
        }
    }

    private void updateMessagesUi() {
        boolean isAllEnabled = application.getNotificationSettings().isEnabled();

        if (isAllEnabled) {
            allAlertCheck.setImageResource(R.drawable.holo_btn_check_on);
            allVibrationLabel.setTextColor(0xFF010101);
            allVibrationContainer.setEnabled(true);
            allSoundLabel.setTextColor(0xFF010101);
            allSoundContainer.setEnabled(true);
            allCustomSoundLabel.setTextColor(0xFF010101);
            allCustomSoundTitle.setTextColor(0xff006FC8);
            allCustomSoundContainer.setEnabled(true);
        } else {
            allAlertCheck.setImageResource(R.drawable.holo_btn_check_off);
            allVibrationLabel.setTextColor(0x883D3D3D);
            allVibrationContainer.setEnabled(false);
            allSoundLabel.setTextColor(0x883D3D3D);
            allSoundContainer.setEnabled(false);
            allCustomSoundLabel.setTextColor(0x883D3D3D);
            allCustomSoundTitle.setTextColor(0x883D3D3D);
            allCustomSoundContainer.setEnabled(false);
        }

        if (application.getNotificationSettings().isMessageVibrationEnabled()) {
            if (!isAllEnabled) {
                allVibrationCheck.setImageResource(R.drawable.holo_btn_check_on_disabled);
            } else {
                allVibrationCheck.setImageResource(R.drawable.holo_btn_check_on);
            }
        } else {
            if (!isAllEnabled) {
                allVibrationCheck.setImageResource(R.drawable.holo_btn_check_off_disabled);
            } else {
                allVibrationCheck.setImageResource(R.drawable.holo_btn_check_off);
            }
        }

        if (application.getNotificationSettings().isMessageSoundEnabled()) {
            if (!isAllEnabled) {
                allSoundCheck.setImageResource(R.drawable.holo_btn_check_on_disabled);
            } else {
                allSoundCheck.setImageResource(R.drawable.holo_btn_check_on);
            }
        } else {
            if (!isAllEnabled) {
                allSoundCheck.setImageResource(R.drawable.holo_btn_check_off_disabled);
            } else {
                allSoundCheck.setImageResource(R.drawable.holo_btn_check_off);
            }
        }

        if (application.getNotificationSettings().getNotificationSound() != null) {
            allCustomSoundTitle.setText(application.getNotificationSettings().getNotificationSoundTitle());
        } else {
            allCustomSoundTitle.setText(R.string.st_default);
        }
    }

    private void updateGroupUi() {
        boolean isEnabled = application.getNotificationSettings().isGroupEnabled() && application.getNotificationSettings().isEnabled();

        if (application.getNotificationSettings().isGroupEnabled()) {
            if (isEnabled) {
                groupAlertCheck.setImageResource(R.drawable.holo_btn_check_on);
            } else {
                groupAlertCheck.setImageResource(R.drawable.holo_btn_check_on_disabled);
            }
        } else {
            if (isEnabled) {
                groupAlertCheck.setImageResource(R.drawable.holo_btn_check_off);
            } else {
                groupAlertCheck.setImageResource(R.drawable.holo_btn_check_off_disabled);
            }
        }

        if (application.getNotificationSettings().isEnabled()) {
            groupAlertLabel.setTextColor(0xFF010101);
            groupAlertContainer.setEnabled(true);
        } else {
            groupAlertLabel.setTextColor(0x883D3D3D);
            groupAlertContainer.setEnabled(false);
        }

        if (isEnabled) {
            groupVibrationLabel.setTextColor(0xFF010101);
            groupVibrationContainer.setEnabled(true);
            groupSoundLabel.setTextColor(0xFF010101);
            groupSoundContainer.setEnabled(true);
            groupCustomSoundLabel.setTextColor(0xFF010101);
            groupCustomSoundTitle.setTextColor(0xff006FC8);
            groupCustomSoundContainer.setEnabled(true);
        } else {
            groupVibrationLabel.setTextColor(0x883D3D3D);
            groupVibrationContainer.setEnabled(false);
            groupSoundLabel.setTextColor(0x883D3D3D);
            groupSoundContainer.setEnabled(false);
            groupCustomSoundLabel.setTextColor(0x883D3D3D);
            groupCustomSoundTitle.setTextColor(0x883D3D3D);
            groupCustomSoundContainer.setEnabled(false);
        }

        if (application.getNotificationSettings().isGroupVibrateEnabled()) {
            if (isEnabled) {
                groupVibrationCheck.setImageResource(R.drawable.holo_btn_check_on);
            } else {
                groupVibrationCheck.setImageResource(R.drawable.holo_btn_check_on_disabled);
            }
        } else {
            if (isEnabled) {
                groupVibrationCheck.setImageResource(R.drawable.holo_btn_check_off);
            } else {
                groupVibrationCheck.setImageResource(R.drawable.holo_btn_check_off_disabled);
            }
        }

        if (application.getNotificationSettings().isGroupSoundEnabled()) {
            if (isEnabled) {
                groupSoundCheck.setImageResource(R.drawable.holo_btn_check_on);
            } else {
                groupSoundCheck.setImageResource(R.drawable.holo_btn_check_on_disabled);
            }
        } else {
            if (isEnabled) {
                groupSoundCheck.setImageResource(R.drawable.holo_btn_check_off);
            } else {
                groupSoundCheck.setImageResource(R.drawable.holo_btn_check_off_disabled);
            }
        }

        if (application.getNotificationSettings().getNotificationGroupSound() != null) {
            groupCustomSoundTitle.setText(application.getNotificationSettings().getNotificationSoundGroupTitle());
        } else {
            groupCustomSoundTitle.setText(R.string.st_default);
        }
    }

    private void updateInAppUi() {

        boolean isEnabled = application.getNotificationSettings().isEnabled();
        if (isEnabled) {
            inAppSoundLabel.setTextColor(0xFF010101);
            inAppSoundContainer.setEnabled(true);
            inAppVibrateLabel.setTextColor(0xFF010101);
            inAppVibrateContainer.setEnabled(true);
            inAppPreviewLabel.setTextColor(0xFF010101);
            inAppPreviewContainer.setEnabled(true);
        } else {
            inAppSoundLabel.setTextColor(0x883D3D3D);
            inAppSoundContainer.setEnabled(false);
            inAppVibrateLabel.setTextColor(0x883D3D3D);
            inAppVibrateContainer.setEnabled(false);
            inAppPreviewLabel.setTextColor(0x883D3D3D);
            inAppPreviewContainer.setEnabled(false);
        }

        if (application.getNotificationSettings().isInAppSoundsEnabled()) {
            if (isEnabled) {
                inAppSoundsCheck.setImageResource(R.drawable.holo_btn_check_on);
            } else {
                inAppSoundsCheck.setImageResource(R.drawable.holo_btn_check_on_disabled);
            }
        } else {
            if (isEnabled) {
                inAppSoundsCheck.setImageResource(R.drawable.holo_btn_check_off);
            } else {
                inAppSoundsCheck.setImageResource(R.drawable.holo_btn_check_off_disabled);
            }
        }

        if (application.getNotificationSettings().isInAppVibrateEnabled()) {
            if (isEnabled) {
                inAppVibrateCheck.setImageResource(R.drawable.holo_btn_check_on);
            } else {
                inAppVibrateCheck.setImageResource(R.drawable.holo_btn_check_on_disabled);
            }
        } else {
            if (isEnabled) {
                inAppVibrateCheck.setImageResource(R.drawable.holo_btn_check_off);
            } else {
                inAppVibrateCheck.setImageResource(R.drawable.holo_btn_check_off_disabled);
            }
        }

        if (application.getNotificationSettings().isInAppPreviewEnabled()) {
            if (isEnabled) {
                inAppPreviewCheck.setImageResource(R.drawable.holo_btn_check_on);
            } else {
                inAppPreviewCheck.setImageResource(R.drawable.holo_btn_check_on_disabled);
            }
        } else {
            if (isEnabled) {
                inAppPreviewCheck.setImageResource(R.drawable.holo_btn_check_off);
            } else {
                inAppPreviewCheck.setImageResource(R.drawable.holo_btn_check_off_disabled);
            }
        }
    }

    private void updateUi() {
        updateMessagesUi();
        updateGroupUi();
        updateInAppUi();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSherlockActivity().getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText(R.string.st_notifications_title));
        getSherlockActivity().getSupportActionBar().setSubtitle(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateHeaderPadding();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        allAlertCheck = null;
        allCustomSoundTitle = null;
    }
}
