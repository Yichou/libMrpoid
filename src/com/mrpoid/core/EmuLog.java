package com.mrpoid.core;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

/**
 * 模拟器日志打印工具
 * 
 * @author Yichou
 *
 */
public class EmuLog {
	private static boolean isShowLog = false;
	private static Toast m_toast = null;

	
	static {
		isShowLog = true;//new File(Environment.getExternalStorageDirectory(), "mythroad/mrpoid_debug.log").exists();
	}
	
	private EmuLog() {
	}
	
	public static void setShowLog(boolean isShow) {
		isShowLog = isShow;
	}

	public static void i(String tag, String msg) {
		if (isShowLog) {
			Log.i(tag, msg!=null? msg : "");
		}
	}

	public static void d(String tag, String msg) {
		if (isShowLog) {
			Log.d(tag, msg!=null? msg : "");
		}
	}

	public static void e(String tag, String msg) {
		Log.e(tag, msg!=null? msg : "");
	}

	public static void v(String tag, String msg) {
		Log.v(tag, msg!=null? msg : "");
	}

	public static void w(String tag, String msg) {
		if (isShowLog) {
			Log.w(tag, msg!=null? msg : "");
		}
	}

	public static void showScreenLog(final Activity activity, final String info) {
		if (isShowLog) {
			activity.runOnUiThread(new Runnable() {
				public void run() {
					if (m_toast == null) {
						//避免每次新建 Toast
						m_toast = Toast.makeText(activity, info, Toast.LENGTH_LONG);
					} else {
						m_toast.setText(info);
					}
					m_toast.show();
				}
			});
		}
	}
}
