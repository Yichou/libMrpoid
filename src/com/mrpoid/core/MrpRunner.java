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
package com.mrpoid.core;

import android.app.Activity;
import android.content.Intent;

import com.mrpoid.core.procmgr.AppProcess;
import com.mrpoid.core.procmgr.AppProcessManager;
import com.mrpoid.core.procmgr.AppProcessManager.RequestCallback;
import com.yichou.common.utils.UIUtils;

/**
 * MRP 运行管理器
 * 
 * @author Yichou 2013-12-19
 * 
 */
public final class MrpRunner {
	static final String TAG = "MrpRunner";
	
	public static final String INTENT_ACTION_LAUNCH_MRP = "com.mrpoid.launchMrp";
	public static final String INTENT_KEY_PATH = "path";
	public static final String INTENT_KEY_ENTRY_ACTIVITY = "entryActivity";
	
	private static AppProcessManager manager;
	
	
	/**
	 * 启动虚拟机并运行 mrp
	 * 
	 * @param context
	 *            Activity 上下文，（后台运行的时候会返回到此 activity）
	 * 
	 * @param mrpPath
	 *            mrp 路径（绝对路径，或者相对于 mythroad 的路径）
	 * 
	 * @param defProcIndex
	 *            指定在哪个进程中运行 （0~5），若此进程被占用，系统分配一个空闲进程
	 * 
	 * @param foce
	 *            如果 defProcIndex 被占用，强制使用
	 */
	public static void runMrp(final Activity context, final String mrpPath, int defProcIndex, boolean foce) {
		EmuLog.i(TAG, "startMrp(" + mrpPath + ")");
		
		if (manager == null)
			manager = new AppProcessManager(context, "com.mrpoid.apps.AppService", 5);
		
		manager.requestIdleProcess(defProcIndex, foce, mrpPath, new RequestCallback() {
			
			@Override
			public void onSuccess(int procIndex, AppProcess process, boolean alreadyRun) {
				/**
				 * 如果 activity 正在运行，我们需要做的是把 activity 调到前台
				 * 
				 * 下面的启动方法可以达到效果
				 */
				/*if(alreadyRun) {
					
				} else */
				{
					UIUtils.ToastMessage(context, "进程获取成功 " + procIndex);
					
					Intent intent = new Intent(INTENT_ACTION_LAUNCH_MRP);
					intent.setClassName(context, "com.mrpoid.apps.AppActivity" + procIndex);
					
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					
					intent.putExtra(INTENT_KEY_PATH, mrpPath);
					intent.putExtra(INTENT_KEY_ENTRY_ACTIVITY, context.getClass().getName());
					
					context.startActivity(intent);
				}
			}
			
			@Override
			public void onFailure(String msg) {
				UIUtils.ToastMessage(context, "进程获取失败 " + msg);
			}
		});
	}
	
	public static void runMrp(Activity context, String mrpPath) {
		runMrp(context, mrpPath, -1, false);
	}
}
