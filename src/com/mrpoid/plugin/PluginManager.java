package com.mrpoid.plugin;

import java.io.File;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.umeng.analytics.MobclickAgent;
import com.yichou.common.dl.DownloadListener;
import com.yichou.common.dl.DownloadManager;
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

	private boolean b_inited = false;
	private JSONObject rootObject;
	private HashMap<String, Plugin> pluginMap;
	@SuppressWarnings("unused")
	private int cfgVer;

	private static final int STATE_NON = 0, 
		STATE_DOWNLOADING = 1;
	
	private static PluginManager instance;

	
	public static PluginManager getInstance() {
		if (instance == null) {
			instance = new PluginManager();
		}
		return instance;
	}

	private PluginManager() {
		pluginMap = new HashMap<String, Plugin>();
	}

	private static class Plugin {
		String key;
		String name;
		int version;
		@SuppressWarnings("unused")
		String localPath;
		int state;
	}

	private void init(Context context) {
		String json = FileUtils.readStringFromAsset(context.getAssets(), "plugins");
		try {
			rootObject = new JSONObject(json);

			cfgVer = rootObject.getInt("cfgver");

			JSONArray array = rootObject.getJSONArray("list");
			for (int i = 0, n = array.length(); i < n; ++i) {
				JSONObject object = array.getJSONObject(i);

				Plugin plugin = new Plugin();
				plugin.key = object.getString("key");
				plugin.name = object.getString("name");
				plugin.version = object.getInt("version");
				plugin.localPath = null;
				plugin.state = STATE_NON;
				pluginMap.put(plugin.key, plugin);
			}

		} catch (JSONException e) {
			Log.e(TAG, "init FAIL!" + e.getMessage());
		}

		b_inited = true;
	}

	public interface LoadCallback {
		public void onSuccess(File path);

		public void onFailue(String msg);
	}

	private int getState(String key) {
		Plugin i = pluginMap.get(key);
		if (i != null)
			return i.state;
		return STATE_NON;
	}

	private boolean isUrlValid(String url) {
		if (url == null || url.length() == 0 || url.equals("null"))
			return false;
		return true;
	}

	public void loadToLocal(final Activity context, String key, String serverPath, final LoadCallback cb) {
		if (!b_inited)
			init(context);

		int state = getState(key);
		if (state == STATE_DOWNLOADING)
			return;

		Plugin plugin = pluginMap.get(key);
		if (plugin == null) {
			cb.onFailue("can not find the key " + key);
			return;
		}

		final File pluginPath = context.getFilesDir();
		final File file = new File(pluginPath, plugin.name);

		int localVer = 1;
		if (file.exists()) {
			try {
				localVer = ApkUtils.getPackageInfo(context, file.getAbsolutePath()).versionCode;
			} catch (Exception e) { // 本地文件有不完整会抛异常
				file.delete();
			}
		}
		Log.d(TAG, "local ver=" + localVer);

		int assetVer = plugin.version; // assets 下版本
		if (!FileUtils.assetExist(context.getAssets(), plugin.name))
			assetVer = 1; // 代表 assets 下不存在
		Log.d(TAG, "asset ver=" + assetVer);

		int serverVer = 1;
		try {
			serverVer = Integer.valueOf(MobclickAgent.getConfigParams(context, "adsPluginVer"));
		} catch (Exception e) {
		}
		Log.d(TAG, "server ver=" + serverVer);

		// 加载优先级 本地->assets->网络
		String downloadUrl = MobclickAgent.getConfigParams(context, "adsPluginUrl");
		if (serverVer > assetVer && serverVer > localVer && isUrlValid(downloadUrl)) { // 网络版本最新
			Log.i(TAG, "downloadUrl = " + downloadUrl);

			final Handler handler = new Handler();
			
			if (!DownloadManager.isFileDownloadIng(file.getAbsolutePath())) {
				DownloadManager.start(context, file.getAbsolutePath(), downloadUrl, new DownloadListener() {
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

			if (FileUtils.SUCCESS != FileUtils.assetToFile(context, plugin.name, file))
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
