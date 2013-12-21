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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Looper;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.mrpoid.R;
import com.mrpoid.app.EmulatorActivity;
import com.mrpoid.app.EmulatorSurface;
import com.mrpoid.gui.keypad.Keypad;
import com.mrpoid.utils.SmsUtil;
import com.yichou.common.utils.FileUtils;



/**
 * 2012/10/9
 * 
 * @author JianbinZhu
 * 
 */
public class Emulator implements Callback {
	public static final String TAG = "Emulator";
	
	public static final String SDCARD_ROOT = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separatorChar;
	public static final String DEF_WORK_PATH = "mythroad/";
	public static final String PUBLIC_STROAGE_PATH = SDCARD_ROOT + "Mrpoid/";
	public static final int DEF_MIN_SDCARD_SACE_MB = 8;
	
	private static final int MSG_TIMER_OUT = 0x0001,
		MSG_CALLBACK = 0x0002,
		MSG_MR_SMS_GET_SC = 0x0003;

	private Context context; 
	private EmulatorSurface emulatorView;  
	private EmulatorActivity emulatorActivity;
	private Handler handler;
	private EmuAudio audio;
	private MrpScreen screen;
	private String runMrpPath;
	private Keypad mKeypad;
	private boolean running;
	private boolean bInited;
	private Timer timer;
	private TimerTask task; 
	
	
	/**
	 * end with /
	 */
	private String mVmRoot = SDCARD_ROOT;
	private String mLastVmRoot = SDCARD_ROOT;
	
	/**
	 * end with /
	 */
	private String mWorkPath = DEF_WORK_PATH;
	private String mLastWorkPath = DEF_WORK_PATH;
	
	private final List<OnPathChangeListener> pathListeners = new ArrayList<OnPathChangeListener>();
	
	//--- native params below --------
	public int N2J_charW, N2J_charH; //这2个值保存 每次measure的结果，底层通过获取这2个值来获取尺寸
	public int N2J_memLen, N2J_memLeft, N2J_memTop;
	
	
	static {
		System.loadLibrary("mrpoid"); 
	}
	
	private static final Emulator instance = new Emulator();
	public static Emulator getInstance(){
		return instance;
	}

	private Emulator() {
	}

	/**
	 * memory recyle when exit
	 */
	public synchronized void recyle() {
		native_destroy();
		audio.recyle();
		screen.recyle();
		bInited = false;
	}
	
	private synchronized void checkInit() {
		if (bInited) return;
		
		screen = new MrpScreen(this);
		audio = new EmuAudio(context, this);
		try {
			EmuLog.i(TAG, "call native_create tid=" + Thread.currentThread().getId());
			native_create(screen, audio);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// 起线程获取短信中心
		new Thread(new Runnable() {
			@Override
			public void run() {
				String N2J_smsCenter = SmsUtil.getSmsCenter(context);
				native_setStringOptions("smsCenter", N2J_smsCenter);
				EmuLog.i(TAG, "smsCenter: " + N2J_smsCenter);
			}
		}).start();
		
		bInited = true;
	}
	
	public boolean isInited() {
		return bInited;
	}
	
	public void attachApplicationContext(Context context) {
		this.context = context.getApplicationContext();
		checkInit();
	}
	
	public void attachActivity(EmulatorActivity emulatorActivity) {
		attachApplicationContext(emulatorActivity);
		this.emulatorActivity = emulatorActivity;
	}
	
	public EmulatorActivity getActivity() {
		return emulatorActivity;
	}
	
	public void attachSurface(EmulatorSurface emulatorView) {
		this.emulatorView = emulatorView;
	}
	
	public EmulatorSurface getSurface() {
		return emulatorView;
	}
	
	public MrpScreen getScreen() {
		return screen;
	}
	
	public EmuAudio getAudio() {
		return audio;
	}
	
	public Context getContext() {
		return context;
	}
	
	public void setKeypad(Keypad keypad) {
		this.mKeypad = keypad;
	}
	
	public Keypad getKeypad() {
		return mKeypad;
	}
	
	/**
	 * 设置即将运行的 MRP
	 * @param runMrpPath
	 */
	public void setRunMrp(String path) {
		if(path == null)
			throw new RuntimeException("path can't be null!");
		
		System.out.println("inputPah=" + path);
		
		//1.是不是绝对路径
		if(path.startsWith(SDCARD_ROOT)) {
			final int i = path.indexOf(getVmWorkPath());
			if(i != -1) {
				int l = getVmWorkPath().length();
				path = path.substring(i + l);
				
				System.out.println("newPath=" + path);
			} else { //需要复制
				final int j = path.lastIndexOf(File.separatorChar);
				final String vpath = path.substring(j+1);

				File dstFile = getVmFullFilePath(vpath);
				
				System.out.println("复制文件： " + path + " to " + dstFile);
				
				if (FileUtils.FAILED == FileUtils.copyTo(dstFile, new File(path))) {
					throw new RuntimeException("file path invalid, copy file to VmWorkPath fail!");
				}
				
				path = vpath;
			}
		}
		
		System.out.println("final path=" + path);
		
		this.runMrpPath = path;
	}
	
	public String getCurMrpPath() {
		return runMrpPath;
	}

	public String getCurMrpAppName() {
		return native_getAppName(getVmFullPath() + runMrpPath);
	}
	
	public boolean isRunning() {
		return running;
	}
	
	@Override
	public boolean handleMessage(Message msg) {
		switch (msg.what) {
		case MSG_TIMER_OUT:
			vm_timeOut();
			break;
			
		case MSG_CALLBACK:
			native_callback(msg.arg1, msg.arg2);
			return true;
			
		case MSG_MR_SMS_GET_SC:
			vm_event(MrDefines.MR_SMS_GET_SC, 0, 0); //获取不到，暂时返回都是0
			return true;
			
		default:
			return (1 == native_handleMessage(msg.what, msg.arg1, msg.arg2));
		}
		
		return true;
	}
	
	/**
	 * after EmulatorSurface has created 启动 mrp 虚拟机
	 */
	public void start() {
		if(runMrpPath == null){
			EmuLog.e(TAG, "no run file!");
			return;
		}
		
		EmuLog.i(TAG, "start");
		
		screen.init();
		timer = new Timer();
		handler = new Handler(this);

		//等所有环境准备好后再确定它为运行状态
		running = true;
		
		String path = runMrpPath;
		if(path.charAt(0) != '*'){ //非固化应用
			path = "%" + runMrpPath;
		}

		vm_loadMrp(path);
	}
	
	/**
	 * 停止 MRP 虚拟机
	 */
	public void stop() {
		EmuLog.i(TAG, "stop");
		
		running = false;
		vm_exit();
	}
	
	public void pause() {
		audio.pause();
		screen.pause();
		native_pause();
	}
	
	public void resume() {
		audio.resume();
		screen.resume();
		native_resume();
	}
	
	/**
	 * 强制停止 MRP 虚拟机
	 */
	public void stopFoce() {
		stop();
	}
	
	/**
	 * native vm 结束后 调用方法结束 java 层
	 * 
	 * 注：不能在这里杀掉进程，否则底层释放工作未完成
	 */
	private void N2J_finish(){
		EmuLog.d(TAG, "N2J_finish() called");
		
		if(!running) return;
		
		running = false;
		handler.removeCallbacksAndMessages(null);
		handler = null;
		
		//下面这些都线程安全吗？
		N2J_timerStop();
		
		if(timer != null){
			timer.cancel();
			timer = null;
		}
		
		audio.stop();
		audio.recyle();
		screen.freeRes();
		screen.recyle();
		
		emulatorActivity.finish();
		
		emulatorActivity = null;
		emulatorView = null;
	}
	
	///////////////////////////////////////////////
	private void N2J_flush() {
		if(!running) return;
		
		emulatorView.flush();
	}
	
	private final class MrTimerTask extends TimerTask {
		@Override
		public void run() {
			handler.sendEmptyMessage(MSG_TIMER_OUT);
		}
	}

	private void N2J_timerStart(short t) {
		if (!running)
			return;

		task = new MrTimerTask();
		timer.schedule(task, t);
	}
	
	private void N2J_timerStop() {
		if(!running) return;
		
		if(task != null){
			task.cancel();
			task = null;
		}
		timer.purge();
	}
	
	/**
	 * 调用 mr_event
	 * 
	 * @param p0
	 * @param p1
	 * @param p2
	 */
	public void postMrpEvent(int p0, int p1, int p2) {
		if(!running) return;

		vm_event(p0, p1, p2);
	}
	
	//////////// 编辑框接口  ////////////
	private String N2J_editInputContent;
	
	/// 底层访问方法 ////////////////////////////////////////
	public void setEditInputContent(String editInputContent) {
		this.N2J_editInputContent = editInputContent;
	}
	
	/**
	 * 底层调用显示编辑框
	 * 
	 * @param title 标题
	 * @param content 内容
	 * @param type 类型
	 * @param max 最大值
	 */
	private void N2J_showEdit(final String title, final String content, final int type, final int max) {
		emulatorActivity.createEdit(title, content, type, max);
	}
	
	/**
	 * 底层获取一个 int 型参数
	 * 
	 * @param name 键值
	 * @return 参数值
	 */
	private int N2J_getIntSysinfo(String name) {
		//native 上调的函数一定要检查空指针，否则将导致致命错误
		if(name == null || context == null)
			return 0;
		
//		EmuLog.i(TAG, "getIntSysinfo("+name+")");
		
		if (name.equalsIgnoreCase("netType")) {
			return EmuUtils.getNetworkType(context);
		}else if (name.equalsIgnoreCase("netID")) {
			return EmuUtils.getNetworkID(context);
		}
		
		return 0;
	}
	
	/**
	 * 底层调用获取一个 String 类型的参数
	 * 
	 * @param name 键值
	 * @return 成功：返回获取到的参数 失败：返回 null
	 */
	private String N2J_getStringSysinfo(String name) {
		//native 上调的函数一定要检查空指针，否则将导致致命错误
		if(name == null || context == null)
			return null;
		
		if (name.equalsIgnoreCase("imei")) {
			TelephonyManager mTm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);  
            return mTm.getDeviceId();  
		}else if (name.equalsIgnoreCase("imsi")) {
			TelephonyManager mTm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);  
			return mTm.getSubscriberId();  
		}else if (name.equalsIgnoreCase("phone-model")) {
			return android.os.Build.MODEL; // 手机型号  
		}else if (name.equalsIgnoreCase("phone-num")) {
			TelephonyManager mTm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);  
			return mTm.getLine1Number(); // 手机号码，有的可得，有的不可得
		}
		
		return null;
	}
	
	/**
	 * 其他线程请求mrp线程执行一个回调（线程同步）
	 * 
	 * 注意：来自异步线程
	 * 
	 * @param what 标志
	 * @param param 参数
	 */
	private void N2J_requestCallback(final int what, final int param){
		if(!running) return;
		
		EmuLog.d(TAG, "N2J_requestCallback java pid " + Thread.currentThread().getId());
		
		handler.obtainMessage(MSG_CALLBACK, what, param).sendToTarget();
	}
	
	/**
	 * 底层请求显示一个对话框，该对话框弹出，表明底层出现了不可继续下去的错误，
	 * 需要退出 MRP 运行
	 * 
	 * @param msg
	 */
	private void N2J_showDlg(String msg){
		new AlertDialog.Builder(emulatorActivity) //注意这里不是用 Context 
				.setTitle(R.string.warn)
				.setMessage(msg)
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						stop(); //结束运行
					}
				})
				.create()
				.show();
	}
	
	/**
	 * 底层调用发送短信
	 * 
	 * @param num 号码
	 * @param content 内容
	 * @param showReport 显示发送报告
	 * @param showRet 显示发送结果
	 * 
	 * @return 0
	 */
	private int N2J_sendSms(String num, String msg, boolean showReport, boolean showRet) {
		EmuLog.i(TAG, "N2J_sendSms: " + num + ", " + msg);
			
		emulatorActivity.reqSendSms(msg, num);
		
		return 0;
	}
	
	/**
	 * 底层调用获取主机地址（有些手机貌似不行）
	 * 
	 * @param host 主机名
	 */
	private void N2J_getHostByName(final String host) {
		EmuLog.i(TAG, "N2J_getHostByName:" + host);
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					InetAddress[] addresses = InetAddress.getAllByName(host);
					if (addresses != null) {
						byte[] ba = addresses[0].getAddress();
						int ip = 0;
						for(int i=0; i<4; ++i){
							ip += (ba[i] & 0xff) << (8*(3-i));
						}
						N2J_requestCallback(0x1002, ip);
					}
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
	
	/**
	 * 底层调用设置参数（int类型）
	 * 
	 * @param key 键值
	 * @param value 参数
	 */
	private void N2J_setOptions(String key, String value) {
		if(key == null) return;
		
		EmuLog.i(TAG, "N2J_setOptions(" + key + ", " + value + ")");
		
		if(key.equalsIgnoreCase("keepScreenOn")){
			emulatorView.setKeepScreenOn(Boolean.valueOf(value));
		}
	}
	
	/**
	 * 从 assets 读取tsf字体文件
	 * 
	 * @return 成功：字节数组 失败：null
	 */
	private byte[] N2J_readTsfFont(){
		InputStream is = null;
		byte[] buf = null;
		
		try {
			is = context.getAssets().open("fonts/font16.tsf");
			buf = new byte[is.available()];
			is.read(buf);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return buf;
	}
	
	/**
	 * 底层调用万能方法
	 * 
	 * @param args
	 */
	private void N2J_callVoidMethod(String[] args) {
		if(null == args)
			return;
		
		int argc = args.length;
		if (argc >= 1) {
			String action = args[0];
			
			if(action == null)
				return;
			
			if(action.equals("call")){
				if(argc >= 2 && args[1] != null){
					emulatorActivity.reqCallPhone(args[1]);
				}
			}else if (action.equals("viewUrl")) {
				if(argc >= 2 && args[1] != null){
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://" + args[1]));
					emulatorActivity.startActivity(intent);
				}
			}else if (action.equals("getSmsCenter")) { //获取短信中心，通过 mr_event 回调
				handler.sendEmptyMessageDelayed(MSG_MR_SMS_GET_SC, 500);
			}else if ("showToast".equals(action)) {
				Toast.makeText(emulatorActivity, args[1], Toast.LENGTH_SHORT).show();
			}else if ("crash".equals(action)) {
				new Thread() {
					@Override
					public void run() {
						Looper.prepare();
						Toast.makeText(context, "~~~~(>_<)~~~~ 又崩溃了，即将退出！", Toast.LENGTH_LONG).show();
						Looper.loop();
					}
				}.start();
			}
		}
	}
	
	private void N2J_sendMessage(int what, int p0, int p1, int delay) {
		EmuLog.i(TAG, "n2j sengMessage " + what);
		
		handler.sendMessageDelayed(handler.obtainMessage(what, p0, p1), delay);
	}
	
	
	//-----------------------------------------------------------
	public static interface OnPathChangeListener {
		public void onPathChanged(String newPath, String oldPath);
	}
	
	public void initPath() {
		if(!FileUtils.isSDAvailable(DEF_MIN_SDCARD_SACE_MB)) {
			Prefer.usePrivateDir = true;
//			Toast.makeText(context, "没有SD卡！", Toast.LENGTH_SHORT).show();
		}
		
		if (Prefer.usePrivateDir) { // 使用私有目录
			setVmRootPath(Prefer.privateDir);
		} else {
			setVmRootPath(Prefer.sdPath);
		}
		
		if (Prefer.differentPath) { // 不同路径
			setVmWorkPath(DEF_WORK_PATH + MrpScreen.getSizeTag() + "/");
		} else {
			setVmWorkPath(Prefer.mythoadPath);
		}
		
		EmuLog.i(TAG, "sd path = " + mVmRoot);
		EmuLog.i(TAG, "mythroad path = " + mWorkPath);
	}
	
	public void addOnPathChangeListener(OnPathChangeListener l) {
		pathListeners.add(l);
	}
	
	public void removeOnPathChangeListener(OnPathChangeListener l) {
		pathListeners.remove(l);
	}
	
	private void notifyListeners() {
		for(OnPathChangeListener l : pathListeners){
			l.onPathChanged(mVmRoot + mWorkPath, mLastVmRoot + mLastWorkPath);
		}
	}
	
	/**
	 * 获取默认运行根目录的完整路径
	 * 
	 * @return 绝对路径 /结尾
	 */
	public String getVmDefaultFullPath() {
		return (SDCARD_ROOT + DEF_WORK_PATH);
	}
	
	/**
	 * 获取运行根目录的完整路径
	 * 
	 * @return 绝对路径 /结尾
	 */
	public String getVmFullPath() {
		return (mVmRoot + mWorkPath);
	}
	
	/**
	 * 获取上一次成功改变，运行根目录的绝对路径
	 * 
	 * @return 绝对路径 /结尾
	 */
	public String getVmLastFullPath() {
		return (mLastVmRoot + mLastWorkPath);
	}
	
	/**
	 * 获取运行目录下的一个文件
	 * 
	 * @param name 文件名
	 * 
	 * @return
	 */
	public File getVmFullFilePath(String name) {
		return new File(mVmRoot + mWorkPath, name);
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
	public void setVmRootPath(String tmp) {
		if (tmp == null || tmp.length() == 0) {
			EmuLog.e(TAG, "setSDPath: input error!");
			return;
		}
		
		if(mVmRoot.equals(tmp))
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
			tmp += '/';
		}
		
		mLastVmRoot = mVmRoot;
		mVmRoot = tmp;
		Emulator.getInstance().native_setStringOptions("sdpath", mVmRoot);
		
		notifyListeners();

		EmuLog.i(TAG, "sd path has change to: " + mVmRoot);
	}
	
	public String getVmRootPath() {
		return mVmRoot;
	}
	
	/**
	 * 理论上 mythroad 路径可以为 "" 表示SD卡根目录，这里为了避免麻烦，还是让他不可以
	 * 
	 * @param tmp
	 */
	public void setVmWorkPath(String tmp) {
		if (tmp == null || tmp.length() == 0) {
			EmuLog.e(TAG, "setMythroadPath: input error!");
			return;
		}
		
		if(mWorkPath.equals(tmp))
			return;
		
		File path = new File(mVmRoot, tmp);
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
		mLastWorkPath = mWorkPath;
		mWorkPath = tmp;
		Emulator.getInstance().native_setStringOptions("mythroadPath", mWorkPath);

		notifyListeners();

		EmuLog.i(TAG, "mythroad path has change to: " + mWorkPath);
	}
	
	public String getVmWorkPath(){
		return mWorkPath;
	}
	
	/// ////////////////////////////////////////
	public native void native_create(MrpScreen mrpScreen, EmuAudio emuAudio);
	public native void native_pause();
	public native void native_resume();
	public native void native_destroy();
	public native void native_getMemoryInfo();
	public native String native_getStringOptions(String key);
	public native void native_setStringOptions(String key, String value);
	public native void native_setIntOptions(String key, int value);
	public static native String native_getAppName(String path);
	public native void native_callback(int what, int param);
	public native int native_handleMessage(int what, int p0, int p1);
	
	//---- VM -----------------------
	public native int vm_loadMrp(String path);
	public native int vm_loadMrp_thread(String path);
	public native void vm_pause();
	public native void vm_resume();
	public native void vm_exit();
	public native void vm_exit_foce();
	public native void vm_timeOut();
	public native void vm_event(int code, int p0, int p1);
	public static native int vm_smsIndiaction(String pContent, String pNum);
	public native int vm_newSIMInd(int type, byte[] old_IMSI);
	public native int vm_registerAPP(byte[] p, int len, int index);
	
	
	///////////////////////////////////
	
	public native void hello();
}
