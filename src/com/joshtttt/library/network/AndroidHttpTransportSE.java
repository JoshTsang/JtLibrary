package com.joshtttt.library.network;

import java.io.IOException;

import org.ksoap2.transport.HttpTransportSE;
import org.ksoap2.transport.ServiceConnection;

public class AndroidHttpTransportSE  extends HttpTransportSE{

    
    private int timeout = 60000; // 默认超时时间�?60s

    public AndroidHttpTransportSE(String url) {
        super(url);
    }

    public AndroidHttpTransportSE(String url, int timeout) {
        super(url);
        this.timeout = timeout;
    }

    @Override
    protected ServiceConnection getServiceConnection() throws IOException {
        ServiceConnectionSE serviceConnection = new ServiceConnectionSE(url);
        serviceConnection.setConnectionTimeOut(timeout);
        return serviceConnection;
    }
}
