package com.mrpoid.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import com.mrpoid.core.Emulator;
import com.mrpoid.core.Prefer;
import com.mrpoid.core.Res;
import com.yichou.sdk.CheckUpdateCallback;
import com.yichou.sdk.DownloadCallback;
import com.yichou.sdk.SdkInterface;
import com.yichou.sdk.SdkUtils;

/**
 * 模拟器入口基类，封装了一些初始化销毁工作
 * 
 * @author Yichou
 *
 */
public abstract class BaseActivity extends Activity { //implements 
	
	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);

		//错误报告
		SdkUtils.setRealImpl(fetchSdk());
		SdkUtils.enableCrashHandle(this, true);
		SdkUtils.updateOnlineParams(this);
		SdkUtils.setUpdateCfg(false, false);
		SdkUtils.setCheckUpdateCallback(new CheckUpdateCallback() {
			@Override
			public void onCheckUpdateRet(int ret, Object date) {
				switch (ret) {
				case CheckUpdateCallback.RET_HAS_NEW:
					SdkUtils.showUpdateDialog(getActivity(), date);
					break;

				case CheckUpdateCallback.RET_NO_NEW: // has no update
					Toast.makeText(getActivity(), "已是最新版^_^", Toast.LENGTH_SHORT).show();
					break;
					
				case CheckUpdateCallback.RET_NO_WIFI: // none wifi
					Toast.makeText(getActivity(), "你没有连接wifi哦", Toast.LENGTH_SHORT).show();
					break;
					
				case CheckUpdateCallback.RET_FAILUE: // time out
					Toast.makeText(getActivity(), "网络好像不给力哟", Toast.LENGTH_SHORT).show();
					break;
				}
			}
		});
		
		SdkUtils.setDownloadCallback(new DownloadCallback() {
			@Override
			public void OnDownloadRet(int ret) {
				if(ret != DownloadCallback.RET_SUCCESS)
					Toast.makeText(getActivity(), "更新包下载失败，错误码" + ret , Toast.LENGTH_SHORT).show();
			}
		});
	}
	
	private Activity getActivity(){
		return this;
	}
	
	@Override
	protected void onPause() {
		super.onPause();

		SdkUtils.onPause(this);
	}

	@Override
	protected void onResume() {
		super.onResume();

		SdkUtils.onResume(this);
	}

	@Override
	protected void onDestroy() {
		Prefer.getInstance().otherSave();

		Emulator.releaseInstance();

		super.onDestroy();
	}
	
	protected abstract SdkInterface fetchSdk() ;
}
