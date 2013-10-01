package com.mrpoid.core;

import android.content.Context;
import android.net.Uri;

/**
 * 通用资源管理类
 * 
 * @author Yichou
 *
 */
public final class Res {
	public static final Uri FAQ_URI = Uri.parse("http://mrpej.com/YichouAngle/mrpoid/faq.html");
	public static final Uri CHANGELOG_URI = Uri.parse("http://mrpej.com/YichouAngle/mrpoid/changelog.html");

	public static final Uri FAQ_URI_ASSET = Uri.parse("file:///android_asset/faq.html");
	public static final Uri CHANGELOG_URI_ASSET = Uri.parse("file:///android_asset/changelog.html");
	public static final Uri ABOUT_URI_ASSET = Uri.parse("file:///android_asset/about.html");
	public static final Uri HELP_URI_ASSET = Uri.parse("file:///android_asset/help.html");
	
	
//	private static boolean isLoad = false; //异常恢复避免多次加载
//	
//	public static void load(Context context) {
//		if(isLoad) return;
//		
//		isLoad = true;
//	}
//	
//	public static boolean isLoad() {
//		return isLoad;
//	}
//	
//	public static void unLoad() {
//		isLoad = false;
//	}
}
