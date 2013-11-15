package com.mrpoid.plugin;

import java.io.File;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;

import com.yichou.common.dl.DownloadListener;
import com.yichou.common.dl.DownloadManager;
import com.yichou.common.sdk.SdkUtils;
import com.yichou.common.utils.ApkUtils;
import com.yichou.common.utils.FileUtils;

/**
 * 插件管理器
 * 
 * @author Yichou
 * 
 */
public final class PluginManager {
	static final String TAG = "PluginManager";

	private static PluginManager instance;

	
	public static PluginManager getInstance() {
		if (instance == null) {
			instance = new PluginManager();
		}
		return instance;
	}

	private PluginManager() {
	}

	public interface LoadCallback {
		public void onSuccess(File path);

		public void onFailue(String msg);
	}

	private boolean isUrlValid(String url) {
		if (url == null || url.length() == 0 || url.equals("null"))
			return false;
		return true;
	}

	public void loadToLocal(final Activity context, String key, String serverPath, final LoadCallback cb) {
		final File pluginPath = context.getFilesDir();
		final File file = new File(pluginPath, key + ".apk");

		int localVer = -1;
		if (file.exists()) {
			try {
				localVer = ApkUtils.getPackageInfo(context, file.getAbsolutePath()).versionCode;
			} catch (Exception e) { // 本地文件有不完整会抛异常
				file.delete();
			}
		}
		Log.d(TAG, "local ver=" + localVer);

		int assetVer = -1; // assets 下版本，-1 不存在
		Log.d(TAG, "asset ver=" + assetVer);

		int serverVer = SdkUtils.getOnlineInt(context, "mrpoidPluginVer", -1);
		Log.d(TAG, "server ver=" + serverVer);

		// 加载优先级 本地->assets->网络
		if (serverVer > assetVer && serverVer > localVer && isUrlValid(serverPath)) { // 网络版本最新
			Log.i(TAG, "downloadUrl = " + serverPath);

			final Handler handler = new Handler();
			
			if (!DownloadManager.isFileDownloadIng(file.getAbsolutePath())) {
				DownloadManager.start(context, file.getAbsolutePath(), serverPath, new DownloadListener() {
					@Override
					public void onFinish() {
						Log.d(TAG, "download plugin success!");
						handler.post(new Runnable() {

							@Override
							public void run() {
								// 加载插件
								cb.onSuccess(file);
							}
						});
					}

					@Override
					public void onError(String msg) {
						final String MSG = "download plugin Error! " + msg;
						Log.d(TAG, MSG);

						handler.post(new Runnable() {

							@Override
							public void run() {
								cb.onFailue(MSG);
							}
						});
					}
				});
			}
		} else if (assetVer > localVer) { // assets 版本比本地新
			Log.d(TAG, "asset is new replace local!");
			file.delete();

			if (FileUtils.SUCCESS != FileUtils.assetToFile(context, key, file))
				cb.onFailue("assets to local FAIL!");
			else
				cb.onSuccess(file);
		} else { /* else 加载本地 */
			if (file.exists())
				cb.onSuccess(file);
			else
				cb.onFailue("load from Local Fail!");
		}
	}
}
