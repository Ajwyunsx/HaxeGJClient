package gamejolt.android;

#if android
import haxe.MainLoop;
import lime.system.JNI;
import sys.thread.Thread;

/**
 * Android backend for GameJolt requests.
 *
 * Uses Java's `HttpURLConnection` through JNI instead of `haxe.Http`,
 * because hxcpp's socket/SSL stack frequently fails HTTPS requests on
 * Android (missing CA certificates), and blocking network calls on the
 * main thread are not allowed there anyway.
 */
class GJHttpAndroid {
	static var fetchFunc:Dynamic = null;

	static function init():Void {
		if (fetchFunc == null)
			fetchFunc = JNI.createStaticMethod("org.haxe.extension.GameJoltHttp", "fetch", "(Ljava/lang/String;)Ljava/lang/String;");
	}

	/**
	 * Performs a blocking GET request through Java's `HttpURLConnection`.
	 * Safe to call from any thread, but NOT from the Android UI thread
	 * (use `fetchAsync` there, or let `GJRequest` handle it for you).
	 * @param url The URL to request.
	 * @return The response body, or `null` if the request failed.
	 */
	public static function fetch(url:String):Null<String> {
		init();

		var raw:Dynamic = fetchFunc(url);
		if (raw == null)
			return null;

		var str:String = cast raw;
		if (str.substr(0, 5) == "gjok:")
			return str.substr(5);

		throw str.substr(0, 6) == "gjerr:" ? str.substr(6) : str;
	}

	/**
	 * Performs `fetch()` on a background thread and delivers the result
	 * on the main thread through `haxe.MainLoop`.
	 * @param url The URL to request.
	 * @param onData Called with the response body on success.
	 * @param onError Called with the error message on failure.
	 */
	public static function fetchAsync(url:String, onData:String->Void, onError:String->Void):Void {
		Thread.create(function() {
			try {
				var data:String = fetch(url);
				MainLoop.add(() -> onData(data));
			} catch (e:Dynamic) {
				var message:String = Std.string(e);
				MainLoop.add(() -> onError(message));
			}
		});
	}
}
#end
