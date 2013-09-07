package com.mrpoid.app;

import java.io.File;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.telephony.SmsManager;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout.LayoutParams;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.mrpoid.R;
import com.mrpoid.core.EmuLog;
import com.mrpoid.core.EmuPath;
import com.mrpoid.core.EmuStatics;
import com.mrpoid.core.EmuUtils;
import com.mrpoid.core.Emulator;
import com.mrpoid.core.EmulatorView;
import com.mrpoid.core.Keypad;
import com.mrpoid.core.KeypadView;
import com.mrpoid.core.MrDefines;
import com.mrpoid.core.MrpFile;
import com.mrpoid.core.MrpScreen;
import com.mrpoid.core.Prefer;
import com.mrpoid.keysprite.ChooserFragment;
import com.mrpoid.keysprite.KeyEventListener;
import com.mrpoid.keysprite.KeySprite;
import com.mrpoid.keysprite.OnChooseLitener;
import com.mrpoid.keysprite.Sprite;
import com.yichou.common.InternalID;
import com.yichou.sdk.SdkUtils;

/**
 * @author JianbinZhu
 *
 * 创建日期：2012/10/9
 * 
 * 最后修改：2013-3-14 20:06:44
 */
public class EmulatorActivity extends FragmentActivity implements MrDefines,
		Handler.Callback, 
		OnClickListener{
	static final String TAG = "EmulatorActivity";
	
	static final String ACTION_SMS_SENT = "com.mrpej.mrpoid.SMS_SENT_ACTION";
	
	static final int MSG_ID_SHOWEDIT = 1001,
		MSG_ID_KEY_DOWN = 1002,
		MSG_ID_KEY_UP = 1003,
		MSG_ID_UPDATE_INFO_TEXT = 1004,
		
		MSG_ID_LAST = 1100;
	
	static final int INFO_TYPE_KEY_SPRITE = 1001;
	
	static final int REQ_SHOWEDIT = 1001,
		REQ_GET_IMAGE = 1002;
	
	static final int DLG_EDIT = 1001, 
		DLG_SCALE_MODE = 1002,
		DLG_PAD_ALPHA = 1003,
		DLG_TOOLS = 1004,
		
		DLG_LAST = 1103;
	
	
	private TextView tvMemory, tvInfo;
	private EmulatorView emulatorView;
	private Emulator emulator;
	public Handler handler;
	private LayoutInflater inflater;
	private SmsReceiver mSmsReceiver;
	private BroadcastReceiver mReceiver;
	private ViewGroup continer;
	private KeypadView padView;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		EmuLog.i(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		
		EmuStatics.emulatorActivity = this;
		
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);//去掉标题栏
		if(!Prefer.showStatusBar)
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		setContentView(R.layout.activity_emulator);

		handler = new Handler(this);
		inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		emulator = Emulator.getInstance(this);
		emulator.setEmulatorActivity(this);
		
		emulatorView = new EmulatorView(this);
		emulatorView.setBackgroundColor(Color.TRANSPARENT);
		continer =  (ViewGroup) findViewById(R.id.contener);
		continer.addView(emulatorView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		
		//虚拟键盘
		Keypad.loadBmp(getResources());
		padView = new KeypadView(this);
		continer.addView(padView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		
		//短信模块初始化
		smsInit();
		
		if(Prefer.showMemInfo) {
			tvMemory = new TextView(this);
			tvMemory.setGravity(Gravity.TOP|Gravity.RIGHT);
			tvMemory.setVisibility(View.GONE);
			tvMemory.setTextColor(Color.DKGRAY);
			tvMemory.setGravity(Gravity.RIGHT); //居右
			
			LayoutParams p = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			p.gravity = Gravity.RIGHT|Gravity.TOP; //顶 左
			continer.addView(tvMemory, p);

			tvMemory.setVisibility(View.VISIBLE);
		}
		
		{
//			LinearLayout layout = new LinearLayout(this);
			
			tvInfo = new TextView(this);
			tvInfo.setBackgroundColor(0x80000000);
			tvInfo.setTextColor(0xfff0f0f0);
			tvInfo.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL);
			tvInfo.setSingleLine(true);
			tvInfo.setVisibility(View.INVISIBLE);
			tvInfo.setId(R.id.tv_info);
			tvInfo.setOnClickListener(this);
			
			int padding = getResources().getDimensionPixelSize(R.dimen.dp5);
			tvInfo.setPadding(padding, padding, padding, padding);
			continer.addView(tvInfo, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		}
	}
	
	@Override
	protected void onPause() {
		EmuLog.i(TAG, "onPause");

		SdkUtils.onPause(this);
		mSmsReceiver.unRegister();
		emulator.pause();
		
		if (!isFinishing())
			backNotification2();
		
		if(Prefer.showMemInfo)
			handler.removeCallbacks(updateMemInfoRunnable);

		super.onPause();
	}
	
	@Override
	protected void onResume() {
		EmuLog.i(TAG, "onResume");

		cancelNotification();
		
		SdkUtils.onResume(this);
		emulator.resume();
		mSmsReceiver.register();
		
		if(Prefer.showMemInfo)
			startMemUpdate();
		
		super.onResume();
	}
	
	@Override
	protected void onDestroy() {
		EmuLog.i(TAG, "onDestroy");
		
		if(emulator.isRunning()){ //说明在后台运行被杀了
			EmuLog.e(TAG, "后台运行被杀！");
			SdkUtils.event(this, "beKilled", "");
		}
		
		cancelNotification();
		Keypad.releaseBmp();
		unregisterReceiver(mReceiver);
		
		EmuStatics.emulatorActivity = null;
		EmuStatics.emulatorView = null;

		super.onDestroy(); 
	}
	
	@Override
	protected void onStop() {
		EmuLog.i(TAG, "onStop");
		
		super.onStop();
	}
	
	@Override
	protected void onRestart() {
		EmuLog.i(TAG, "onRestart");
		cancelNotification();
		
		super.onRestart();
	}
	
	@Override
	protected void onStart() {
		EmuLog.i(TAG, "onStart");
		
		super.onStart();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		EmuLog.i(TAG, "onSaveInstanceState:" + outState);
		
		outState.putBoolean("hasSaved", true);
		outState.putString("curMrpPath", emulator.getCurMrpPath());
		
		super.onSaveInstanceState(outState);
	}
	
	/**
	 * 被杀后恢复
	 */
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		EmuLog.i(TAG, "onRestoreInstanceState:" + savedInstanceState);
		
		if(savedInstanceState.getBoolean("hasSaved", false)){
			String curMrpPath = savedInstanceState.getString("curMrpPath");
			if(curMrpPath != null){
				EmuLog.i(TAG, "异常恢复成功");
				
				File path = EmuPath.getInstance().getFullFilePath(curMrpPath);
				
				MrpFile mrpFile = new MrpFile(path);
				mrpFile.setAppName("冒泡社区");
				Emulator.getInstance().setRunMrp(curMrpPath, mrpFile);
			}else {
				finish();
			}
		}else {
			finish();
		}

		super.onRestoreInstanceState(savedInstanceState);
	}
	
	private void smsInit() {
		mSmsReceiver = new SmsReceiver(this);
		
		mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String message = null;
                boolean error = true;
                
                switch (getResultCode()) {
                case Activity.RESULT_OK:
                    message = "发送成功!";
                    error = false;
                    break;
                    
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    message = "失败: 未知错误.";
                    break;
                    
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    message = "失败: 短信服务不可用.";
                    break;
                    
                case SmsManager.RESULT_ERROR_NULL_PDU:
                    message = "失败: PDU 空.";
                    break;
                    
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    message = "失败: 网络错误.";
                    break;
                }
                
                //通知底层结果
                emulator.vm_event(MrDefines.MR_SMS_RESULT, error? MrDefines.MR_FAILED : MrDefines.MR_SUCCESS, 0);

                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        };
		
		// Register broadcast receivers for SMS sent and delivered intents
		registerReceiver(mReceiver, new IntentFilter(ACTION_SMS_SENT));
	}
	
	public ViewGroup getViewContiner() {
		return continer;
	}
	
	private final Runnable updateMemInfoRunnable = new Runnable() {
		
		@Override
		public void run() {
			final String memInfo = String.format(Locale.US, 
					"memoryinfo--\ntotal:%d\ntop:%d\nleft:%d", 
					emulator.N2J_memLen, emulator.N2J_memTop, emulator.N2J_memLeft);
			
			EmuLog.i(TAG, memInfo);

			emulator.native_getMemoryInfo();
			tvMemory.setText(memInfo);
			
			handler.postDelayed(this, 1000); //自循环
		}
	};
	
	private void startMemUpdate() {
		handler.postDelayed(updateMemInfoRunnable, 1000);
	}
	
	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.tv_info) {
			if(v.getTag().equals(INFO_TYPE_KEY_SPRITE)){
				stopKeySprite();
			}
		}
	}
	
	public void postUIRunable(Runnable r) {
		handler.post(r);
	}
	
	public Handler getHandler() {
		return handler;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean onMenuItemSelected(int featureId, android.view.MenuItem item) {
		if (item.getItemId() == R.id.mi_close) {
			emulator.stop();
		} else if (item.getItemId() == R.id.mi_scnshot) {
			emulator.getScreen().screenShot(this);
		} else if (item.getItemId() == R.id.mi_foce_close) {
			emulator.stopFoce();
		} else if (item.getItemId() == R.id.mi_switch_keypad) {
			padView.switchKeypad();
		} else if (item.getItemId() == R.id.mi_image) {
			Intent i = new Intent(
					Intent.ACTION_PICK,
					android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			startActivityForResult(i, REQ_GET_IMAGE);
		} else if (item.getItemId() == R.id.mi_color) {
			
		} else if (item.getItemId() == R.id.mi_scale_mode) {
			showDialog(DLG_SCALE_MODE);
		} else if (item.getItemId() == R.id.mi_keypad_opacity) {
			showDialog(DLG_PAD_ALPHA);
		} else if (item.getItemId() == R.id.mi_float_view) {
			floatView();
		} else if (item.getItemId() == R.id.mi_tools) {
			showDialog(DLG_TOOLS);
		}
		
		return true;
	}
	
	@Override
	public boolean onCreateOptionsMenu(android.view.Menu menu) { ////只会在第一次弹出的时候调用
		if(!Prefer.showMenu)
			return false;
		
		int i = 0;
		menu.add(0, R.id.mi_close, i++, R.string.close);
		menu.add(0, R.id.mi_foce_close, i++, R.string.foce_close);
		menu.add(0, R.id.mi_scnshot, i++, R.string.scnshot);
		menu.add(0, R.id.mi_switch_keypad, i++, R.string.switch_keypad);
		menu.add(0, R.id.mi_keypad_opacity, i++, R.string.pad_opacity);
//		menu.add(0, R.id.mi_float_view, i++, R.string.float_view);
		
//		SubMenu subMenu = menu.addSubMenu(0, R.id.mi_set_background, i++, R.string.set_background);
//		int j = 0;
//		subMenu.add(0, R.id.mi_color, j++, R.string.color);
//		subMenu.add(0, R.id.mi_image, j++, R.string.image);
		
		menu.add(0, R.id.mi_scale_mode, i++, R.string.scaling_mode);

		menu.add(0, R.id.mi_tools, i++, R.string.tools);
		
		return true;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		if (id == DLG_EDIT) {
			if(args == null)
				return null;
			
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			
			/**
			 * view 不能重用
			 * 2013-3-24 23:26:03
			 */
			View editDlgView = inflater.inflate(R.layout.dialog_input, null);
			final EditText editText = (EditText) editDlgView.findViewById(R.id.editText1);
			
			Object obj = args.get("title");
			builder.setTitle(obj!=null? obj.toString() : ""); 
			builder.setView(editDlgView);
			builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					emulator.setEditInputContent(editText.getText().toString());
					emulator.postMrpEvent(MrDefines.MR_DIALOG_EVENT, MrDefines.MR_DIALOG_KEY_OK, 0);
				}
			});
			builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					emulator.setEditInputContent(null);
					emulator.postMrpEvent(MrDefines.MR_DIALOG_EVENT, MrDefines.MR_DIALOG_KEY_CANCEL, 0);
				}
			});
			builder.setCancelable(false);
			
			return builder.create();
		}
		
		return super.onCreateDialog(id, args);
	}
	
	private int tmpChoice;
	@SuppressWarnings("deprecation")
	@Override
	protected Dialog onCreateDialog(int id) {
		if(id == DLG_SCALE_MODE){
			final String[] items = getResources().getStringArray(R.array.scaling_mode_entryvalues);
			int choice = 0;

			for(String s : items){
				if(s.equals(MrpScreen.getScaleModeTag()))
					break;
				choice++;
			}
			if(choice > items.length - 1)
				choice = items.length - 1;

			return new AlertDialog.Builder(this)
	            .setTitle(R.string.scaling_mode)
	            .setSingleChoiceItems(R.array.scaling_mode_entries, choice, new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int which) {
	                    /* User clicked on a radio button do some stuff */
	                	tmpChoice = which;
	                }
	            })
	            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {
	                	MrpScreen.parseScaleMode(items[tmpChoice]);
	                	emulator.getScreen().initScale();
	                	emulatorView.reDraw();
	                }
	            })
	            .setNegativeButton(R.string.cancel, null)
	           .create();
		} else if (id == DLG_PAD_ALPHA) {
			SeekBar bar = new SeekBar(this);
			bar.setMax(255);
			bar.setProgress(Keypad.getInstance().getOpacity());
			bar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
				}
				
				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
				}
				
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					if(fromUser){
						if(progress < 20)
							progress = 20;
						else if (progress > 0xff)
							progress = 0xff;
						
						Prefer.keypadOpacity = progress;
						padView.setKeypadOpacity(progress);
					}
				}
			});
			
			return new AlertDialog.Builder(this)
				.setTitle(R.string.pad_opacity)
				.setView(bar)
				.setPositiveButton(R.string.ok, null)
				.create();
		} else if (id == DLG_TOOLS) {
			
			return new AlertDialog.Builder(this)
				.setTitle(R.string.tools)
				.setItems(TOOLS, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						runTool(which);
					}
				})
				.create();
		}
		
		return super.onCreateDialog(id);
	}
	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
		if (id == DLG_EDIT) {
			final EditText editText = (EditText) dialog.findViewById(R.id.editText1);
			if(editText == null){ //虽然不可能失败
				emulator.setEditInputContent(null);
				emulator.postMrpEvent(MrDefines.MR_DIALOG_EVENT, MrDefines.MR_DIALOG_KEY_CANCEL, 0);
				return;
			}
			
			Object obj = args.get("content");
			editText.setText(obj!=null? obj.toString() : "");
			//字数限制
			if(Prefer.limitInputLength)
				editText.setFilters(new  InputFilter[]{ new  InputFilter.LengthFilter(args.getInt("max"))});
			
			int newType, type = args.getInt("type", MrDefines.MR_EDIT_ANY);
			
			if (type == MrDefines.MR_EDIT_ALPHA)
				newType = EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_FLAG_CAP_CHARACTERS | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE;
			else if (type == MrDefines.MR_EDIT_NUMERIC)
				newType = EditorInfo.TYPE_CLASS_NUMBER | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE;
			else if (type == MrDefines.MR_EDIT_PASSWORD)
				newType = EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD;
			else
				newType = EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE;
			
			editText.setInputType(newType);
			
			obj = args.get("title");
			dialog.setTitle(obj!=null? obj.toString() : "");
//			dialog.setTitle(args.getCharSequence("title", "")); //2.2 无此方法
		} else {
			super.onPrepareDialog(id, dialog);
		}
	}
	
	void showFragmentDialog(DialogFragment fragment) {
        fragment.show(getSupportFragmentManager(), "dialog");
    }
	
	private static final String[] TOOLS = new String[]{
		"按键精灵"
	};
	
	private KeySprite mKeySprite;
	
	private void stopKeySprite() {
		if(mKeySprite != null){
			mKeySprite.stop();
			mKeySprite = null;
			tvInfo.setVisibility(View.INVISIBLE);
		}
	}
	
	private void runKeySprite(KeySprite sprite) {
		mKeySprite = sprite;
		tvInfo.setTag(INFO_TYPE_KEY_SPRITE);
		
		sprite.run(new KeyEventListener() {
			
			@Override
			public void onKeyUp(int key, Sprite sprite) {
				handler.obtainMessage(MSG_ID_KEY_UP, key, 0).sendToTarget();
				handler.obtainMessage(MSG_ID_UPDATE_INFO_TEXT, sprite.toString()).sendToTarget();
			}
			
			@Override
			public void onKeyDown(int key, Sprite sprite) {
				handler.obtainMessage(MSG_ID_KEY_DOWN, key, 0).sendToTarget();
				handler.obtainMessage(MSG_ID_UPDATE_INFO_TEXT, sprite.toString()).sendToTarget();
			}
		});
	}
	
	private void runTool(int index) {
		switch (index) {
		case 0: {
			showFragmentDialog(new ChooserFragment().setOnChooseLitener(new OnChooseLitener() {
				
				@Override
				public void onChoose(Object object) {
					runKeySprite((KeySprite) object);
				}
				
				@Override
				public void onCancel() {
					
				}
			}));
			break;
		}
		}
	}
	
	private boolean isNotificationShow = false;
	
	//自定义的setNotiType()方法
	@SuppressWarnings("deprecation")
	private void backNotification2() {
		// 建立新的Intent
		Intent notifyIntent = new Intent(this, EmulatorActivity.class);
		notifyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		
		// 建立PendingIntent作为设定递延执行的Activity
		PendingIntent appIntent = PendingIntent.getActivity(EmulatorActivity.this, 0, notifyIntent, 0);
		
		// 建立Notification，并设定相关参数
		Notification n = new Notification(R.drawable.ic_notify_small, null, System.currentTimeMillis());
		n.setLatestEventInfo(this, 
				emulator.getCurMrpFile().getAppName(), 
				getString(R.string.hint_click_to_back), 
				appIntent);
		
		if(n.contentView != null){
			n.contentView.setImageViewResource(InternalID.id_icon, R.drawable.ic_notify);
		}
		
		n.defaults = Notification.DEFAULT_LIGHTS;
		n.flags = Notification.FLAG_ONGOING_EVENT; //不可清楚
		
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		manager.notify(1001, n);
		
		isNotificationShow = true;
		
		SdkUtils.event(this, "backrun", EmuUtils.getTimeNow());
	}
	
	// 取消通知
	public void cancelNotification() {
		if(isNotificationShow){
			NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.cancel(1001);

			isNotificationShow = false;
			SdkUtils.event(this, "fgrun", EmuUtils.getTimeNow());
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == REQ_SHOWEDIT){
			if(resultCode == 1 && data != null){
				emulator.setEditInputContent(data.getStringExtra("input"));
				emulator.postMrpEvent(MrDefines.MR_DIALOG_EVENT, MrDefines.MR_DIALOG_KEY_OK, 0);
			}else {
				emulator.setEditInputContent(null);
				emulator.postMrpEvent(MrDefines.MR_DIALOG_EVENT, MrDefines.MR_DIALOG_KEY_CANCEL, 0);
			}
		}else if (requestCode == REQ_GET_IMAGE) { //选择图片
			if (resultCode == RESULT_OK && null != data) {
//				Uri selectedImage = data.getData();
//				String[] filePathColumn = { MediaStore.Images.Media.DATA };
//
//				Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
//				cursor.moveToFirst();
//
//				int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
//				String picturePath = cursor.getString(columnIndex);
//				cursor.close();
//				
//				bitmapBg = BitmapFactory.decodeFile(picturePath);
			}
		}
		
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	////////////////////////////////////////////////
	@SuppressWarnings("deprecation")
	public void createEdit(String title, String content, int type, int max) {
		if(Prefer.fullScnEditor){
			Intent intent = new Intent(this, EditActivity.class);
			intent.putExtra("title", title);
			intent.putExtra("content", content);
			intent.putExtra("type", type);
			intent.putExtra("max", max);
			startActivityForResult(intent, REQ_SHOWEDIT);
		}else {
			Bundle b = new Bundle();
			b.putString("title", title);
			b.putString("content", content);
			b.putInt("type", type);
			b.putInt("max", max);
			showDialog(DLG_EDIT, b);
		}
	}
	
	/**
	 * 发送短信提示
	 * 
	 * @param text
	 * @param addr
	 */
	public void reqSendSms(final String text, final String addr) {
		AlertDialog dialog = new AlertDialog.Builder(this)
			.setTitle(R.string.hint)
			.setMessage(emulator.getCurMrpFile().getAppName() 
					+ "请求发送短信：\n"
					+ "地址：" + addr + "\n"
					+ "内容：" + text + "\n")
			.setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					SmsManager sms = SmsManager.getDefault();
					List<String> messages = sms.divideMessage(text);
			        for (String message : messages) {
			            sms.sendTextMessage(addr, null, message, 
			            		PendingIntent.getBroadcast(EmulatorActivity.this, 0, new Intent(ACTION_SMS_SENT), 0), 
			            		null);
			        }
				}
			})
			.setNegativeButton(R.string.refused, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					//直接通知底层失败
	                emulator.vm_event(MrDefines.MR_SMS_RESULT, MrDefines.MR_FAILED, 0);
				}
			})
			.create();
		dialog.setCancelable(false);
		dialog.setCanceledOnTouchOutside(false);
		dialog.show();
	}
	
	/**
	 * 打电话提示
	 * 
	 * @param number
	 */
	public void reqCallPhone(final String number) {
		new AlertDialog.Builder(this)
			.setTitle(R.string.hint)
			.setMessage(emulator.getCurMrpFile().getAppName() 
					+ "请求拨打：\n" 
					+ number)
			.setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number));
					EmulatorActivity.this.startActivity(intent);
				}
			})
			.setNegativeButton(R.string.refused, null)
			.create()
			.show();
	}
	
	private WindowManager wm;
    private WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams();
    
	private void floatView() {
        //获取WindowManager
        wm = (WindowManager)getSystemService("window");

         /**
         *以下都是WindowManager.LayoutParams的相关属性
         * 具体用途可参考SDK文档
         */
        wmParams.type = WindowManager.LayoutParams.TYPE_PHONE;   //设置window type
        wmParams.format = PixelFormat.RGBA_8888;   //设置图片格式，效果为背景透明

        //设置Window flag
//        wmParams.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL | LayoutParams.FLAG_NOT_FOCUSABLE;
        /*
         * 下面的flags属性的效果形同“锁定”。
         * 悬浮窗不可触摸，不接受任何事件,同时不影响后面的事件响应。
         wmParams.flags=LayoutParams.FLAG_NOT_TOUCH_MODAL 
                               | LayoutParams.FLAG_NOT_FOCUSABLE
                               | LayoutParams.FLAG_NOT_TOUCHABLE;
        */
        
        
        wmParams.gravity=Gravity.LEFT|Gravity.TOP;   //调整悬浮窗口至左上角
        //以屏幕左上角为原点，设置x、y初始值
        wmParams.x=0;
        wmParams.y=0;
        
        //设置悬浮窗口长宽数据
        wmParams.width=40;
        wmParams.height=40;
    
        //显示myFloatView图像
        wm.addView(emulatorView, wmParams);
    }
	
	@Override
	public boolean handleMessage(Message msg) {
		switch (msg.what) {
		case MSG_ID_KEY_DOWN:
			emulator.postMrpEvent(MR_KEY_PRESS, msg.arg1, 0);
			break;
			
		case MSG_ID_KEY_UP:
			emulator.postMrpEvent(MR_KEY_RELEASE, msg.arg1, 0);
			break;
			
		case MSG_ID_UPDATE_INFO_TEXT: {
			String text = msg.obj.toString();
			if(text == null)
				tvInfo.setVisibility(View.INVISIBLE);
			else {
				if(tvInfo.getVisibility() != View.VISIBLE)
					tvInfo.setVisibility(View.VISIBLE);
				tvInfo.setText(text);
			}
			
			break;
		}
		
		default:
			return false;
		}
		
		return true;
	}
}
