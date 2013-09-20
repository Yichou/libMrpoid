package com.mrpoid.app;

import com.mrpoid.core.EmuStatics;
import com.mrpoid.core.Emulator;
import com.mrpoid.core.Prefer;

import android.app.Application;
import android.widget.Toast;

public class EmuApplication extends Application {
	private Emulator mEmulator;
	
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		EmuStatics.setAppContext(getApplicationContext());
		
		mEmulator = Emulator.getInstance(this);
		if (mEmulator == null) {
			Toast.makeText(this, "虚拟机初始化失败!", Toast.LENGTH_SHORT).show();
			System.exit(0);
			
			return;
		}
		
		// 一定要在模拟器初始化之后
		Prefer.getInstance().init(this);
	}
}
