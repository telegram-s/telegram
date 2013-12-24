package org.telegram.android.critical;

import android.content.Context;
import org.telegram.android.core.model.TLLocalContext;
import org.telegram.android.core.model.storage.TLDcInfo;
import org.telegram.android.core.model.storage.TLKey;
import org.telegram.android.core.model.storage.TLLastKnownSalt;
import org.telegram.android.core.model.storage.TLStorage;
import org.telegram.android.log.Logger;
import org.telegram.api.TLConfig;
import org.telegram.api.TLDcOption;
import org.telegram.api.TLUserSelf;
import org.telegram.api.auth.TLAuthorization;
import org.telegram.api.engine.storage.AbsApiState;
import org.telegram.bootstrap.DcInitialConfig;
import org.telegram.mtproto.state.AbsMTProtoState;
import org.telegram.mtproto.state.ConnectionInfo;
import org.telegram.mtproto.state.KnownSalt;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 09.11.13
 * Time: 0:41
 */
public class ApiStorage extends TLPersistence<TLStorage> implements AbsApiState {
    public static final String STORAGE_FILE_NAME = "api.bin";

    private static final String TAG = "ApiStorage";

    public ApiStorage(Context context) {
        super(context, STORAGE_FILE_NAME, TLStorage.class, TLLocalContext.getInstance());

        if (getObj().getDcInfos().size() == 0) {
            getObj().getDcInfos().add(new TLDcInfo(1, DcInitialConfig.ADDRESS, DcInitialConfig.PORT, 0));
        }
    }

    public int[] getKnownDc() {
        HashSet<Integer> dcs = new HashSet<Integer>();
        for (TLDcInfo dcInfo : getObj().getDcInfos()) {
            dcs.add(dcInfo.getDcId());
        }
        Integer[] dcsArray = dcs.toArray(new Integer[0]);
        int[] res = new int[dcs.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = dcsArray[i];
        }
        return res;
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

    public synchronized boolean isAuthenticated() {
        return isAuthenticated(getPrimaryDc());
    }

    public synchronized void doAuth(TLAuthorization authorization) {
        Logger.d(TAG, "doAuth1");
        TLKey key = findKey(getPrimaryDc());
        key.setAuthorised(true);
        getObj().setUid(authorization.getUser().getId());
        getObj().setPhone(((TLUserSelf) authorization.getUser()).getPhone());
        write();
    }

    public synchronized void doAuth(int uid, String phone) {
        Logger.d(TAG, "doAuth2");
        TLKey key = findKey(getPrimaryDc());
        key.setAuthorised(true);
        getObj().setUid(uid);
        getObj().setPhone(phone);
        write();
    }

    @Override
    public synchronized int getPrimaryDc() {
        return getObj().getPrimaryDc();
    }

    @Override
    public synchronized void setPrimaryDc(int dc) {
        Logger.d(TAG, "setPrimaryDc dc #" + dc);
        getObj().setPrimaryDc(dc);
        write();
    }

    @Override
    public synchronized boolean isAuthenticated(int dcId) {
        TLKey key = findKey(dcId);
        return (key != null && key.isAuthorised());
    }

    @Override
    public synchronized void setAuthenticated(int dcId, boolean auth) {
        Logger.d(TAG, "setAuthenticated dc #" + dcId + ": " + auth);
        TLKey key = findKey(dcId);
        key.setAuthorised(auth);
        write();
    }

    @Override
    public synchronized void updateSettings(TLConfig config) {
        int version = 0;
        for (TLDcInfo info : getObj().getDcInfos()) {
            version = Math.max(version, info.getVersion());
        }

        boolean hasUpdates = false;
        for (TLDcOption option : config.getDcOptions()) {
            boolean contains = false;
            for (TLDcInfo info : getObj().getDcInfos().toArray(new TLDcInfo[0])) {
                if (info.getAddress().equals(option.getIpAddress()) && info.getPort() == option.getPort() && info.getDcId() == option.getId() && info.getVersion() == version) {
                    contains = true;
                    break;
                }
            }

            if (!contains) {
                hasUpdates = true;
            }
        }

        if (!hasUpdates) {
            Logger.d(TAG, "No updates for DC");
            return;
        }

        int nextVersion = version + 1;
        for (TLDcOption option : config.getDcOptions()) {
            for (TLDcInfo info : getObj().getDcInfos().toArray(new TLDcInfo[0])) {
                if (info.getAddress().equals(option.getIpAddress()) && info.getDcId() == option.getId()) {
                    getObj().getDcInfos().remove(info);
                }
            }
            getObj().getDcInfos().add(new TLDcInfo(option.getId(), option.getIpAddress(), option.getPort(), nextVersion));
        }
        write();
    }

    public synchronized void updateDCInfo(int dcId, String ip, int port) {
        for (TLDcInfo info : getObj().getDcInfos().toArray(new TLDcInfo[0])) {
            if (info.getAddress().equals(ip) && info.getPort() == port && info.getDcId() == dcId) {
                getObj().getDcInfos().remove(info);
            }
        }

        int version = 0;
        for (TLDcInfo info : getObj().getDcInfos()) {
            version = Math.max(version, info.getVersion());
        }

        getObj().getDcInfos().add(new TLDcInfo(dcId, ip, port, version));
        write();
    }

    @Override
    public synchronized byte[] getAuthKey(int dcId) {
        TLKey key = findKey(dcId);
        return key != null ? key.getAuthKey() : null;
    }

    @Override
    public synchronized void putAuthKey(int dcId, byte[] authKey) {
        Logger.d(TAG, "putAuthKey dc #" + dcId);
        TLKey key = findKey(dcId);
        if (key != null) {
            return;
        }
        getObj().getKeys().add(new TLKey(dcId, authKey));
        write();
    }

    @Override
    public ConnectionInfo[] getAvailableConnections(int dcId) {
        ArrayList<TLDcInfo> infos = new ArrayList<TLDcInfo>();
        int maxVersion = 0;
        for (TLDcInfo info : getObj().getDcInfos()) {
            if (info.getDcId() == dcId) {
                infos.add(info);
                maxVersion = Math.max(maxVersion, info.getVersion());
            }
        }

        ArrayList<ConnectionInfo> res = new ArrayList<ConnectionInfo>();

        // Maximum version addresses
        HashMap<String, DcAddress> mainAddresses = new HashMap<String, DcAddress>();
        for (TLDcInfo i : infos) {
            if (i.getVersion() != maxVersion) {
                continue;
            }

            if (mainAddresses.containsKey(i.getAddress())) {
                mainAddresses.get(i.getAddress()).ports.put(i.getPort(), 1);
            } else {
                DcAddress address = new DcAddress();
                address.ports.put(i.getPort(), 1);
                address.host = i.getAddress();
                mainAddresses.put(i.getAddress(), address);
            }
        }

        for (DcAddress address : mainAddresses.values()) {
            address.ports.put(443, 2);
            address.ports.put(80, 1);
            address.ports.put(25, 0);
        }

        HashMap<Integer, HashMap<String, DcAddress>> otherAddresses = new HashMap<Integer, HashMap<String, DcAddress>>();

        for (TLDcInfo i : infos) {
            if (i.getVersion() == maxVersion) {
                continue;
            }

            if (!otherAddresses.containsKey(i.getVersion())) {
                otherAddresses.put(i.getVersion(), new HashMap<String, DcAddress>());
            }

            HashMap<String, DcAddress> addressHashMap = otherAddresses.get(i.getVersion());

            if (addressHashMap.containsKey(i.getAddress())) {
                addressHashMap.get(i.getAddress()).ports.put(i.getPort(), 1);
            } else {
                DcAddress address = new DcAddress();
                address.ports.put(i.getPort(), 1);
                address.host = i.getAddress();
                addressHashMap.put(i.getAddress(), address);
            }
        }

        for (Integer version : otherAddresses.keySet()) {
            for (DcAddress address : otherAddresses.get(version).values()) {
                if (mainAddresses.containsKey(address.host)) {
                    continue;
                }
                address.ports.put(443, 2);
                address.ports.put(80, 1);
                address.ports.put(25, 0);
            }
        }


        // Writing main addresses
        int index = 0;

        for (DcAddress address : mainAddresses.values()) {
            for (Integer port : address.ports.keySet()) {
                int priority = maxVersion + address.ports.get(port);
                res.add(new ConnectionInfo(index++, priority, address.host, port));
            }
        }

        // Writing other addresses

        for (Integer version : otherAddresses.keySet()) {
            for (DcAddress address : otherAddresses.get(version).values()) {
                for (Integer port : address.ports.keySet()) {
                    int priority = version + address.ports.get(port);
                    res.add(new ConnectionInfo(index++, priority, address.host, port));
                }
            }
        }

        Logger.d(TAG, "Created connections for dc #" + dcId);
        for (ConnectionInfo c : res) {
            Logger.d(TAG, "Connection: #" + c.getId() + " " + c.getAddress() + ":" + c.getPort() + " at " + c.getPriority());
        }

        return res.toArray(new ConnectionInfo[0]);
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
            public ConnectionInfo[] getAvailableConnections() {
                return ApiStorage.this.getAvailableConnections(dcId);
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
        Logger.d(TAG, "resetAuth");
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
        Logger.d(TAG, "reset");
        getObj().getKeys().clear();
        getObj().setAuthorized(false);
        getObj().setPrimaryDc(1);
        getObj().setUid(0);
        write();
    }

    private class DcAddress {
        public String host;
        public HashMap<Integer, Integer> ports = new HashMap<Integer, Integer>();
    }
}
