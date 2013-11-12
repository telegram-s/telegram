package org.telegram.android.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import com.crittercism.app.Crittercism;
import com.j256.ormlite.stmt.QueryBuilder;
import org.telegram.android.Configuration;
import org.telegram.android.StelsApplication;
import org.telegram.android.core.model.ContentType;
import org.telegram.android.core.model.DialogDescription;
import org.telegram.android.core.model.PeerType;
import org.telegram.android.core.model.User;
import org.telegram.android.cursors.ViewSource;
import org.telegram.android.cursors.ViewSourceState;
import org.telegram.android.log.Logger;
import org.telegram.android.ui.TextUtil;
import org.telegram.api.TLAbsMessageAction;
import org.telegram.api.TLMessageActionChatAddUser;
import org.telegram.api.TLMessageActionChatDeleteUser;
import org.telegram.api.messages.TLAbsDialogs;
import org.telegram.api.messages.TLDialogs;
import org.telegram.api.messages.TLDialogsSlice;
import org.telegram.api.requests.TLRequestMessagesGetDialogs;
import org.telegram.tl.TLObject;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Author: Korshakov Stepan
 * Created: 28.07.13 21:22
 */
public class DialogSource {

    private static final String TAG = "DialogsSource";

    public static void clearData(StelsApplication application) {
        SharedPreferences preferences = application.getSharedPreferences("org.telegram.android.Dialogs", Context.MODE_PRIVATE);
        preferences.edit().remove("state").commit();
    }

    public static final int PAGE_SIZE = 25;

    public static final int PAGE_OVERLAP = 3;

    public static final int PAGE_REQUEST_PADDING = 20;

    private ExecutorService service = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread res = new Thread(runnable);
            res.setName("DialogSourceService#" + res.hashCode());
            res.setPriority(Configuration.SOURCES_THREAD_PRIORITY);
            return res;
        }
    });

    private final Object stateSync = new Object();
    private DialogSourceState state;

    private StelsApplication application;

    private ViewSource<DialogDescription> dialogsSource;

    private SharedPreferences preferences;

    private boolean isDestroyed;

    public DialogSource(StelsApplication _application) {
        this.application = _application;
        this.isDestroyed = false;
        this.preferences = this.application.getSharedPreferences("org.telegram.android.Dialogs", Context.MODE_PRIVATE);
        this.state = DialogSourceState.valueOf(preferences.getString("state", DialogSourceState.UNSYNCED.name()));

        if (this.state == DialogSourceState.FULL_SYNC) {
            this.state = DialogSourceState.UNSYNCED;
        } else if (this.state == DialogSourceState.LOAD_MORE || this.state == DialogSourceState.LOAD_MORE_ERROR) {
            this.state = DialogSourceState.SYNCED;
        }

        this.dialogsSource = new ViewSource<DialogDescription>() {
            @Override
            protected DialogDescription[] loadItems(int offset) {
                if (offset < PAGE_OVERLAP) {
                    offset = 0;
                } else {
                    offset -= PAGE_OVERLAP;
                }

                if (offset == 0) {
                    TextUtil.formatPhone("+75555555555");
                }

                try {
                    QueryBuilder<DialogDescription, Long> queryBuilder = application.getEngine().getDialogsDao().queryBuilder();
                    queryBuilder.orderBy("date", false);
                    queryBuilder.where().ne("date", 0);
                    queryBuilder.offset(offset);
                    queryBuilder.limit(PAGE_SIZE);
                    List<DialogDescription> dialogDescriptions = application.getEngine().getDialogsDao().query(queryBuilder.prepare());
                    DialogDescription[] res = dialogDescriptions.toArray(new DialogDescription[dialogDescriptions.size()]);
                    HashSet<Integer> uids = new HashSet<Integer>();
                    for (int i = 0; i < res.length; i++) {
                        if (res[i].getPeerType() == PeerType.PEER_USER) {
                            uids.add(res[i].getPeerId());
                        }
                        if (res[i].getContentType() == ContentType.MESSAGE_SYSTEM) {
                            TLObject object = res[i].getExtras();
                            if (object != null && object instanceof TLAbsMessageAction) {
                                if (object instanceof TLMessageActionChatAddUser) {
                                    uids.add(((TLMessageActionChatAddUser) object).getUserId());
                                } else if (object instanceof TLMessageActionChatDeleteUser) {
                                    uids.add(((TLMessageActionChatDeleteUser) object).getUserId());
                                }
                            }
                        }
                    }
                    User[] users = application.getEngine().getUsersById(uids.toArray());
                    for (Integer uid : uids) {
                        boolean founded = false;
                        for (User user : users) {
                            if (user.getUid() == uid) {
                                founded = true;
                                break;
                            }
                        }
                        if (!founded) {
                            application.dropLogin();
                            Logger.d(TAG, "Missed user: " + uid);
                            Crittercism.logHandledException(new RuntimeException("Reset by missed user: " + uid));
                            return new DialogDescription[0];
                        }
                    }
                    return res;
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return new DialogDescription[0];
            }

            @Override
            protected long getSortingKey(DialogDescription obj) {
                return obj.getDate();
            }

            @Override
            protected long getItemKey(DialogDescription obj) {
                return obj.getDatabaseId();
            }

            @Override
            protected ViewSourceState getInternalState() {
                switch (state) {
                    default:
                    case UNSYNCED:
                        return ViewSourceState.UNSYNCED;
                    case COMPLETED:
                        return ViewSourceState.COMPLETED;
                    case LOAD_MORE_ERROR:
                        return ViewSourceState.LOAD_MORE_ERROR;
                    case LOAD_MORE:
                        return ViewSourceState.IN_PROGRESS;
                    case FULL_SYNC:
                        return ViewSourceState.IN_PROGRESS;
                    case SYNCED:
                        return ViewSourceState.SYNCED;
                }
            }

            @Override
            protected void onItemRequested(int index) {
                if (index > getItemsCount() - PAGE_REQUEST_PADDING) {
                    requestLoadMore(getItemsCount());
                }
            }
        };
    }

    public void destroy() {
        isDestroyed = true;
        service.shutdownNow();
    }

    public ViewSource<DialogDescription> getViewSource() {
        return dialogsSource;
    }

    public DialogSourceState getState() {
        return state;
    }

    private void setState(DialogSourceState nState) {
        if (isDestroyed)
            return;
        state = nState;
        Logger.d(TAG, "Dialogs state: " + nState);
        preferences.edit().putString("state", state.name()).commit();
        dialogsSource.invalidateState();
    }

    public void startSyncIfRequired() {
        if (state == DialogSourceState.UNSYNCED) {
            startSync();
        }
    }

    public void resetSync() {
        setState(DialogSourceState.UNSYNCED);
    }

    public void startSync() {
        if (isDestroyed) {
            return;
        }
        synchronized (stateSync) {
            if (state != DialogSourceState.FULL_SYNC && state != DialogSourceState.LOAD_MORE) {
                setState(DialogSourceState.FULL_SYNC);
                service.execute(new Runnable() {
                    @Override
                    public void run() {
                        long start = SystemClock.uptimeMillis();
                        try {
                            boolean isCompleted = false;
                            try {
                                while (application.isLoggedIn()) {
                                    try {
                                        isCompleted = requestLoad(0);
                                        dialogsSource.invalidateDataIfRequired();
                                        // notifyDataChanged();
                                        return;
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            } catch (Exception e) {
                                Logger.t(TAG, e);
                            } finally {
                                synchronized (stateSync) {
                                    if (state == DialogSourceState.FULL_SYNC) {
                                        if (isCompleted) {
                                            setState(DialogSourceState.COMPLETED);
                                        } else {
                                            setState(DialogSourceState.SYNCED);
                                        }
                                    }
                                }
                            }

                            setState(DialogSourceState.UNSYNCED);
                        } finally {
                            Logger.d(TAG, "Dialogs full sync time: " + (SystemClock.uptimeMillis() - start) + " ms");
                        }
                    }
                });
            }
        }
    }

    public void requestLoadMore(final int offset) {
        if (isDestroyed) {
            return;
        }
        synchronized (stateSync) {
            if (state != DialogSourceState.FULL_SYNC && state != DialogSourceState.LOAD_MORE
                    && state != DialogSourceState.COMPLETED) {
                setState(DialogSourceState.LOAD_MORE);
                service.execute(new Runnable() {
                    @Override
                    public void run() {
                        long start = SystemClock.uptimeMillis();
                        try {
                            try {
                                boolean isCompleted = requestLoad(offset);
                                dialogsSource.invalidateDataIfRequired();
                                synchronized (stateSync) {
                                    if (state == DialogSourceState.LOAD_MORE) {
                                        if (isCompleted) {
                                            setState(DialogSourceState.COMPLETED);
                                        } else {
                                            setState(DialogSourceState.SYNCED);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                Logger.t(TAG, e);
                                synchronized (stateSync) {
                                    if (state == DialogSourceState.LOAD_MORE) {
                                        setState(DialogSourceState.LOAD_MORE_ERROR);
                                    }
                                }
                            }
                        } finally {
                            Logger.d(TAG, "Dialogs load more time: " + (SystemClock.uptimeMillis() - start) + " ms");
                        }
                    }
                });
            }
        }
    }

    public boolean isCompleted() {
        return state == DialogSourceState.COMPLETED;
    }

    private boolean requestLoad(int offset) throws Exception {
        if (isDestroyed) {
            return true;
        }
        application.getMonitor().waitForConnection();
        if (isDestroyed) {
            return true;
        }
        TLAbsDialogs dialogs = application.getApi().doRpcCall(new TLRequestMessagesGetDialogs(offset, 0, PAGE_SIZE));
        if (isDestroyed) {
            return true;
        }

        long start = SystemClock.uptimeMillis();
        application.getEngine().onLoadMoreDialogs(
                dialogs.getMessages(),
                dialogs.getUsers(),
                dialogs.getChats(),
                dialogs.getDialogs());
        Logger.d(TAG, "Dialog apply time: " + (SystemClock.uptimeMillis() - start) + " ms");

        if (dialogs instanceof TLDialogs) {
            return true;
        } else if (dialogs instanceof TLDialogsSlice) {
            return dialogs.getMessages().size() == 0 || ((TLDialogsSlice) dialogs).getCount() <= offset + PAGE_SIZE;
        } else {
            return true;
        }
    }
}
