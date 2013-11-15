package com.mrpoid.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Looper;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.mrpoid.R;
import com.mrpoid.app.EmulatorActivity;
import com.mrpoid.services.SmsUtil;



/**
 * 2012/10/9
 * 
 * @author JianbinZhu
 * 
 */
public class Emulator implements Callback {
	public static final String TAG = "Emulator";
	
	private static Emulator instance;

	private Context context; 
	public EmulatorView emulatorView;  
	public EmulatorActivity emulatorActivity;
	
	/**
	 * 运行在 mrp 线程的handle
	 */
	private Handler handler;
	private EmuAudio audio;
	private MrpScreen screen;
	private MrpFile runMrpFile;
	private String runMrpPath;
	private Keypad mKeypad;
	
	private int threadMod = THREAD_MAIN;
	private boolean running;
	private boolean bInited;
	
	public int N2J_charW, N2J_charH; //这2个值保存 每次measure的结果，底层通过获取这2个值来获取尺寸
	public int N2J_memLen, N2J_memLeft, N2J_memTop;
	
	
	private static boolean soLoded = false;
	
	private int state;
	public static final int STA_NOR = 0, 
					 STA_SHOWEDIT = 1,
					 STA_PAUSE = 2;
	
	private static final int MSG_TIMER_OUT = 0x0001,
		MSG_CALLBACK = 0x0002,
		MSG_MR_SMS_GET_SC = 0x0003;
	
	
	static {
		System.loadLibrary("mrpoid"); 
	}
	
	private Emulator() {
	}

	/**
	 * 用在没有上下文对象的地方获取 模拟器实例，调用此方法的时候，模拟器必须已经初始化
	 * 
	 * @return 模拟器实例
	 */
	public static Emulator getInstance(){
		if(instance == null)
			instance = new Emulator();
		
		return instance;
	}
	
	/**
	 * 销毁实例
	 * 
	 * native 层也将是释放，应该在整个程序退出时调用
	 */
	public static void releaseInstance() {
		if (instance != null) {
			instance.native_destroy();
			instance.audio.dispose();
			instance.screen.dispose();
			instance = null;
		}
	}
	
	/**
	 * 启动虚拟机并运行 mrp
	 * 
	 * @param context
	 * @param mrpPath 要运行的 mrp 路径，相对于 mythroad
	 * @param mrpFile MrpFile 实例
	 */
	public static void startMrp(Context context, String mrpPath, MrpFile mrpFile) {
		EmuLog.i(TAG, "startMrp(" + mrpPath + ", " + mrpFile + ")");
		
		Emulator emulator = getInstance();
		if(emulator != null) {
			if(!emulator.isInited())
				emulator.init(context);
			emulator.setRunMrp(mrpPath, mrpFile);;
			Intent intent = new Intent(context, EmulatorActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(intent);
		}
	}
	
	/////////////////////////////////////////////////////////
	public Emulator init(final Context context) {
		this.context = context.getApplicationContext();
		
		synchronized (this) {
			if(bInited)
				return this;
			
			screen = new MrpScreen(this);
			audio = new EmuAudio(context, this);
			try {
				EmuLog.i(TAG, "call native_create tid=" + Thread.currentThread().getId());
				native_create(screen, audio);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			//起线程获取短信中心
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
		
		return this;
	}
	
	public boolean isInited() {
		return bInited;
	}
	
	public void setEmulatorActivity(EmulatorActivity emulatorActivity) {
		this.emulatorActivity = emulatorActivity;
	}
	
	public EmulatorActivity getEmulatorActivity() {
		return emulatorActivity;
	}
	
	public void setEmulatorView(EmulatorView emulatorView) {
		this.emulatorView = emulatorView;
	}
	
	public EmulatorView getEmulatorView() {
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
	public void setRunMrp(String path, MrpFile file) {
		this.runMrpPath = path;
		if(file == null){
			runMrpFile = new MrpFile(new File(runMrpPath));
			runMrpFile.setAppName("冒泡社区");
		}
		this.runMrpFile = file;
	}
	
	public String getCurMrpPath() {
		return runMrpPath;
	}
	
	public MrpFile getCurMrpFile() {
		if(runMrpFile == null){
			runMrpFile = new MrpFile("安卓冒泡");
		}
		return runMrpFile;
	}
	
	public boolean isRunning() {
		return running;
	}
	
	public static final int THREAD_MAIN=0,
//		THREAD_JAVA=1,
		THREAD_NATIVE=2;
	
	public void setThreadMod(int threadMod) {
		this.threadMod = threadMod;
	}
	
	public boolean isNativeThread() {
		return threadMod == THREAD_NATIVE;
	}
	
	@Override
	public boolean handleMessage(Message msg) {
//		EmuLog.i(TAG, "emu handle message: " + msg.what);
		
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
	 * 启动 mrp 虚拟机
	 */
	public void start() {
		if(runMrpPath == null){
			EmuLog.e(TAG, "no run file!");
			return;
		}
		
		if(emulatorActivity == null || emulatorView == null){
			Toast.makeText(context, "运行环境错误，即将退出！", Toast.LENGTH_SHORT).show();
			
			if(emulatorActivity != null)
				emulatorActivity.finish();
			else
				android.os.Process.killProcess(android.os.Process.myPid());
			
			return;
//			throw new RuntimeException("emulator run Context not set!");
		}
		
		EmuLog.i(TAG, "start");
		
		Prefer.getInstance().init(emulatorActivity);
		
		audio.init();
		screen.init();
		timer = new Timer();
		handler = new Handler(this);

		//等所有环境准备好后再确定它为运行状态
		running = true;
		
		String path = runMrpPath;
		if(path.charAt(0) != '*'){ //非固化应用
			path = "%" + runMrpPath;
		}

		if(threadMod == THREAD_NATIVE){
			vm_loadMrp_thread(path);
		} else {
			vm_loadMrp(path);
		}
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
		state = STA_PAUSE;
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
		if(threadMod == THREAD_NATIVE){ 
			vm_exit_foce();
		} else {
			stop();
//			android.os.Process.killProcess(android.os.Process.myPid());
		}
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

	private Timer timer;
	private TimerTask task; 

	private void N2J_timerStart(short t) {
		if (!running)
			return;

		task = new TimerTask() {
			@Override
			public void run() {
				handler.sendEmptyMessage(MSG_TIMER_OUT);
			}
		};
		timer.schedule(task, t);
	}
	
	private Runnable timeRunnable = new Runnable() {
		@Override
		public void run() {
			vm_timeOut();
		}
	};
	
	private void N2J_timerStop() {
		if(!running) return;
		
		if(task != null){
			task.cancel();
			task = null;
		}
		if(timer != null) {
			timer.purge();
		}
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
		if(threadMod != THREAD_MAIN){
			emulatorActivity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					emulatorActivity.createEdit(title, content, type, max);
				}
			});
		}else {
			emulatorActivity.createEdit(title, content, type, max);
		}
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
