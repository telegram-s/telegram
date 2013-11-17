package org.telegram.android.kernel.auth.compat;

import java.io.Serializable;

/**
 * Created by ex3ndr on 17.11.13.
 */
public class CompatDcConfig implements Serializable {
    private String hostName;
    private String ipAddress;
    private int port;

    public CompatDcConfig(String hostName, String ipAddress, int port) {
        this.hostName = hostName;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public CompatDcConfig() {

    }

    public String getHostName() {
        return hostName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }
}
