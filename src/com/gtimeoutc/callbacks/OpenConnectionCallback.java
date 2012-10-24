package com.gtimeoutc.callbacks;

import java.net.URLConnection;

public interface OpenConnectionCallback{
    public void connectionOpened(URLConnection urlConnection, Exception exception);
}
