package org.haxe.extension;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Blocking HTTP GET helper for the GameJolt client on Android.
 *
 * Requests are performed through Java's HttpURLConnection, which handles
 * SSL certificates correctly on Android (unlike hxcpp's haxe.Http) and,
 * as long as it is called off the main thread, avoids
 * NetworkOnMainThreadException entirely.
 *
 * The result is prefixed so the Haxe side can distinguish success from
 * failure without exceptions crossing the JNI boundary:
 *   "gjok:<body>"  on HTTP 2xx
 *   "gjerr:<msg>"  on any failure
 */
public class GameJoltHttp extends Extension
{
	public static final String LOG_TAG = "GameJoltHttp";

	private static final int CONNECT_TIMEOUT = 15000;
	private static final int READ_TIMEOUT = 15000;

	public static String fetch(final String urlString)
	{
		HttpURLConnection connection = null;

		try
		{
			URL url = new URL(urlString);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(CONNECT_TIMEOUT);
			connection.setReadTimeout(READ_TIMEOUT);
			connection.setUseCaches(false);
			connection.setInstanceFollowRedirects(true);
			connection.setRequestProperty("User-Agent", "HaxeGJClient/Android");

			final int code = connection.getResponseCode();
			final InputStream stream = (code >= 200 && code < 400) ? connection.getInputStream() : connection.getErrorStream();
			final String body = readFully(stream);

			if (code >= 200 && code < 300)
				return "gjok:" + body;

			return "gjerr:HTTP " + code + (body.length() > 0 ? ": " + body : "");
		}
		catch (Exception e)
		{
			return "gjerr:" + e.toString();
		}
		finally
		{
			if (connection != null)
				connection.disconnect();
		}
	}

	private static String readFully(final InputStream in) throws Exception
	{
		if (in == null)
			return "";

		final StringBuilder sb = new StringBuilder();
		final BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		final char[] buffer = new char[4096];
		int read;

		while ((read = reader.read(buffer)) != -1)
			sb.append(buffer, 0, read);

		reader.close();
		return sb.toString();
	}
}
