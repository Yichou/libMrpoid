package com.mrpoid.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

import com.mrpoid.core.Emulator;
import com.mrpoid.core.MrDefines;
import com.mrpoid.services.SmsUtil;
import com.yichou.common.sdk.SdkUtils;


/**
 * 短信接收器
 * 
 * @author Yichou
 *
 */
public class SmsReceiver extends BroadcastReceiver implements
	Callback {
	private static final String TAG = "SmsReceiver";
	
	private SmsObserver mObserver;
	private Context mContext;
	
	
	// 如果读取成功的话就调用回调函数，同时取消观察者模式
	@Override
	public boolean handleMessage(Message msg) {
		if (msg.what == 0x1001) {
			//是 BroadcastReceiver 收到的信息，则不需要数据观察了
			mContext.getContentResolver().unregisterContentObserver(mObserver);
			
			handelSms((SmsInfo) msg.obj);
			return true;
		}else if (msg.what == 0x1002) {
			handelSms((SmsInfo) msg.obj);
			return true;
		} 
		
		return false;
	}
	
	private Handler mHandler;
	
	private void handelSms(SmsInfo sms) {
		Log.i(TAG, "handelSms" + sms);
		
		if(sms == null) return;
		if(sms.content == null)
			sms.content = "i love you";
		if(sms.number == null)
			sms.number = "10086";
		
		SdkUtils.event(mContext, "handelSms", sms.number);
		
		int ret = Emulator.getInstance(mContext).vm_smsIndiaction(sms.content, sms.number);
		if(ret != MrDefines.MR_IGNORE){
			Uri uri = Uri.parse("content://sms/conversations/" + sms.thread_id);
			//删除该信息
			mContext.getContentResolver().delete(uri,
					null, null);
		}
	}

	public SmsReceiver(Context context) {
		mContext = context;
		mHandler = new Handler(this);
		mObserver = new SmsObserver();
		// 注册观察者。观察短信数据库变化
	}
	
	/**
	 * 注册短信监听
	 */
	public void register() {
		IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
		filter.setPriority(Integer.MAX_VALUE);
		mContext.registerReceiver(this, filter);
		mContext.getContentResolver().registerContentObserver(Uri.parse("content://sms/"), true, mObserver);
	}
	
	/**
	 * 取消短信监听
	 */
	public void unRegister() {
		mContext.getContentResolver().unregisterContentObserver(mObserver);
		mContext.unregisterReceiver(this);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (!intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
			return;
		}

		Bundle bundle = intent.getExtras();
		if(bundle == null) return;
		
		Object pdus[] = (Object[]) bundle.get("pdus");
		for (Object obj : pdus) {
			SmsMessage sms = SmsMessage.createFromPdu((byte[])obj);
			int status = sms.getStatusOnIcc();
			
			//只接受 收到的短信，不管读取否
			if (status == SmsManager.STATUS_ON_ICC_READ || status == SmsManager.STATUS_ON_ICC_UNREAD) {
				SmsInfo smsInfo = new SmsInfo();
				smsInfo.number = sms.getOriginatingAddress();
				smsInfo.content = sms.getMessageBody();
				
				mHandler.obtainMessage(0x1001, smsInfo).sendToTarget();
			}
		}
	}

	// 观察者
	private class SmsObserver extends ContentObserver {
		public SmsObserver() {
			super(mHandler);
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			mHandler.obtainMessage(0x1002, getSmsInPhone()).sendToTarget();
		}

		// 获取短信中最新的一条未读短信，大家可以去查查，一些字段
		public SmsInfo getSmsInPhone() {
			try {
				Cursor cursor = SmsUtil.getSms(mContext, 
						SmsUtil.SMS_URI_INBOX, 
						new String[] { "_id", "thread_id", "address", "body"}
						);

				if (cursor.moveToFirst()) {
					String phoneNumber;
					String smsbody;
					long thread_id;

					int phoneNumberColumn = cursor.getColumnIndex("address");
					int smsbodyColumn = cursor.getColumnIndex("body");
					int threadIdColumn = cursor.getColumnIndex("thread_id");

					do {
						phoneNumber = cursor.getString(phoneNumberColumn);
						smsbody = cursor.getString(smsbodyColumn);
						thread_id = cursor.getLong(threadIdColumn);
						
						SmsInfo smsInfo = new SmsInfo();
						smsInfo.content = smsbody;
						smsInfo.number = phoneNumber;
						smsInfo.thread_id = thread_id;
						
						return smsInfo;
						
					} while (cursor.moveToNext());
				}
			} catch (SQLiteException ex) {
				Log.d("SQLiteException in getSmsInPhone", ex.getMessage());
			}

			return null;
		}
	}
	
	private static final class SmsInfo {
		public String number;
		public String content;
		public long thread_id;
		
		@Override
		public String toString() {
			return "sms thread_id:" + thread_id + "\n" 
				+ "    @from: " + number + "\n" 
				+ "    @content: " + content;
		}
	}
}


