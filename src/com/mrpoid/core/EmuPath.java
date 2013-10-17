package com.mrpoid.core;

import java.io.File;
import java.util.ArrayList;

import android.os.Environment;

import com.yichou.common.utils.FileUtils;

/**
 * 路径管理
 * 
 * @author Yichou
 */
public final class EmuPath {
	public static interface OnPathChangeListener {
		public void onPathChanged(String newPath, String oldPath);
	}
	
	public static final String TAG = "EmuPath";
	
	public static final String DEF_SD_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
	public static final String DEF_MYTHROAD_DIR = "mythroad/";
	public static final String PUBLIC_STROAGE_PATH = DEF_SD_PATH + "Mrpoid/";
	
	
	private String SD_PATH = DEF_SD_PATH;
	private String LAST_SD_PATH = DEF_SD_PATH;
	
	private String ROOT_DIR = DEF_MYTHROAD_DIR;
	private String LAST_ROOT_DIR = DEF_MYTHROAD_DIR;
	
	private ArrayList<OnPathChangeListener> listeners;
	
	private static EmuPath instance;
	
	
	private static final class InstanceHolder {
		static final EmuPath INSTANCE = new EmuPath();
	}
	
	public static EmuPath getInstance() {
		if(instance == null){
			instance = InstanceHolder.INSTANCE;
		}

		return instance;
	}
	
	public void init() {
		if(!FileUtils.isSDMounted()) {
			Prefer.usePrivateDir = true;
//			Toast.makeText(context, "没有SD卡！", Toast.LENGTH_SHORT).show();
		}
		
		if(Prefer.usePrivateDir) { //使用私有目录
			setSDPath(Prefer.privateDir);
		}else {
			setSDPath(Prefer.sdPath);
		}
		
		if(Prefer.differentPath) { //不同路径
			setMythroadPath(DEF_MYTHROAD_DIR + MrpScreen.getSizeTag() + "/");
		} else {
			setMythroadPath(Prefer.mythoadPath);
		}
		
		EmuLog.i(TAG, "sd path = " + SD_PATH);
		EmuLog.i(TAG, "mythroad path = " + ROOT_DIR);
	}
	
	private EmuPath(){
		listeners = new ArrayList<EmuPath.OnPathChangeListener>(3);
	}
	
	public void addOnPathChangeListener(OnPathChangeListener l) {
		listeners.add(l);
	}
	
	public void removeOnPathChangeListener(OnPathChangeListener l) {
		listeners.remove(l);
	}
	
	private void notifyListeners() {
		for(OnPathChangeListener l : listeners){
			l.onPathChanged(SD_PATH + ROOT_DIR, LAST_SD_PATH + LAST_ROOT_DIR);
		}
	}
	
	/**
	 * 获取默认运行根目录的完整路径
	 * 
	 * @return 绝对路径 /结尾
	 */
	public String getDefFullPath() {
		return (DEF_SD_PATH + DEF_MYTHROAD_DIR);
	}
	
	/**
	 * 获取运行根目录的完整路径
	 * 
	 * @return 绝对路径 /结尾
	 */
	public String getFullPath() {
		return (SD_PATH + ROOT_DIR);
	}
	
	/**
	 * 获取上一次成功改变，运行根目录的绝对路径
	 * 
	 * @return 绝对路径 /结尾
	 */
	public String getLastFullPath() {
		return (LAST_SD_PATH + LAST_ROOT_DIR);
	}
	
	/**
	 * 获取运行目录下的一个文件
	 * 
	 * @param name 文件名
	 * 
	 * @return
	 */
	public File getFullFilePath(String name) {
		return new File(SD_PATH + ROOT_DIR, name);
	}
	
	/**
	 * 获取公共目录下存储的文件
	 * 
	 * @param name
	 * @return
	 */
	public static File getPublicFilePath(String name){
		File file = new File(PUBLIC_STROAGE_PATH);
		FileUtils.createDir(file);
		return new File(PUBLIC_STROAGE_PATH, name);
	}
	
	/**
	 * SD卡根目录，该目录必须可以创建
	 * 
	 * @param tmp
	 */
	public void setSDPath(String tmp) {
		if (tmp == null || tmp.length() == 0) {
			EmuLog.e(TAG, "setSDPath: input error!");
			return;
		}
		
		if(SD_PATH.equals(tmp))
			return;
		
		File path = new File(tmp);
		if (FileUtils.SUCCESS != FileUtils.createDir(path)) {
			EmuLog.e(TAG, "setSDPath: " + path.getAbsolutePath() + " mkdirs FAIL!");
			return;
		}
		
		if(!path.canRead() || !path.canWrite()) {
			EmuLog.e(TAG, "setSDPath: " + path.getAbsolutePath() + " can't read or write!");
			return;
		}
		
		int i = tmp.length();
		if(tmp.charAt(i-1) != '/'){
			tmp += "/";
		}
		
		LAST_SD_PATH = SD_PATH;
		SD_PATH = tmp;
		Emulator.getInstance().native_setStringOptions("sdpath", SD_PATH);
		
		notifyListeners();

		EmuLog.i(TAG, "sd path has change to: " + SD_PATH);
	}
	
	public String getSDPath() {
		return SD_PATH;
	}
	
	/**
	 * 理论上 mythroad 路径可以为 "" 表示SD卡根目录，这里为了避免麻烦，还是让他不可以
	 * 
	 * @param tmp
	 */
	public void setMythroadPath(String tmp) {
		if (tmp == null || tmp.length() == 0) {
			EmuLog.e(TAG, "setMythroadPath: input error!");
			return;
		}
		
		if(ROOT_DIR.equals(tmp))
			return;
		
		File path = new File(SD_PATH, tmp);
		if (FileUtils.SUCCESS != FileUtils.createDir(path)) {
			EmuLog.e(TAG, "setMythroadPath: " + path.getAbsolutePath() + " mkdirs FAIL!");
			return;
		}
		
		if(!path.canRead() || !path.canWrite()) {
			EmuLog.e(TAG, "setMythroadPath: " + path.getAbsolutePath() + " can't read or write!");
			return;
		}

		int i = tmp.length();
		if (tmp.charAt(i - 1) != '/') {
			tmp += "/";
		}
		LAST_ROOT_DIR = ROOT_DIR;
		ROOT_DIR = tmp;
		Emulator.getInstance().native_setStringOptions("mythroadPath", ROOT_DIR);

		notifyListeners();

		EmuLog.i(TAG, "mythroad path has change to: " + ROOT_DIR);
	}
	
	public String getMythroadPath(){
		return ROOT_DIR;
	}
}
