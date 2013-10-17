package com.mrpoid.app;

import com.mrpoid.core.PluginProxy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class EmuReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		PluginProxy.onReceive(context, intent);
	}

}
