package com.mrpoid.app;

import com.mrpoid.core.EmuStatics;

import android.app.Application;

public class EmuApplication extends Application {
	@Override
	public void onCreate() {
		super.onCreate();
		
		EmuStatics.setAppContext(getApplicationContext());
	}
}
