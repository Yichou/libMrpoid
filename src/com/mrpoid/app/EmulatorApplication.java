/*
 * Copyright (C) 2013 The Mrpoid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mrpoid.app;

import android.app.Application;
import android.content.Context;
import android.os.Process;
import android.util.Log;

import com.mrpoid.core.EmuLog;
import com.mrpoid.core.Emulator;
import com.mrpoid.core.Prefer;
import com.yichou.common.sdk.ISdk;
import com.yichou.common.sdk.ISdk.CheckUpdateCallback;
import com.yichou.common.sdk.SdkUtils;

/**
 * 
 * @author Yichou2013-12-20
 *
 */
public class EmulatorApplication extends Application {
	static Context gContext;
	
	public static Context getContext() {
		return gContext;
	}
	
	public static void callOnCreate(Context context) {
		EmuLog.d("", "EmulatorApplication create! pid=" + Process.myPid());
		
		System.out.println(context.getApplicationInfo().processName);
		
		gContext = context.getApplicationContext();
		Emulator.getInstance().attachApplicationContext(gContext);
		
		// 一定要在模拟器初始化之后
		Prefer.getInstance().init(gContext);
		SdkUtils.getSdk().enableCrashHandle(gContext);
		SdkUtils.getSdk().updateOnlineParams(gContext);
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
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		callOnCreate(this);
	}
}
