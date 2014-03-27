package org.telegram.android.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import org.telegram.android.base.TelegramFragment;
import org.telegram.android.config.NotificationSettings;

/**
 * Author: Korshakov Stepan
 * Created: 16.08.13 15:04
 */
public class SettingsNottificationsFragment extends TelegramFragment {

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
    private TextView lightLabel;
    private TextView lightTitle;
    private View lightContainer;

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
    private TextView groupLightLabel;
    private TextView groupLightTitle;
    private View groupLightContainer;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View res = wrap(inflater).inflate(R.layout.settings_notifications, container, false);
        mainContainer = res.findViewById(R.id.mainContainer);

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
        lightContainer = res.findViewById(R.id.ledContainer);
        lightLabel = (TextView) res.findViewById(R.id.ledLabel);
        lightTitle = (TextView) res.findViewById(R.id.ledTitle);

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
        groupLightContainer = res.findViewById(R.id.ledGroupContainer);
        groupLightLabel = (TextView) res.findViewById(R.id.ledGroupLabel);
        groupLightTitle = (TextView) res.findViewById(R.id.ledGroupTitle);

        inAppSoundLabel = (TextView) res.findViewById(R.id.inAppSoundsLabel);
        inAppSoundsCheck = (ImageView) res.findViewById(R.id.inAppSoundsCheck);
        inAppSoundContainer = res.findViewById(R.id.inAppSoundsContainer);

        inAppVibrateLabel = (TextView) res.findViewById(R.id.inAppVibrateLabel);
        inAppVibrateCheck = (ImageView) res.findViewById(R.id.inAppVibrateCheck);
        inAppVibrateContainer = res.findViewById(R.id.inAppVibrateContainer);

        inAppPreviewLabel = (TextView) res.findViewById(R.id.inAppPreviewLabel);
        inAppPreviewCheck = (ImageView) res.findViewById(R.id.inAppPreviewCheck);
        inAppPreviewContainer = res.findViewById(R.id.inAppPreviewContainer);

        res.findViewById(R.id.allAlertContainer).setOnClickListener(secure(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.getNotificationSettings().setEnabled(!application.getNotificationSettings().isEnabled());
                updateUi();
            }
        }));

        allVibrationContainer.setOnClickListener(secure(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.getNotificationSettings().setMessageVibrationEnabled(!application.getNotificationSettings().isMessageVibrationEnabled());
                updateUi();
            }
        }));

        allSoundContainer.setOnClickListener(secure(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.getNotificationSettings().setMessageSoundEnabled(!application.getNotificationSettings().isMessageSoundEnabled());
                updateUi();
            }
        }));


        allCustomSoundContainer.setOnClickListener(secure(new View.OnClickListener() {
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
        }));

        groupAlertContainer.setOnClickListener(secure(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.getNotificationSettings().setGroupEnabled(!application.getNotificationSettings().isGroupEnabled());
                updateUi();
            }
        }));

        groupVibrationContainer.setOnClickListener(secure(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.getNotificationSettings().setGroupVibrateEnabled(!application.getNotificationSettings().isGroupVibrateEnabled());
                updateUi();
            }
        }));

        groupSoundContainer.setOnClickListener(secure(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.getNotificationSettings().setGroupSoundEnabled(!application.getNotificationSettings().isGroupSoundEnabled());
                updateUi();
            }
        }));

        groupCustomSoundContainer.setOnClickListener(secure(new View.OnClickListener() {
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
        }));

        inAppSoundContainer.setOnClickListener(secure(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.getNotificationSettings().setInAppSoundsEnabled(!application.getNotificationSettings().isInAppSoundsEnabled());
                updateUi();
            }
        }));

        inAppVibrateContainer.setOnClickListener(secure(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.getNotificationSettings().setInAppVibrateEnabled(!application.getNotificationSettings().isInAppVibrateEnabled());
                updateUi();
            }
        }));

        inAppPreviewContainer.setOnClickListener(secure(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.getNotificationSettings().setInAppPreviewEnabled(!application.getNotificationSettings().isInAppPreviewEnabled());
                updateUi();
            }
        }));

        res.findViewById(R.id.resetNotifications).setOnClickListener(secure(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                application.getNotificationSettings().resetGroupSettings();
                Toast.makeText(getActivity(), R.string.st_notifications_reset_success, Toast.LENGTH_SHORT).show();
            }
        }));

        lightContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int[] ids = new int[]{
                        NotificationSettings.LED_NONE,
                        NotificationSettings.LED_COLORFUL,
                        NotificationSettings.LED_WHITE,
                        NotificationSettings.LED_RED,
                        NotificationSettings.LED_GREEN,
                        NotificationSettings.LED_BLUE,
                        NotificationSettings.LED_YELLOW,
                        NotificationSettings.LED_ORANGE,
                        NotificationSettings.LED_PINK,
                        NotificationSettings.LED_PURPLE,
                        NotificationSettings.LED_CYAN,
                };
                final CharSequence[] items = new CharSequence[]{
                        getStringSafe(R.string.st_none),
                        getStringSafe(R.string.st_notifications_led_colorful),
                        getStringSafe(R.string.st_notifications_led_white),
                        getStringSafe(R.string.st_notifications_led_red),
                        getStringSafe(R.string.st_notifications_led_green),
                        getStringSafe(R.string.st_notifications_led_blue),
                        getStringSafe(R.string.st_notifications_led_yellow),
                        getStringSafe(R.string.st_notifications_led_orange),
                        getStringSafe(R.string.st_notifications_led_pink),
                        getStringSafe(R.string.st_notifications_led_purple),
                        getStringSafe(R.string.st_notifications_led_cyan),
                };
                AlertDialog dialog = new AlertDialog.Builder(getActivity())
                        .setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                application.getNotificationSettings().setLedMode(ids[which]);
                                updateUi();
                            }
                        }).create();
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
            }
        });

        groupLightContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int[] ids = new int[]{
                        NotificationSettings.LED_NONE,
                        NotificationSettings.LED_DEFAULT,
                        NotificationSettings.LED_COLORFUL,
                        NotificationSettings.LED_WHITE,
                        NotificationSettings.LED_RED,
                        NotificationSettings.LED_GREEN,
                        NotificationSettings.LED_BLUE,
                        NotificationSettings.LED_YELLOW,
                        NotificationSettings.LED_ORANGE,
                        NotificationSettings.LED_PINK,
                        NotificationSettings.LED_PURPLE,
                        NotificationSettings.LED_CYAN,
                };
                final CharSequence[] items = new CharSequence[]{
                        getStringSafe(R.string.st_none),
                        getStringSafe(R.string.st_default),
                        getStringSafe(R.string.st_notifications_led_colorful),
                        getStringSafe(R.string.st_notifications_led_white),
                        getStringSafe(R.string.st_notifications_led_red),
                        getStringSafe(R.string.st_notifications_led_green),
                        getStringSafe(R.string.st_notifications_led_blue),
                        getStringSafe(R.string.st_notifications_led_yellow),
                        getStringSafe(R.string.st_notifications_led_orange),
                        getStringSafe(R.string.st_notifications_led_pink),
                        getStringSafe(R.string.st_notifications_led_purple),
                        getStringSafe(R.string.st_notifications_led_cyan),
                };
                AlertDialog dialog = new AlertDialog.Builder(getActivity())
                        .setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                application.getNotificationSettings().setLedGroupMode(ids[which]);
                                updateUi();
                            }
                        }).create();
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
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
            allVibrationLabel.setTextColor(getResources().getColor(R.color.st_section_title));
            allVibrationContainer.setEnabled(true);
            allSoundLabel.setTextColor(getResources().getColor(R.color.st_section_title));
            allSoundContainer.setEnabled(true);
            allCustomSoundLabel.setTextColor(getResources().getColor(R.color.st_section_title));
            allCustomSoundTitle.setTextColor(getResources().getColor(R.color.st_section_value));
            allCustomSoundContainer.setEnabled(true);
            lightLabel.setTextColor(getResources().getColor(R.color.st_section_title));
            lightTitle.setTextColor(getResources().getColor(R.color.st_section_value));
            lightContainer.setEnabled(true);
        } else {
            allAlertCheck.setImageResource(R.drawable.holo_btn_check_off);
            allVibrationLabel.setTextColor(getResources().getColor(R.color.st_section_title_disabled));
            allVibrationContainer.setEnabled(false);
            allSoundLabel.setTextColor(getResources().getColor(R.color.st_section_title_disabled));
            allSoundContainer.setEnabled(false);
            allCustomSoundLabel.setTextColor(getResources().getColor(R.color.st_section_title_disabled));
            allCustomSoundTitle.setTextColor(getResources().getColor(R.color.st_section_value_disabled));
            allCustomSoundContainer.setEnabled(false);
            lightLabel.setTextColor(getResources().getColor(R.color.st_section_title_disabled));
            lightTitle.setTextColor(getResources().getColor(R.color.st_section_value_disabled));
            lightContainer.setEnabled(false);
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

        lightTitle.setText(getLedModeString(application.getNotificationSettings().getLedMode()));
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
            groupAlertLabel.setTextColor(getResources().getColor(R.color.st_section_title));
            groupAlertContainer.setEnabled(true);
        } else {
            groupAlertLabel.setTextColor(getResources().getColor(R.color.st_section_title_disabled));
            groupAlertContainer.setEnabled(false);
        }

        if (isEnabled) {
            groupVibrationLabel.setTextColor(getResources().getColor(R.color.st_section_title));
            groupVibrationContainer.setEnabled(true);
            groupSoundLabel.setTextColor(getResources().getColor(R.color.st_section_title));
            groupSoundContainer.setEnabled(true);
            groupCustomSoundLabel.setTextColor(getResources().getColor(R.color.st_section_title));
            groupCustomSoundTitle.setTextColor(getResources().getColor(R.color.st_section_value));
            groupCustomSoundContainer.setEnabled(true);
            groupLightLabel.setTextColor(getResources().getColor(R.color.st_section_title));
            groupLightTitle.setTextColor(getResources().getColor(R.color.st_section_value));
            groupLightContainer.setEnabled(true);
        } else {
            groupVibrationLabel.setTextColor(getResources().getColor(R.color.st_section_title_disabled));
            groupVibrationContainer.setEnabled(false);
            groupSoundLabel.setTextColor(getResources().getColor(R.color.st_section_title_disabled));
            groupSoundContainer.setEnabled(false);
            groupCustomSoundLabel.setTextColor(getResources().getColor(R.color.st_section_title_disabled));
            groupCustomSoundTitle.setTextColor(getResources().getColor(R.color.st_section_value_disabled));
            groupCustomSoundContainer.setEnabled(false);
            groupLightLabel.setTextColor(getResources().getColor(R.color.st_section_title_disabled));
            groupLightTitle.setTextColor(getResources().getColor(R.color.st_section_value_disabled));
            groupLightContainer.setEnabled(false);
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

        groupLightTitle.setText(getLedModeString(application.getNotificationSettings().getLedGroupMode()));
    }

    private String getLedModeString(int mode) {
        switch (mode) {
            case NotificationSettings.LED_COLORFUL:
            default:
                return getStringSafe(R.string.st_notifications_led_colorful);
            case NotificationSettings.LED_DEFAULT:
                return getStringSafe(R.string.st_default);
            case NotificationSettings.LED_NONE:
                return getStringSafe(R.string.st_none);
            case NotificationSettings.LED_BLUE:
                return getStringSafe(R.string.st_notifications_led_blue);
            case NotificationSettings.LED_CYAN:
                return getStringSafe(R.string.st_notifications_led_cyan);
            case NotificationSettings.LED_GREEN:
                return getStringSafe(R.string.st_notifications_led_green);
            case NotificationSettings.LED_ORANGE:
                return getStringSafe(R.string.st_notifications_led_orange);
            case NotificationSettings.LED_PINK:
                return getStringSafe(R.string.st_notifications_led_pink);
            case NotificationSettings.LED_PURPLE:
                return getStringSafe(R.string.st_notifications_led_purple);
            case NotificationSettings.LED_RED:
                return getStringSafe(R.string.st_notifications_led_red);
            case NotificationSettings.LED_WHITE:
                return getStringSafe(R.string.st_notifications_led_white);
            case NotificationSettings.LED_YELLOW:
                return getStringSafe(R.string.st_notifications_led_yellow);
        }
    }

    private void updateInAppUi() {

        boolean isEnabled = application.getNotificationSettings().isEnabled();
        if (isEnabled) {
            inAppSoundLabel.setTextColor(getResources().getColor(R.color.st_section_title));
            inAppSoundContainer.setEnabled(true);
            inAppVibrateLabel.setTextColor(getResources().getColor(R.color.st_section_title));
            inAppVibrateContainer.setEnabled(true);
            inAppPreviewLabel.setTextColor(getResources().getColor(R.color.st_section_title));
            inAppPreviewContainer.setEnabled(true);
        } else {
            inAppSoundLabel.setTextColor(getResources().getColor(R.color.st_section_title_disabled));
            inAppSoundContainer.setEnabled(false);
            inAppVibrateLabel.setTextColor(getResources().getColor(R.color.st_section_title_disabled));
            inAppVibrateContainer.setEnabled(false);
            inAppPreviewLabel.setTextColor(getResources().getColor(R.color.st_section_title_disabled));
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
    public void onDestroyView() {
        super.onDestroyView();
        allAlertCheck = null;
        allCustomSoundTitle = null;
    }
}
