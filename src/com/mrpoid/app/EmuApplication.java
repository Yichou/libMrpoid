package com.mrpoid.app;

import android.app.Application;
import android.util.Log;

import com.mrpoid.core.EmuStatics;
import com.mrpoid.core.Emulator;
import com.mrpoid.core.Prefer;
import com.yichou.common.sdk.ISdk;
import com.yichou.common.sdk.ISdk.CheckUpdateCallback;
import com.yichou.common.sdk.SdkUtils;


/**
 * 
 * @author Yichou
 *
 */
public class EmuApplication extends Application {
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		EmuStatics.setAppContext(this);
		
		Emulator.getInstance().init(this);
		
		// 一定要在模拟器初始化之后
		Prefer.getInstance().init(this);
		
		SdkUtils.getSdk().enableCrashHandle(this);
		SdkUtils.getSdk().updateOnlineParams(this);
		SdkUtils.getSdk().setUpdateWifiOnly(false);
		SdkUtils.getSdk().setCheckUpdateCallback(new CheckUpdateCallback() {
			
			@Override
			public void onSuccess(Object updateInfo) {
				
			}
			
			@Override
			public void onFailure(int code, String msg) {
				Log.e("update", "update fail code=" + code + ", msg=" + msg);
			}
		});
		
		SdkUtils.getSdk().setUpdateDownloadCallback(new ISdk.DownloadCallback() {
			
			@Override
			public void onSuccess() {
			}
			
			@Override
			public void onFailure(int code, String msg) {
				Log.e("update", "download fial code=" + code + ", savePath=" + msg);
			}
		});
	}
}
