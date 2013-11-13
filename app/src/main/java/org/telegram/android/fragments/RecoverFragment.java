package org.telegram.android.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import org.telegram.android.R;
import org.telegram.android.StelsFragment;
import org.telegram.android.tasks.AsyncAction;
import org.telegram.android.tasks.AsyncException;
import org.telegram.android.tasks.ProgressInterface;
import org.telegram.api.TLAbsInputUser;
import org.telegram.api.TLAbsUser;
import org.telegram.api.TLInputUserSelf;
import org.telegram.api.engine.RpcException;
import org.telegram.api.requests.TLRequestUsersGetUsers;
import org.telegram.tl.TLVector;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 17.09.13
 * Time: 20:18
 */
public class RecoverFragment extends StelsFragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View res = inflater.inflate(R.layout.recover, container, false);
        setDefaultProgressInterface(new ProgressInterface() {
            @Override
            public void showContent() {
            }

            @Override
            public void hideContent() {
            }

            @Override
            public void showProgress() {
            }

            @Override
            public void hideProgress() {
            }
        });

        runUiTask(new AsyncAction() {
            @Override
            public void execute() throws AsyncException {
                TLVector<TLAbsInputUser> users = new TLVector<TLAbsInputUser>();
                users.add(new TLInputUserSelf());
                try {
                    TLVector<TLAbsUser> response = application.getApi().doRpcCall(new TLRequestUsersGetUsers(users));
                    application.getEngine().onUsers(response);
                } catch (RpcException e) {
                    e.printStackTrace();
                    if (application.getEngine().getUser(application.getCurrentUid()) != null) {
                        return;
                    }
                    throw new AsyncException(e);
                } catch (IOException e) {
                    if (application.getEngine().getUser(application.getCurrentUid()) != null) {
                        return;
                    }
                    throw new AsyncException(AsyncException.ExceptionType.CONNECTION_ERROR);
                }
            }

            @Override
            public void afterExecute() {
                getRootController().onRecovered();
            }
        });
        return res;
    }
}
