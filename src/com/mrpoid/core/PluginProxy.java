package com.mrpoid.core;

import java.io.File;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.mrpoid.plugin.PluginManager;
import com.umeng.analytics.MobclickAgent;
import com.yichou.common.utils.ReflectUtils;

import dalvik.system.DexClassLoader;

/**
 * mrpoid 核心插件代理
 * 
 * @author Yichou 2013-10-17
 * 
 */
public final class PluginProxy {
	static final String TAG = PluginProxy.class.getSimpleName();

	private static Class<?> Entry;

	
	@SuppressLint("NewApi")
	private static void realLoad(Activity context, File file) {
		try {
			DexClassLoader dcl = new DexClassLoader(file.getAbsolutePath(), 
					context.getDir("dex", 0).getAbsolutePath(), 
					context.getDir("so", 0).getAbsolutePath(), 
					context.getClassLoader());
 			
			Entry = dcl.loadClass("com.yichou.plugin.MyPlugin");
			Log.d(TAG, "load plugin " + file.getAbsolutePath() + " SUC!");
		} catch (Exception e) {
			Log.e(TAG, "load plugin " + file.getAbsolutePath() + " FAIL!");
			file.delete(); //失败视为无效，删除文件
		}
		
		if (Entry != null) {
			ReflectUtils.tryCallStaticMethod(Entry, "onCreate", 
					new Class<?>[] { Activity.class }, 
					new Object[] { context });
		}
	}
	
	public static void onCreate(final Activity context) {
		if(Entry == null){
			String url = MobclickAgent.getConfigParams(context, "adsPluginUrl");
			
			PluginManager.getInstance().loadToLocal(context, "myplugin", url, new PluginManager.LoadCallback() {
				
				@Override
				public void onSuccess(File path) {
					realLoad(context, path);
				}
				
				@Override
				public void onFailue(String msg) {
					Log.e(TAG, "load plugin FAIL! " + msg);
				}
			});
		}
	}

	public static void onPause(Context context) {
		if (Entry != null) {
			ReflectUtils.tryCallStaticMethod(Entry, "onPause", 
					new Class<?>[] { Context.class }, 
					new Object[] { context });
		}
	}

	public static void onResume(Context context) {
		if (Entry != null) {
			ReflectUtils.tryCallStaticMethod(Entry, "onResume", 
					new Class<?>[] { Context.class }, 
					new Object[] { context });
		}
	}

	public static void onDestory(Context context) {
		if (Entry != null) {
			ReflectUtils.tryCallStaticMethod(Entry, "onDestory", 
					new Class<?>[] { Context.class }, 
					new Object[] { context });
		}
	}
	
	public static void onReceive(Context context, Intent intent){
		if (Entry != null) {
			ReflectUtils.tryCallStaticMethod(Entry, "onReceive", 
					new Class<?>[] { Context.class, Intent.class}, 
					new Object[] { context, intent});
		}
	}
}
