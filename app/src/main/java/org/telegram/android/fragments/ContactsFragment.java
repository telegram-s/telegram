package org.telegram.android.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import org.telegram.android.R;
import org.telegram.android.core.ContactsSource;
import org.telegram.android.core.model.Contact;
import org.telegram.android.core.model.LinkType;
import org.telegram.android.core.model.update.TLLocalAffectedHistory;
import org.telegram.android.core.wireframes.ContactWireframe;
import org.telegram.android.fragments.common.BaseContactsFragment;
import org.telegram.android.tasks.AsyncAction;
import org.telegram.android.tasks.AsyncException;
import org.telegram.android.ui.pick.PickIntentClickListener;
import org.telegram.android.ui.pick.PickIntentDialog;
import org.telegram.android.ui.pick.PickIntentItem;
import org.telegram.api.TLAbsInputUser;
import org.telegram.api.TLInputPeerContact;
import org.telegram.api.TLInputUserContact;
import org.telegram.api.messages.TLAffectedHistory;
import org.telegram.api.requests.TLRequestContactsBlock;
import org.telegram.api.requests.TLRequestContactsDeleteContacts;
import org.telegram.api.requests.TLRequestMessagesDeleteHistory;
import org.telegram.tl.TLVector;

/**
 * Author: Korshakov Stepan
 * Created: 30.07.13 11:33
 */
public class ContactsFragment extends BaseContactsFragment {
    private View share;

    @Override
    public boolean isSaveInStack() {
        return false;
    }

    @Override
    protected void onFilterChanged() {
        if (share != null) {
            if (isFiltering()) {
                share.findViewById(R.id.container).setVisibility(View.GONE);
            } else {
                share.findViewById(R.id.container).setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        getSherlockActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSherlockActivity().getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSherlockActivity().getSupportActionBar().setTitle(highlightTitleText(R.string.st_contacts_title));
        getSherlockActivity().getSupportActionBar().setSubtitle(null);

        if (!isLoaded()) {
            return;
        }

        inflater.inflate(R.menu.contacts_menu, menu);

        menu.findItem(R.id.systemContacts).setTitle(highlightMenuText(R.string.st_contacts_menu_book));
        menu.findItem(R.id.addContact).setTitle(highlightMenuText(R.string.st_contacts_menu_add_contact));
        menu.findItem(R.id.allContacts).setTitle(highlightMenuText(R.string.st_contacts_menu_all_contacts));
        menu.findItem(R.id.onlyTContacts).setTitle(highlightMenuText(R.string.st_contacts_menu_only_t));

        MenuItem searchItem = menu.findItem(R.id.searchMenu);

        com.actionbarsherlock.widget.SearchView searchView = (com.actionbarsherlock.widget.SearchView) searchItem.getActionView();
        // searchView.setQueryHint(getStringSafe(R.string.st_contacts_filter));
        searchView.setQueryHint("");

        ((ImageView) searchView.findViewById(R.id.abs__search_button)).setImageResource(R.drawable.st_bar_ic_search);

        searchView.setOnSuggestionListener(new com.actionbarsherlock.widget.SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                return false;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                return false;
            }
        });

        searchView.setOnQueryTextListener(new com.actionbarsherlock.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                doFilter(newText);
                return true;
            }
        });

        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                doFilter(null);
                return true;
            }
        });

        if (application.getUserSettings().showOnlyTelegramContacts()) {
            menu.findItem(R.id.allContacts).setVisible(true);
            menu.findItem(R.id.onlyTContacts).setVisible(false);
        } else {
            menu.findItem(R.id.allContacts).setVisible(false);
            menu.findItem(R.id.onlyTContacts).setVisible(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.onlyTContacts) {
            application.getUserSettings().setShowOnlyTelegramContacts(true);
            getSherlockActivity().invalidateOptionsMenu();
            reloadData();
            return true;
        } else if (item.getItemId() == R.id.allContacts) {
            application.getUserSettings().setShowOnlyTelegramContacts(false);
            getSherlockActivity().invalidateOptionsMenu();
            reloadData();
            return true;
        } else if (item.getItemId() == R.id.systemContacts) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
            startActivity(intent);
        } else if (item.getItemId() == R.id.addContact) {
            Intent intent = new Intent(Intent.ACTION_INSERT);
            intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected boolean showOnlyTelegramContacts() {
        return application.getUserSettings().showOnlyTelegramContacts();
    }

    @Override
    protected int getLayout() {
        return R.layout.contacts_list;
    }

    @Override
    protected void onCreateView(View res, LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        share = inflater.inflate(R.layout.contacts_header_share, null);
        getListView().addHeaderView(share);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        if (i == 0) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, getStringSafe(R.string.st_invite_short));

            PickIntentItem[] pickIntentItems = createPickIntents(shareIntent);
            PickIntentDialog dialog = new PickIntentDialog(getActivity(), pickIntentItems, new PickIntentClickListener() {
                @Override
                public void onItemClicked(int index, PickIntentItem item) {
                    sendEvent("share_pressed", item.getIntent().getComponent().getPackageName());
                    startActivity(item.getIntent());
                }
            });
            dialog.setTitle("Share by...");
            dialog.show();
        } else {
            // TODO: Correct fix
            final ContactWireframe contact = (ContactWireframe) adapterView.getItemAtPosition(i);
            // final Contact[] contacts = application.getEngine().getUsersEngine().getContactsForLocalId(contact.getContactId());
            if (contact.getRelatedUsers().length == 1) {
                getRootController().openUser(contact.getRelatedUsers()[0].getUid());
            } else if (contact.getRelatedUsers().length > 0) {
                CharSequence[] names = new CharSequence[contact.getRelatedUsers().length];
                for (int j = 0; j < names.length; j++) {
                    names[j] = application.getEngine().getUser(contact.getRelatedUsers()[j].getUid()).getDisplayName();
                }
                AlertDialog dialog = new AlertDialog.Builder(getActivity())
                        .setItems(names, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                getRootController().openUser(contact.getRelatedUsers()[i].getUid());
                            }
                        }).create();
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
            } else {
                AlertDialog dialog = new AlertDialog.Builder(getActivity()).setTitle(R.string.st_contacts_not_registered_title)
                        .setMessage(getStringSafe(R.string.st_contacts_not_registered_message).replace("{0}", contact.getDisplayName()))
                        .setPositiveButton(R.string.st_yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                try {
                                    Cursor pCur = application.getContentResolver().query(
                                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                            null,
                                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                            new String[]{contact.getContactId() + ""}, null);

                                    if (pCur.moveToFirst()) {
                                        String phoneNo = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                                        Intent sendSms = new Intent(Intent.ACTION_VIEW, Uri.parse("tel:" + phoneNo));
                                        sendSms.putExtra("address", phoneNo);
                                        sendSms.putExtra("sms_body", getStringSafe(R.string.st_invite_short));
                                        sendSms.setType("vnd.android-dir/mms-sms");
                                        startActivity(sendSms);
                                    } else {
                                        Context context1 = getActivity();
                                        if (context1 != null) {
                                            Toast.makeText(context1, R.string.st_error_unable_sms, Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                } catch (Exception e) {
                                    Context context1 = getActivity();
                                    if (context1 != null) {
                                        Toast.makeText(context1, R.string.st_error_unable_sms, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        }).setNegativeButton(R.string.st_no, null).create();
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
            }
        }
    }

    @Override
    public boolean onItemLongClick(final AdapterView<?> adapterView, View view, final int i, long l) {
        if (i == 0) {
            return false;
        }

        final ContactWireframe contact = (ContactWireframe) adapterView.getItemAtPosition(i);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(contact.getDisplayName());

        if (contact.getRelatedUsers().length > 0) {
            builder.setItems(new CharSequence[]{
                    getStringSafe(R.string.st_contacts_action_view),
                    getStringSafe(R.string.st_contacts_action_share),
                    getStringSafe(R.string.st_contacts_action_delete),
                    getStringSafe(R.string.st_contacts_action_block),
                    getStringSafe(R.string.st_contacts_action_block_and_delete)
            }, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (i == 0) {
                        viewInBookContact(contact);
                    } else if (i == 1) {
                        shareContact(contact);
                    } else if (i == 2) {
                        deleteContact(contact);
                    } else if (i == 3) {
                        blockContact(contact);
                    } else if (i == 4) {
                        blockDeleteContact(contact);
                    }
                }
            });
        } else {
            builder.setItems(new CharSequence[]{
                    getStringSafe(R.string.st_contacts_action_view),
                    getStringSafe(R.string.st_contacts_action_delete),
            }, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (i == 0) {
                        viewInBookContact(contact);
                    } else if (i == 1) {
                        deleteContact(contact);
                    }
                }
            });
        }
        AlertDialog contextMenu = builder.create();
        contextMenu.setCanceledOnTouchOutside(true);
        contextMenu.show();

        return true;
    }

    private void viewInBookContact(final ContactWireframe contact) {
        openUri(Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contact.getContactId() + ""));
    }

    private void shareContact(final ContactWireframe contact) {
        getRootController().shareContact(contact.getRelatedUsers()[0].getUid());
    }

    private void deleteContact(final ContactWireframe contact) {
        AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setMessage(getStringSafe(R.string.st_contacts_delete).replace("{0}", contact.getDisplayName()))
                .setPositiveButton(R.string.st_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int b) {
                        final Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, contact.getLookupKey());
                        final Contact[] contacts = application.getEngine().getUsersEngine().getContactsForLocalId(contact.getContactId());
                        if (contacts.length > 0) {
                            runUiTask(new AsyncAction() {
                                @Override
                                public void execute() throws AsyncException {
                                    TLVector<TLAbsInputUser> inputUsers = new TLVector<TLAbsInputUser>();
                                    for (Contact c : contacts) {
                                        inputUsers.add(new TLInputUserContact(c.getUid()));
                                    }
                                    rpc(new TLRequestContactsDeleteContacts(inputUsers));

                                    for (Contact c : contacts) {
                                        application.getEngine().getUsersEngine().onUserLinkChanged(c.getUid(), LinkType.REQUEST);
                                    }

                                    application.getContentResolver().delete(uri, null, null);
                                    application.getSyncKernel().getContactsSync().removeContact(contact.getContactId());
                                }

                                @Override
                                public void afterExecute() {
                                    reloadData();
                                }
                            });
                        } else {
                            application.getContentResolver().delete(uri, null, null);
                            application.getSyncKernel().getContactsSync().removeContact(contact.getContactId());
                            reloadData();
                        }
                    }
                }).setNegativeButton(R.string.st_no, null).create();
        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.show();
    }

    private void blockContact(final ContactWireframe contact) {
        AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setMessage(getStringSafe(R.string.st_contacts_block).replace("{0}", contact.getDisplayName()))
                .setPositiveButton(R.string.st_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        runUiTask(new AsyncAction() {
                            @Override
                            public void execute() throws AsyncException {
                                rpc(new TLRequestContactsBlock(new TLInputUserContact(contact.getRelatedUsers()[0].getUid())));
                            }
                        });
                    }
                }).setNegativeButton(R.string.st_no, null).create();
        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.show();
    }

    private void blockDeleteContact(final ContactWireframe contact) {
        AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setMessage(getStringSafe(R.string.st_contacts_block_delete).replace("{0}", contact.getDisplayName()))
                .setPositiveButton(R.string.st_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        runUiTask(new AsyncAction() {
                            @Override
                            public void execute() throws AsyncException {
                                TLAffectedHistory tlAffectedHistory = rpc(new TLRequestMessagesDeleteHistory(new TLInputPeerContact(contact.getRelatedUsers()[0].getUid()), 0));
                                application.getUpdateProcessor().onMessage(new TLLocalAffectedHistory(tlAffectedHistory));
                                while (tlAffectedHistory.getOffset() > 0) {
                                    tlAffectedHistory = rpc(new TLRequestMessagesDeleteHistory(new TLInputPeerContact(contact.getRelatedUsers()[0].getUid()), tlAffectedHistory.getOffset()));
                                    application.getUpdateProcessor().onMessage(new TLLocalAffectedHistory(tlAffectedHistory));
                                }
                                rpc(new TLRequestContactsBlock(new TLInputUserContact(contact.getRelatedUsers()[0].getUid())));

                                Contact[] contacts = application.getEngine().getUsersEngine().getContactsForLocalId(contact.getContactId());

                                TLVector<TLAbsInputUser> inputUsers = new TLVector<TLAbsInputUser>();
                                for (Contact c : contacts) {
                                    inputUsers.add(new TLInputUserContact(c.getUid()));
                                }
                                rpc(new TLRequestContactsDeleteContacts(inputUsers));

                                for (Contact c : contacts) {
                                    application.getEngine().getUsersEngine().onUserLinkChanged(c.getUid(), LinkType.REQUEST);
                                }

                                final Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, contact.getLookupKey());
                                application.getContentResolver().delete(uri, null, null);
                                application.getSyncKernel().getContactsSync().removeContact(contact.getContactId());
                            }

                            @Override
                            public void afterExecute() {
                                reloadData();
                            }
                        });
                    }
                }).setNegativeButton(R.string.st_no, null).create();
        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.show();
    }

}
