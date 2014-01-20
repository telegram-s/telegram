package org.telegram.android.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import org.telegram.android.R;
import org.telegram.android.base.TelegramFragment;
import org.telegram.android.log.Logger;

import java.io.*;

/**
 * Author: Korshakov Stepan
 * Created: 10.09.13 23:27
 */
public class DebugFragment extends TelegramFragment {

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText("Debug"));
        getSherlockActivity().getSupportActionBar().setSubtitle(null);
        getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSherlockActivity().getSupportActionBar().setDisplayShowHomeEnabled(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View res = inflater.inflate(R.layout.settings_debug, container, false);
        res.findViewById(R.id.resetContactsSync).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                application.getSyncKernel().getContactsSync().invalidateContactsSync();
            }
        });
        res.findViewById(R.id.resetDcSync).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                application.getSyncKernel().getBackgroundSync().resetDcSync();
            }
        });
//        res.findViewById(R.id.phoneBook).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                runUiTask(new AsyncAction() {
//                    @Override
//                    public void execute() throws AsyncException {
//                        ContactsSource.PhoneBookRecord[] records = application.getContactsSource().loadPhoneBook();
//                        String phoneBook = "START\n";
//                        for (ContactsSource.PhoneBookRecord record : records) {
//                            phoneBook += record.getContactId() + ":" + record.getFirstName() + "/" + record.getLastName() + "\n";
//                            for (ContactsSource.Phone phone : record.getPhones()) {
//                                phoneBook += "PHONE:" + phone.getId() + ":" + phone.getNumber() + "\n";
//                            }
//                        }
//                        phoneBook += "END";
//                        try {
//                            FileOutputStream stream = new FileOutputStream(Environment.getExternalStorageDirectory().toString() + "/phone_book_export.txt");
//                            stream.write(phoneBook.getBytes());
//                            stream.close();
//                        } catch (FileNotFoundException e) {
//                            e.printStackTrace();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
//
//                    @Override
//                    public void afterExecute() {
//                        String fileName = Environment.getExternalStorageDirectory().toString() + "/phone_book_export.txt";
//                        startActivity(new Intent(Intent.ACTION_SEND).setDataAndType(Uri.fromFile(new File(fileName)), "text/plain"));
//                    }
//                });
//            }
//        });

        CheckBox forceAnim = (CheckBox) res.findViewById(R.id.pageAnimations);
        forceAnim.setChecked(application.getTechKernel().getDebugSettings().isForceAnimations());
        forceAnim.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                application.getTechKernel().getDebugSettings().setForceAnimations(b);
            }
        });

        CheckBox saveLogs = (CheckBox) res.findViewById(R.id.enableLogging);
        saveLogs.setChecked(application.getTechKernel().getDebugSettings().isSaveLogs());
        saveLogs.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                application.getTechKernel().getDebugSettings().setSaveLogs(b);
                if (b) {
                    Logger.enableDiskLog();
                } else {
                    Logger.disableDiskLog();
                }
            }
        });

        res.findViewById(R.id.exportLogs).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String file = Logger.exportLogs();
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(file)));
                intent.setType("*/*");
                startActivity(intent);
            }
        });

        res.findViewById(R.id.clearLogs).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.clearLogs();
            }
        });

        Spinner spinner = (Spinner) res.findViewById(R.id.dialogListLayer);
        spinner.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_dropdown_item_1line, new String[]
                {
                        "Default", "None", "Hardware", "Software"
                }));
        spinner.setSelection(application.getTechKernel().getDebugSettings().getDialogListLayerType());
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                application.getTechKernel().getDebugSettings().setDialogListLayerType(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        spinner = (Spinner) res.findViewById(R.id.dialogListItemLayer);
        spinner.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_dropdown_item_1line, new String[]
                {
                        "Default", "None", "Hardware", "Software"
                }));
        spinner.setSelection(application.getTechKernel().getDebugSettings().getDialogListItemLayerType());
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                application.getTechKernel().getDebugSettings().setDialogListItemLayerType(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        spinner = (Spinner) res.findViewById(R.id.convListLayer);
        spinner.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_dropdown_item_1line, new String[]
                {
                        "Default", "None", "Hardware", "Software"
                }));
        spinner.setSelection(application.getTechKernel().getDebugSettings().getConversationListLayerType());
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                application.getTechKernel().getDebugSettings().setConversationListLayerType(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        spinner = (Spinner) res.findViewById(R.id.convListItemLayer);
        spinner.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_dropdown_item_1line, new String[]
                {
                        "Default", "None", "Hardware", "Software"
                }));
        spinner.setSelection(application.getTechKernel().getDebugSettings().getConversationListItemLayerType());
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                application.getTechKernel().getDebugSettings().setConversationListItemLayerType(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        return res;
    }
}
