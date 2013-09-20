/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

import com.mrpoid.R;
import com.mrpoid.core.Emulator;
import com.yichou.common.InternalID;
import com.yichou.common.UIUtils;


/**
 * 模拟器 Service 
 * 
 * @author Yichou 2013-9-6
 *
 */
public class EmuService extends Service {
	public static final String ACTION_FOREGROUND = "com.androidemu.actions.FOREGROUND";
	public static final String ACTION_BACKGROUND = "com.androidemu.actions.BACKGROUND";
    
	public class LocalBinder extends Binder {
		EmuService getService() {
            return EmuService.this;
        }
    }
	
	// This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();
    
    @Override
    public void onCreate() {
    	super.onCreate();
    }

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		handleStart(intent);
		// We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY_COMPATIBILITY;
	}
	
	@SuppressWarnings("deprecation")
	private void handleStart(Intent intent) {
		if (ACTION_FOREGROUND.equals(intent.getAction())) {
			// 建立新的Intent
			Intent notifyIntent = new Intent(this, EmulatorActivity.class);
			notifyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			
			// 建立PendingIntent作为设定递延执行的Activity
			PendingIntent appIntent = PendingIntent.getActivity(this, 0, notifyIntent, 0);
			
			// 建立Notification，并设定相关参数
			Notification n = new Notification(R.drawable.ic_notify_small, null, System.currentTimeMillis());
			n.setLatestEventInfo(this, 
					Emulator.getInstance().getCurMrpFile().getAppName(), 
					getString(R.string.hint_click_to_back), 
					appIntent);
			
			if(n.contentView != null){
				n.contentView.setImageViewResource(InternalID.id_icon, R.drawable.ic_notify);
			}
			
			n.defaults = Notification.DEFAULT_LIGHTS;
			n.flags = Notification.FLAG_ONGOING_EVENT; //不可清楚
			
            startForeground(1001, n); 
        } else if (ACTION_BACKGROUND.equals(intent.getAction())) {
        	stopForeground(true);
        }
	}
	
    @Override
    public void onDestroy() {
    	stopForeground(true);
    	UIUtils.ToastMessage(this, "I am killed!");
    }
}
