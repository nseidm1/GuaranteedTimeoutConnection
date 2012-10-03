package com.gtimeoutc;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import android.os.Handler;
import com.gtimeoutc.Exceptions.WrongModeException;
import com.gtimeoutc.callbacks.InputStreamCallback;
import com.gtimeoutc.callbacks.OpenConnectionCallback;
/**
 * @author Noah Seidman
 */
public class GuaranteedTimeoutConnection
{
    private int mMilliseconds;
    private Thread mOpenConnectionThread;
    private boolean mUseSSL;
    private Handler mUiHandler;
    private HttpURLConnection mHttpUrlConnection;
    private HttpsURLConnection mHttpsUrlConnection;
    private Runnable mTimeoutRunnable = new Runnable()
    {
	@Override
	public void run()
	{
	    mHttpUrlConnection.disconnect();
	    mOpenConnectionThread.interrupt();
	}
    };
    /**
     * This wrapper creates an HttpURLConnection with or without SSL; use the
     * withSSL boolean in the constructor to specify use of HttpsURLConnection.
     * You can modify the HttpsURLConnection using the respective get method,
     * clearly if you want to specify a custom socket factory this is needed. Be
     * cautious that if you pass null as the uiHandler param a handler will be
     * created; if passing null please call the constructor from the ui thread,
     * or use Looper properly. You can also pass a handler for the ui if desired
     * to construct this not in the ui thread.
     * 
     * @param milliseconds
     *            Specify your guaranteed timeout.
     * @param useSSL
     *            Specify if SSL will be used.
     * @param uiHandler
     *            Provide null and a handler will be created, or pass a handler
     *            for the ui thread if desired. If you pass null make sure this
     *            constructor is called from the ui thread or Looper is used
     *            properly.
     */
    public GuaranteedTimeoutConnection(int milliseconds, boolean useSSL, Handler uiHandler)
    {
	System.setProperty("http.keepAlive", "false");
	mMilliseconds = milliseconds;
	mUseSSL = useSSL;
	if (uiHandler == null)
	{
	    mUiHandler = new Handler();
	}
	else
	{
	    mUiHandler = uiHandler;
	}
    }
    /**
     * Return the HttpURLConnection
     * 
     * @throws WrongModeException
     *             If you specify true as the useSSL mode, this will throw a
     *             WrongModeException
     */
    public HttpURLConnection getHttpURLConnection() throws WrongModeException
    {
	if (mUseSSL)
	    throw new WrongModeException("You specied useSSL (true) as a param in the constructor");
	return mHttpUrlConnection;
    }
    /**
     * Return the HttpsURLConnection
     * 
     * @throws WrongModeException
     *             If you specify false as the useSSL mode, this will throw a
     *             WrongModeException
     */
    public HttpURLConnection getHttpsURLConnection() throws WrongModeException
    {
	if (!mUseSSL)
	    throw new WrongModeException("You specied useSSL (false) as a param in the constructor");
	return mHttpsUrlConnection;
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
    public void getInputStream(final InputStreamCallback inputStreamCallback, final URL url, final boolean connectionTimeoutRemainsAfterInputStreamReturned)
    {
	mUiHandler.postDelayed(mTimeoutRunnable, mMilliseconds);
	mOpenConnectionThread = new Thread()
	{
	    @Override
	    public void run()
	    {
		try
		{
		    if (mUseSSL)
		    {
			mHttpsUrlConnection = (HttpsURLConnection) url.openConnection();
			mHttpsUrlConnection.connect();
		    }
		    else
		    {
			mHttpUrlConnection = (HttpURLConnection) url.openConnection();
			mHttpUrlConnection.connect();
		    }
		    final InputStream in;
		    if (mUseSSL)
		    {
			in = mHttpsUrlConnection.getInputStream();
		    }
		    else
		    {
			in = mHttpUrlConnection.getInputStream();
		    }
		    if (!connectionTimeoutRemainsAfterInputStreamReturned)
			mUiHandler.removeCallbacks(mTimeoutRunnable);
		    mUiHandler.post(new Runnable()
		    {
			@Override
			public void run()
			{
			    inputStreamCallback.getInputStream(in, null);
			}
		    });
		}
		catch (final Exception e)
		{
		    mUiHandler.post(new Runnable()
		    {
			@Override
			public void run()
			{
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
     * This method creates a thread and posts to the supplied callback instead
     * of returning the inputStream; please consider this during your
     * development.
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
    public void openConnection(final OpenConnectionCallback openConnectionCallback, final URL url, final boolean connectionTimeoutRemainsAfterConnected)
    {
	mUiHandler.postDelayed(mTimeoutRunnable, mMilliseconds);
	mOpenConnectionThread = new Thread()
	{
	    @Override
	    public void run()
	    {
		try
		{
		    if (mUseSSL)
		    {
			mHttpsUrlConnection = (HttpsURLConnection) url.openConnection();
			mHttpsUrlConnection.connect();
		    }
		    else
		    {
			mHttpUrlConnection = (HttpURLConnection) url.openConnection();
			mHttpUrlConnection.connect();
		    }
		    if (!connectionTimeoutRemainsAfterConnected)
			mUiHandler.removeCallbacks(mTimeoutRunnable);
		    mUiHandler.post(new Runnable()
		    {
			@Override
			public void run()
			{
			    openConnectionCallback.connectionOpened(true, null);
			}
		    });
		}
		catch (final Exception e)
		{
		    mUiHandler.post(new Runnable()
		    {
			@Override
			public void run()
			{
			    openConnectionCallback.connectionOpened(false, e);
			}
		    });
		}
	    }
	};
	mOpenConnectionThread.start();
    }
}