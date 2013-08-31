package com.mrpoid.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Point;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.yichou.common.FileUtils;
import com.yichou.sdk.SdkUtils;

/**
 * 配置管理
 * 
 * @author Yichou
 *
 */
public class Prefer implements OnSharedPreferenceChangeListener {
	static final String TAG = "Prefer";
	
	public static final long SD_RESERVE_SIZE = 1024*1024*4;		//SD卡剩余空间小于这个值时, 不再创建新文件
	public static final long ROM_RESERVE_SIZE = 1024*1024*2;	//ROM剩余空间小于这个值时, 不再创建新文件
	
//	private Context context;
	public SharedPreferences sp;
	private boolean bInited; //初始化标志
	
	private static Prefer instance;
	
	public static Prefer getInstance() {
		if(instance == null){
			instance = new Prefer();
		}
		return instance;
	}
	
	private Prefer() {
	}

	private Prefer(Context context) {
//		this.context = context;
	}
	
	public void otherSave() {
		// 需要注销，否则还会收到回调事件
		sp.unregisterOnSharedPreferenceChangeListener(this);
		
		Editor e = sp.edit();

//		e.putInt("theme", THEME==R.style.Theme_Sherlock_Light? 1 : 0);
		e.putInt("keypadMode", Keypad.getInstance().getMode());
		e.putInt("lrbX", lrbX);
		e.putInt("lrbY", lrbY);
		e.putInt("keypadOpacity", keypadOpacity);
		e.putBoolean("notHintAdvSet", notHintAdvSet);
		
		e.putString(KEY_SCALING_MODE, MrpScreen.getScaleModeTag());
		e.putString(KEY_SCN_SIZE, MrpScreen.getSizeTag());
//		e.putBoolean(KEY_SHOW_STATUSBAR, showStatusBar);
		
		e.commit();
	}
	
	private void otherRead() {
		Keypad.getInstance().setMode(sp.getInt("keypadMode", 0));
		
		lrbX = sp.getInt("lrbX", 1);
		lrbY = sp.getInt("lrbY", 1);
		keypadOpacity = sp.getInt("keypadOpacity", 0xf0);
		notHintAdvSet = sp.getBoolean("notHintAdvSet", false);
		
//		THEME = sp.getInt("theme", 0)==1? R.style.Theme_Sherlock_Light : R.style.Theme_Sherlock;
	}

	// apilog 选项
	private static final String[] logs = { 
		"enable_log_input", 
		"enable_log_file", 
		"enable_log_net", 
		"enable_log_mrplat", 
		"enable_log_timer", 
		"enable_log_fw", 
		"enable_log_mrprintf" 
	};
	
	public static final boolean B_DEF_CATCH_VOLUME_KEY = true;
	public static final boolean B_DEF_PLAT_DRAW_CHAR = false;
	public static final boolean B_DEF_MULTI_PATH = true;
	public static final String DEF_SCALE_MOD = MrpScreen.ScaleMode.SCALE_STRE.tag;
	public static final String DEF_MEM_SIZE = "2";

	/**
	 * 初始化读取配置
	 * 
	 * @param context
	 */
	public void init(Context context) {
		if(bInited) return;
		
		bInited = true;
		
		Point scSize = EmuUtils.getScreenSize(context.getResources());
		
		sp = PreferenceManager.getDefaultSharedPreferences(context);
		sp.registerOnSharedPreferenceChangeListener(this);

		setScaleMode(sp.getString("scalingMode", DEF_SCALE_MOD));
		
		if(scSize.x == 480){
			setMrpscnType(sp.getString(KEY_SCN_SIZE, "240x400"));
		}else {
			setMrpscnType(sp.getString(KEY_SCN_SIZE, "240x320"));
		}
		
		dpadAtLeft = sp.getBoolean("dpadAtLeft", false);
		//默认
		showStatusBar = sp.getBoolean(KEY_SHOW_STATUSBAR, (scSize.y<=480? false : true));
		
//		EmuLog.i(TAG, "showStatusBar = " + showStatusBar);
		
		noKey = sp.getBoolean("noKey", false);
		enableAntiAtial = sp.getBoolean("enableAntiAtial", true);
		fullScnEditor = sp.getBoolean("fullScnEditor", false);
		catchVolumekey = sp.getBoolean("catchVolumekey", B_DEF_CATCH_VOLUME_KEY);
		volume = sp.getInt("volume", 100);
		enableSound = sp.getBoolean("enableSound", true);
		showMemInfo = sp.getBoolean("showMemInfo", false);
		differentPath = sp.getBoolean(KEY_MULTI_PATH, B_DEF_MULTI_PATH);
		autoUpdate = sp.getBoolean(KEY_AUTO_UPDATE, true);
		showFloatButton = sp.getBoolean("showFloatButton", false);
		limitInputLength = sp.getBoolean("showFloatButton", false);
		enableKeyVirb = sp.getBoolean(KEY_ENABLE_KEY_VIRB, true);
		
		Emulator emulator = Emulator.getInstance();
		
		emulator.native_setIntOptions("enableSound", sp.getBoolean("enableSound", true)? 1 : 0);
		emulator.native_setIntOptions("platdrawchar", sp.getBoolean("platdrawchar", B_DEF_PLAT_DRAW_CHAR)? 1 : 0);
//		emulator.setThreadMod(Integer.valueOf(sp.getString("thread", "0")));
//		emulator.native_setIntOptions("uselinuxTimer", sp.getBoolean("uselinuxTimer", false) ? 1 : 0);
//		emulator.native_setIntOptions("enableExram", sp.getBoolean("enableExram", false) ? 1 : 0);
//		emulator.setIntOptions("platform", Integer.valueOf(sp.getString("platform", "12"))); // linux
		emulator.native_setIntOptions("uselinuxTimer", 0);
		emulator.native_setIntOptions("enableExram", 0);
		emulator.native_setIntOptions("platform", 12); // linux
		emulator.native_setIntOptions("uselinuxTimer", 1);
		emulator.native_setIntOptions("memSize", Integer.valueOf(sp.getString(KEY_MEM_SIZE, DEF_MEM_SIZE))); //虚拟机内存 单位 M

		if(FileUtils.isSDMounted()){
			EmuPath.getInstance().setSDPath(sp.getString(KEY_SDCARD_PATH, EmuPath.DEF_SD_PATH));
		}else {
			EmuPath.getInstance().setSDPath(context.getFilesDir().getAbsolutePath());
			Toast.makeText(context, "没有SD卡！", Toast.LENGTH_SHORT).show();
		}
		
		if(differentPath){
			EmuPath.getInstance().setMythroadPath( EmuPath.DEF_MYTHROAD_DIR + MrpScreen.getSizeTag() + "/");
		} else {
			EmuPath.getInstance().setMythroadPath(sp.getString(KEY_MYTHROAD_PATH, EmuPath.DEF_MYTHROAD_DIR));
		}

		// api log
		emulator.native_setIntOptions("enableApilog", sp.getBoolean("enableApilog", false) ? 1 : 0);
		for (String key : logs) {
			emulator.native_setIntOptions(key, sp.getBoolean(key, false) ? 1 : 0);
		}
		
		otherRead();
		
		//自动更新处理
		if (autoUpdate) {
			int lastTime = sp.getInt(KEY_LAST_UPDATE_TIME, 0);
			int nowTime = EmuUtils.getDayOfYear();
			if(nowTime - lastTime >= 7){ //每周检测更新
				SdkUtils.checkUpdate(context);
				
				sp.edit()
				.putInt(KEY_LAST_UPDATE_TIME, nowTime)
				.commit();
			}
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
		Emulator emulator = Emulator.getInstance();
		
		if (key.equals("scalingMode")) {
			setScaleMode(sp.getString("scalingMode", DEF_SCALE_MOD));
		} else if (key.equals("enableApilog")) {
			boolean b = sp.getBoolean(key, true);
			emulator.native_setIntOptions(key, b ? 1 : 0);
			if (b) {
				for (String s : logs) {
					emulator.native_setIntOptions(s, sp.getBoolean(s, false) ? 1 : 0);
				}
			}
		} else if (key.equals("enableSound")) {
			enableSound = sp.getBoolean(key, true);
		} else if (key.equals("volume")) {
			volume = sp.getInt("volume", 100);
		} else if (key.equals("orientation")) {

		} else if (key.equals("theme")) {

		} else if (key.equals("thread")) {
			emulator.setThreadMod(Integer.valueOf(sp.getString(key, "0")));
		} else if (key.equals(KEY_SHOW_STATUSBAR)) {
			showStatusBar = sp.getBoolean(key, false);
		}  else if (key.equals("showMemInfo")) {
			showMemInfo = sp.getBoolean(key, false);
		} else if (key.equals("noKey")) {
			noKey = sp.getBoolean(key, false);
		} else if (key.equals("dpadAtLeft")) {
			dpadAtLeft = sp.getBoolean(key, false);
		} else if (key.equals("fullScnEditor")) {
			fullScnEditor = sp.getBoolean(key, false);
		} else if (key.equals(KEY_SDCARD_PATH)) {
			EmuPath.getInstance().setSDPath(sp.getString(KEY_SDCARD_PATH, EmuPath.DEF_SD_PATH));
		} else if (key.equals(KEY_MYTHROAD_PATH)) {
			EmuPath.getInstance().setMythroadPath(sp.getString(KEY_MYTHROAD_PATH, EmuPath.DEF_MYTHROAD_DIR));
		} else if (key.equals(KEY_SCN_SIZE)) {
			MrpScreen.parseScreenSize(sp.getString(key, "240x320"));
			
			if(differentPath){
				EmuPath.getInstance().setMythroadPath( EmuPath.DEF_MYTHROAD_DIR + MrpScreen.getSizeTag() + "/");
			}
		} else if (key.equals("showFloatButton")) {
			showFloatButton = sp.getBoolean(key, false);
		} else if (key.equals("catchVolumekey")) {
			catchVolumekey = sp.getBoolean("catchVolumekey", B_DEF_CATCH_VOLUME_KEY);
		} else if (key.equals("platform")) {
//			emulator.setIntOptions(key, Integer.valueOf(sp.getString(key, "12")));
		} else if (key.equals(KEY_MEM_SIZE)) {
			emulator.native_setIntOptions(key, Integer.valueOf(sp.getString(key, DEF_MEM_SIZE)));
		} else if (key.equals(KEY_MULTI_PATH)) {
			differentPath = sp.getBoolean(key, B_DEF_MULTI_PATH);
			
			if(differentPath){
				EmuPath.getInstance().setMythroadPath( EmuPath.DEF_MYTHROAD_DIR + MrpScreen.getSizeTag() + "/");
			}else {
				EmuPath.getInstance().setMythroadPath(sp.getString(KEY_MYTHROAD_PATH, EmuPath.DEF_MYTHROAD_DIR));
			}
		} else if (key.equals(KEY_AUTO_UPDATE)) {
			autoUpdate = sp.getBoolean(KEY_AUTO_UPDATE, true);
		} else if (key.equals(KEY_LAST_UPDATE_TIME)) {
			
		} else if (key.equals(KEY_LIMIT_INPUT_LENGTH)) {
			limitInputLength = sp.getBoolean(KEY_LIMIT_INPUT_LENGTH, false);
		} else if (key.equals(KEY_EXRAM)) {
			emulator.native_setIntOptions(KEY_EXRAM, sp.getBoolean(key, false) ? 1 : 0);
		} else if (key.equals(KEY_ENABLE_KEY_VIRB)) {
			enableKeyVirb = sp.getBoolean(KEY_ENABLE_KEY_VIRB, true);
		}else {
			//应该全部改为使用 native_setStringOptions() 这样可以避免转型失败
			emulator.native_setIntOptions(key, sp.getBoolean(key, true) ? 1 : 0);
		}
	}
	
	public static void setVmMemSize(int size) {
		Emulator.getInstance().native_setIntOptions("memSize", size);
	}
	
	public static void setPlatDrawChar(boolean b) {
		Emulator.getInstance().native_setIntOptions("platdrawchar", b? 1 : 0);
	}
	
	///////////////////////////////////////////////////////
	public static boolean enableKeyVirb = true;
	public static boolean fullScnEditor = false;
	public static boolean catchVolumekey = false;
	public static boolean dpadAtLeft = false;
	public static boolean autoUpdate = false;
	public static boolean notHintAdvSet = false;
	public static int lrbX, lrbY;
	public static boolean enableAntiAtial = true;
	public static int screenOrientation;
	public static boolean enableSound;
	public static boolean showStatusBar;
	public static boolean noKey;
	public static boolean showMemInfo;
	public static boolean showFloatButton;
	public static boolean showMenu = true;
	public static boolean limitInputLength = true;
	/**
	 * 不同分辨率在不同目录下运行
	 * 
	 * <p>如果为 true：
	 * 		1.ROOT_DIR = DEF_ROOT_DIR + 分辨率 </p>
	 * 
	 * <p>如果为 false:
	 * 		1.ROOT_DIR</p>
	 */
	public static boolean differentPath;
	public static int volume;

	/**
	 * 状态栏高度
	 */
	public static int statusBarHeight;
	
	/**
	 * 键盘透明度
	 */
	public static int keypadOpacity;
	public static int THEME;
	

	public static void setScaleMode(String mode) {
		MrpScreen.parseScaleMode(mode);
	}

	public static void setMrpscnType(String type) {
		MrpScreen.parseScreenSize(type);
	}
	
	/**
	 * 模拟器主界面是否显示菜单
	 */
	public static void setShowMenu(boolean showMenu) {
		Prefer.showMenu = showMenu;
	}
	
	public static final String KEY_MYTHROAD_PATH = "mythroadPath";
	public static final String KEY_SDCARD_PATH = "sdpath";
	public static final String KEY_ENABLE_KEY_VIRB = "enableKeyVirb";
	public static final String KEY_MEM_SIZE= "memSize";
	public static final String KEY_EXRAM= "enableExram";
	public static final String KEY_SCALING_MODE= "scalingMode";
	public static final String KEY_SCN_SIZE= "screensize";
	public static final String KEY_AUTO_UPDATE= "autoUpdate";
	public static final String KEY_SHOW_STATUSBAR= "showStatusBar";
	public static final String KEY_LAST_UPDATE_TIME = "lastUpdateTime";
	public static final String KEY_MULTI_PATH= "runUnderMultiPath";
	public static final String KEY_LIMIT_INPUT_LENGTH = "limitInputLength";
}
