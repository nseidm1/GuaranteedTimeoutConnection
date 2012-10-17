package com.gtimeoutc.callbacks;

import java.net.URLConnection;

public interface OpenConnectionCallback{
    public void connectionOpened(String TAG, URLConnection urlConnection, Exception exception);
}
