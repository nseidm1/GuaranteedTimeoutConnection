package com.project.gtimeoutc;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import android.os.Handler;

/**
 * @author Noah Seidman
 * This library creates an HttpURLConnection with or without SSL; use the withSSL boolean in the constructor to specify.
 * You can modify the HttpsURLConnection using the respective get method, clearly if you want to specify a custom socket factory this is needed.
 */
public class GuaranteedTimeoutConnection {
	private int mMilliseconds;
	private Thread mInputStreamGettter;
	private boolean mUseSSL;
	private Handler mHandler = new Handler();
	private HttpURLConnection mHttpUrlConnection;
	private HttpsURLConnection mHttpsUrlConnection;
	private Runnable mTimeoutRunnable = new Runnable() {
		@Override
		public void run(){
			mHttpUrlConnection.disconnect();
			mInputStreamGettter.interrupt();
		}
	};
    /**
     * @param milliseconds
     * Specify your guaranteed timeout.
     */
	public GuaranteedTimeoutConnection(int milliseconds, boolean useSSL) {
		System.setProperty("http.keepAlive", "false");
		mMilliseconds = milliseconds;
		mUseSSL = useSSL;
	}
	/**
     * Return the HttpURLConnection
	 * @throws WrongModeException 
	 * If you specify true as the useSSL mode, this will throw a WrongModeException
     */
	public HttpURLConnection getHttpURLConnection() throws WrongModeException
	{
		if (mUseSSL)
			throw new WrongModeException("You specied useSSL (true) as a param in the constructor");
		return mHttpUrlConnection;
	}
	/**
     * Return the HttpsURLConnection
	 * @throws WrongModeException 
	 * If you specify false as the useSSL mode, this will throw a WrongModeException
     */
	public HttpURLConnection getHttpsURLConnection() throws WrongModeException
	{
		if (mUseSSL)
			throw new WrongModeException("You specied useSSL (false) as a param in the constructor");
		return mHttpsUrlConnection;
	}
	/**
     * @param InputStreamCallback
     * @param URL
     * Your inputStreamCallback will always be called! It will either return the actual inputStream or null and an exception
     */
	public void getInputStream(final InputStreamCallback inputStreamCallback, final URL url) throws IOException {
		mHandler.postDelayed(mTimeoutRunnable, mMilliseconds);
		mInputStreamGettter = new Thread()
		{
			@Override
			public void run()
			{
				try {
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

					mHandler.removeCallbacks(mTimeoutRunnable);

					if (mUseSSL)
					{
						inputStreamCallback.getInputStream(mHttpsUrlConnection.getInputStream(), null);
					}
					else
					{
						inputStreamCallback.getInputStream(mHttpUrlConnection.getInputStream(), null);
					}
				} catch (IOException e) {
					inputStreamCallback.getInputStream(null, e);
				}
			}
		};
		mInputStreamGettter.start();
	}
}