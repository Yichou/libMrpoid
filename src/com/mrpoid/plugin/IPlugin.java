package com.mrpoid.plugin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

public interface IPlugin {

	public void onCreate(Activity context);
	
	public  void onPause(Context context);
	
	public  void onResume(Context context);

	public  void onDestroy(Context context);
	
	public  void onReceive(Context context, Intent intent);
}
