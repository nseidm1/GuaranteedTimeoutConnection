package com.project.gtimeoutc;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import android.os.Handler;

import com.project.gtimeoutc.Exceptions.WrongModeException;
import com.project.gtimeoutc.callbacks.InputStreamCallback;
import com.project.gtimeoutc.callbacks.OpenConnectionCallback;

/**
 * @author Noah Seidman
 */
public class GuaranteedTimeoutConnection {
	private int mMilliseconds;
	private Thread mOpenConnectionThread;
	private boolean mUseSSL;
	private Handler mHandler = new Handler();
	private HttpURLConnection mHttpUrlConnection;
	private HttpsURLConnection mHttpsUrlConnection;
	private Runnable mTimeoutRunnable = new Runnable() {
		@Override
		public void run() {
			mHttpUrlConnection.disconnect();
			mOpenConnectionThread.interrupt();
		}
	};

	/**
	 * This library creates an HttpURLConnection with or without SSL; use the
	 * withSSL boolean in the constructor to specify. You can modify the
	 * HttpsURLConnection using the respective get method, clearly if you want
	 * to specify a custom socket factory this is needed. Be cautious that the
	 * constructor creates a Handler so either call the constructor from the UI
	 * thread or use Looper properly.
	 * 
	 * @param milliseconds
	 *            Specify your guaranteed timeout.
	 * @param useSSL
	 *            Specify if SSL will be used.
	 */
	public GuaranteedTimeoutConnection(int milliseconds, boolean useSSL) {
		System.setProperty("http.keepAlive", "false");
		mMilliseconds = milliseconds;
		mUseSSL = useSSL;
	}

	/**
	 * Return the HttpURLConnection
	 * 
	 * @throws WrongModeException
	 *             If you specify true as the useSSL mode, this will throw a
	 *             WrongModeException
	 */
	public HttpURLConnection getHttpURLConnection() throws WrongModeException {
		if (mUseSSL)
			throw new WrongModeException(
					"You specied useSSL (true) as a param in the constructor");
		return mHttpUrlConnection;
	}

	/**
	 * Return the HttpsURLConnection
	 * 
	 * @throws WrongModeException
	 *             If you specify false as the useSSL mode, this will throw a
	 *             WrongModeException
	 */
	public HttpURLConnection getHttpsURLConnection() throws WrongModeException {
		if (!mUseSSL)
			throw new WrongModeException(
					"You specied useSSL (false) as a param in the constructor");
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
	 */
	public void getInputStream(final InputStreamCallback inputStreamCallback,
			final URL url) {
		mHandler.postDelayed(mTimeoutRunnable, mMilliseconds);
		mOpenConnectionThread = new Thread() {
			@Override
			public void run() {
				try {
					if (mUseSSL) {
						mHttpsUrlConnection = (HttpsURLConnection) url
								.openConnection();
						mHttpsUrlConnection.connect();
					} else {
						mHttpUrlConnection = (HttpURLConnection) url
								.openConnection();
						mHttpUrlConnection.connect();
					}
					mHandler.removeCallbacks(mTimeoutRunnable);
					final InputStream in;
					if (mUseSSL) {
						in = mHttpsUrlConnection.getInputStream();
					} else {
						in = mHttpUrlConnection.getInputStream();
					}
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							inputStreamCallback.getInputStream(in, null);
						}
					});
				} catch (final Exception e) {
					mHandler.post(new Runnable() {
						@Override
						public void run() {
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
	 */
	public void openConnection(
			final OpenConnectionCallback openConnectionCallback, final URL url) {
		mHandler.postDelayed(mTimeoutRunnable, mMilliseconds);
		mOpenConnectionThread = new Thread() {
			@Override
			public void run() {
				try {
					if (mUseSSL) {
						mHttpsUrlConnection = (HttpsURLConnection) url
								.openConnection();
						mHttpsUrlConnection.connect();
					} else {
						mHttpUrlConnection = (HttpURLConnection) url
								.openConnection();
						mHttpUrlConnection.connect();
					}
					mHandler.removeCallbacks(mTimeoutRunnable);
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							openConnectionCallback.connectionOpened(true, null);
						}
					});
				} catch (final Exception e) {
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							openConnectionCallback.connectionOpened(false,
									e);
						}
					});
				}
			}
		};
		mOpenConnectionThread.start();
	}
}