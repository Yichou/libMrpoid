package com.mrpoid.core;

import android.content.Context;

/**
 * 保存全局静态变量
 * 
 * @author Yichou
 *
 */
public final class EmuStatics {
	public static Context appContext;
	
	
	public static Context getAppContext() {
		return appContext;
	}
	
	public static void setAppContext(Context appContext) {
		EmuStatics.appContext = appContext;
	}
}
