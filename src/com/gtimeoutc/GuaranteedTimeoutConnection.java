package com.gtimeoutc;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import android.os.Handler;
import com.gtimeoutc.callbacks.InputStreamCallback;
import com.gtimeoutc.callbacks.OpenConnectionCallback;

/**
 * @author Noah Seidman
 */
public class GuaranteedTimeoutConnection {
    private int                mMilliseconds;
    private Thread             mOpenConnectionThread;
    private boolean            mUseSSL;
    private Handler            mUiHandler;
    private HttpURLConnection  mHttpUrlConnection;
    private HttpsURLConnection mHttpsUrlConnection;
    private Runnable           mTimeoutRunnable = new Runnable(){
	@Override
	public void run(){
	    mHttpUrlConnection.disconnect();
	    mOpenConnectionThread.interrupt();
	}
    };

    /**
     * This wrapper creates an HttpURLConnection with or without SSL; use the
     * withSSL boolean in the constructor to specify use of HttpsURLConnection.
     * You can modify the HttpsURLConnection using the respective get method,
     * clearly if you want to specify a custom socket factory this is needed.
     * You must also pass a handler for the ui thread.
     * 
     * @param milliseconds
     *            Specify your guaranteed timeout.
     * @param useSSL
     *            Specify if SSL will be used.
     * @param uiHandler
     *            Pass a handler for the ui thread.
     * @throws NullPointerException
     * 		  Throws null pointer if the UI handler is null.
     */
    public GuaranteedTimeoutConnection(int     milliseconds, 
	    			       boolean useSSL, 
	    			       Handler uiHandler){
	System.setProperty("http.keepAlive", "false");
	mMilliseconds = milliseconds;
	mUseSSL       = useSSL;
	mUiHandler    = uiHandler;
    }

    /**
     * This method creates a thread and posts to the supplied callback instead
     * of returning the inputStream directly from the method; please consider
     * this during your development.
     * 
     * @param inputStreamCallback
     *            The InputStreamCallback will always be called! It will either
     *            return the actual inputStream or null and an exception.
     * @param url
     *            Supply the url for the connection.
     * @param connectionTimeoutRemainsAfterInputStreamReturned
     *            Should the timeout occur even after the input stream is
     *            returned?
     */
    public void getInputStream(final InputStreamCallback inputStreamCallback,
	    		       final URL 		 url,
	    		       final boolean 		 connectionTimeoutRemainsAfterCallback){
	postTimeoutRunnable();
	mOpenConnectionThread = new Thread(){
	    @Override
	    public void run(){
		try{
		    if (mUseSSL){
			mHttpsUrlConnection = (HttpsURLConnection) url.openConnection();
			mHttpsUrlConnection.connect();
		    } else{
			mHttpUrlConnection = (HttpURLConnection) url.openConnection();
			mHttpUrlConnection.connect();
		    }
		    final InputStream in;
		    if (mUseSSL){
			in = mHttpsUrlConnection.getInputStream();
		    }else{
			in = mHttpUrlConnection.getInputStream();
		    }
		    if (!connectionTimeoutRemainsAfterCallback)
			mUiHandler.removeCallbacks(mTimeoutRunnable);
		    mUiHandler.post(new Runnable(){
			@Override
			public void run(){
			    inputStreamCallback.getInputStream(in, null);
			}
		    });
		}catch (final Exception e){
		    mUiHandler.post(new Runnable(){
			@Override
			public void run(){
			    inputStreamCallback.getInputStream(null, e);
			}
		    });
		}
	    }
	};
	mOpenConnectionThread.start();
    }

    /**
     * 
     * This method creates a thread and posts the URLConnection to the supplied callback 
     * instead of returning the inputStream.
     * 
     * @param openConnectionCallback
     *            The OpenConnectionCallback will always be called. It will pass
     *            true and false if the connection succeeds or fails to open.
     *            You can subsequently use the get methods to obtain a reference
     *            to the HttpURLConnection.
     * @param url
     *            Supply the url for the connection.
     * @param connectionTimeoutRemainsAfterConnected
     *            Should the timeout occur even after the connect is
     *            established?
     */
    public void openConnection(final OpenConnectionCallback openConnectionCallback, 
	    		       final URL 		    url,
	    		       final boolean 		    connectionTimeoutRemainsAfterCallback){
	postTimeoutRunnable();
	mOpenConnectionThread = new Thread(){
	    @Override
	    public void run(){
		try {
		    if (mUseSSL){
			mHttpsUrlConnection = (HttpsURLConnection) url.openConnection();
			mUiHandler.post(new Runnable(){
			    @Override
			    public void run(){
				openConnectionCallback.connectionOpened(mHttpsUrlConnection, null);
			    }
			});
		    }else{
			mHttpUrlConnection = (HttpURLConnection) url.openConnection();
			mUiHandler.post(new Runnable(){
			    @Override
			    public void run(){
				openConnectionCallback.connectionOpened(mHttpUrlConnection, null);
			    }
			});
		    }
		    if (!connectionTimeoutRemainsAfterCallback)
			mUiHandler.removeCallbacks(mTimeoutRunnable);
		}catch (final Exception e){
		    mUiHandler.post(new Runnable(){
			@Override
			public void run(){
			    openConnectionCallback.connectionOpened(null, e);
			}
		    });
		}
	    }
	};
	mOpenConnectionThread.start();
    }

    private void postTimeoutRunnable(){
	mUiHandler.postDelayed(mTimeoutRunnable, mMilliseconds);
    }
}