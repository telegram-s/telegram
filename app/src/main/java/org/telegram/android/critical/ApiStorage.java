package org.telegram.android.critical;

import android.content.Context;
import org.telegram.android.core.model.TLLocalContext;
import org.telegram.android.core.model.storage.TLDcInfo;
import org.telegram.android.core.model.storage.TLKey;
import org.telegram.android.core.model.storage.TLLastKnownSalt;
import org.telegram.android.core.model.storage.TLStorage;
import org.telegram.api.TLConfig;
import org.telegram.api.TLDcOption;
import org.telegram.api.TLUserSelf;
import org.telegram.api.auth.TLAuthorization;
import org.telegram.api.engine.storage.AbsApiState;
import org.telegram.mtproto.state.AbsMTProtoState;
import org.telegram.mtproto.state.ConnectionInfo;
import org.telegram.mtproto.state.KnownSalt;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 09.11.13
 * Time: 0:41
 */
public class ApiStorage extends TLPersistence<TLStorage> implements AbsApiState {
    public ApiStorage(Context context) {
        super(context, "api.bin", TLStorage.class, TLLocalContext.getInstance());

        if (getObj().getDcInfos().size() == 0) {
            getObj().getDcInfos().add(new TLDcInfo(1, "173.240.5.1", 443));
        }
    }

    @Override
    public void write() {
        super.write();
    }

    private TLKey findKey(int dc) {
        for (TLKey key : getObj().getKeys()) {
            if (key.getDcId() == dc) {
                return key;
            }
        }
        return null;
    }

    private TLDcInfo findDc(int dc) {
        for (TLDcInfo key : getObj().getDcInfos()) {
            if (key.getDcId() == dc) {
                return key;
            }
        }
        return null;
    }

    public synchronized void switchToPrimaryDc(int dc) {
        getObj().setPrimaryDc(dc);
        write();
    }

    public synchronized boolean isAuthenticated() {
        return isAuthenticated(getPrimaryDc());
    }

    public synchronized void doAuth(TLAuthorization authorization) {
        TLKey key = findKey(getPrimaryDc());
        key.setAuthorised(true);
        getObj().setUid(authorization.getUser().getId());
        getObj().setPhone(((TLUserSelf) authorization.getUser()).getPhone());
        write();
    }

    @Override
    public synchronized int getPrimaryDc() {
        return getObj().getPrimaryDc();
    }

    @Override
    public synchronized boolean isAuthenticated(int dcId) {
        TLKey key = findKey(dcId);
        return (key != null && key.isAuthorised());
    }

    @Override
    public synchronized void setAuthenticated(int dcId, boolean auth) {
        TLKey key = findKey(dcId);
        key.setAuthorised(auth);
        write();
    }

    @Override
    public synchronized void updateSettings(TLConfig config) {
        getObj().getDcInfos().clear();
        for (TLDcOption option : config.getDcOptions()) {
            getObj().getDcInfos().add(new TLDcInfo(option.getId(), option.getIpAddress(), option.getPort()));
        }
        write();
    }

    @Override
    public synchronized byte[] getAuthKey(int dcId) {
        TLKey key = findKey(dcId);
        return key != null ? key.getAuthKey() : null;
    }

    @Override
    public synchronized void putAuthKey(int dcId, byte[] authKey) {
        TLKey key = findKey(dcId);
        if (key != null) {
            return;
        }
        getObj().getKeys().add(new TLKey(dcId, authKey));
        write();
    }

    @Override
    public synchronized ConnectionInfo getConnectionInfo(int dcId) {
        TLDcInfo info = findDc(dcId);
        return info != null ? new ConnectionInfo(info.getAddress(), info.getPort()) : null;
    }


    private synchronized void writeKnownSalts(int dcId, KnownSalt[] salts) {
        TLKey key = findKey(dcId);
        key.getSalts().clear();
        for (int i = 0; i < salts.length; i++) {
            key.getSalts().add(new TLLastKnownSalt(salts[i].getValidSince(), salts[i].getValidUntil(), salts[i].getSalt()));
        }
        write();
    }

    private synchronized KnownSalt[] readKnownSalts(int dcId) {
        TLKey key = findKey(dcId);
        KnownSalt[] salts = new KnownSalt[key.getSalts().size()];
        for (int i = 0; i < salts.length; i++) {
            TLLastKnownSalt sourceSalt = key.getSalts().get(i);
            salts[i] = new KnownSalt(sourceSalt.getValidSince(), sourceSalt.getValidUntil(), sourceSalt.getSalt());
        }
        return salts;
    }

    @Override
    public synchronized AbsMTProtoState getMtProtoState(final int dcId) {
        return new AbsMTProtoState() {
            @Override
            public byte[] getAuthKey() {
                return ApiStorage.this.getAuthKey(dcId);
            }

            @Override
            public ConnectionInfo fetchConnectionInfo() {
                return ApiStorage.this.getConnectionInfo(dcId);
            }

            @Override
            public KnownSalt[] readKnownSalts() {
                return ApiStorage.this.readKnownSalts(dcId);
            }

            @Override
            protected void writeKnownSalts(KnownSalt[] salts) {
                ApiStorage.this.writeKnownSalts(dcId, salts);
            }
        };
    }

    @Override
    public synchronized void resetAuth() {
        for (TLKey key : getObj().getKeys()) {
            key.setAuthorised(false);
        }
        getObj().setAuthorized(false);
        getObj().setPrimaryDc(1);
        getObj().setUid(0);
        write();
    }

    @Override
    public synchronized void reset() {
        getObj().getDcInfos().clear();
        getObj().getDcInfos().add(new TLDcInfo(1, "173.240.5.1", 443));

        getObj().getKeys().clear();
        getObj().setAuthorized(false);
        getObj().setPrimaryDc(1);
        getObj().setUid(0);
        write();
    }
}
