package com.mrpoid.app;

import android.app.Activity;

import com.mrpoid.core.Emulator;
import com.mrpoid.core.Prefer;
import com.yichou.common.sdk.SdkUtils;

/**
 * 模拟器入口基类，封装了一些初始化销毁工作
 * 
 * @author Yichou
 *
 */
public abstract class BaseActivity extends Activity { //implements 
	
	@Override
	protected void onPause() {
		super.onPause();

		SdkUtils.getSdk().onPause(this);
	}

	@Override
	protected void onResume() {
		super.onResume();

		SdkUtils.getSdk().onResume(this);
	}

	@Override
	protected void onDestroy() {
		Prefer.getInstance().otherSave();

		Emulator.releaseInstance();

		super.onDestroy();
	}
}
