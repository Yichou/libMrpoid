package com.mrpoid.core;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class EmuEventHandler extends Handler {
	private Emulator emulator;
	
	public EmuEventHandler(Emulator emulator, Looper looper) {
		super(looper);
		
		this.emulator = emulator;
	}

	@Override
	public void handleMessage(Message msg) {
//		Log.d("---", "handleMessage:" + msg.toString());
//		if(!emulator.running) return;

		switch (msg.what) {
		case EVN_ID_RUNMRP: {
			Log.d(Emulator.TAG, "java start mrp:" + msg.obj.toString());

			emulator.vm_loadMrp((String) msg.obj);
			break;
		}
		
		case EVN_ID_TIMER: {
			emulator.vm_timeOut();
			break;
		}
		
		case EVN_ID_STOPMRP: {
			emulator.vm_exit();
			break;
		}
		
		case EVN_ID_PAUSE:
			emulator.vm_pause();
			break;
			
		case EVN_ID_RESUME:
			emulator.vm_resume();
			break;
		
		case EVN_ID_MREVENT: {
			int [] params = (int[]) msg.obj;
			emulator.vm_event(params[0], params[1], params[2]);
			break;
		}
		}
	}
	
	public static final int EVN_ID_RUNMRP = 0,
		EVN_ID_TIMER = 2,
		EVN_ID_MREVENT = 3,
		EVN_ID_STOPMRP = 4,
		EVN_ID_PAUSE = 5,
		EVN_ID_RESUME = 6,
		EVN_ID_LAST = 1000;
}
