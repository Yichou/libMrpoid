package com.mrpoid.core;

import com.mrpoid.app.EmulatorActivity;

/**
 * 保存全局静态变量
 * 
 * @author Yichou
 *
 */
public final class EmuStatics {
	public static EmulatorActivity emulatorActivity;
	public static EmulatorView emulatorView;
	
	public static EmulatorActivity getEmulatorActivity() {
		return emulatorActivity;
	}
	
	public static EmulatorView getEmulatorView() {
		return emulatorView;
	}
}
