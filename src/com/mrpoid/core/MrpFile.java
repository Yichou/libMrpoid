package com.mrpoid.core;

import java.io.File;

/**
 * 2012/10/9
 * @author JianbinZhu
 *
 */
public class MrpFile implements Comparable<MrpFile> {
	private String name;
	private String appName;
	
	public long length;
	public boolean isDir;

	/**
	 * 
	 * @param f 可为NULL
	 */
	public MrpFile(File f) {
		if(f != null){
			this.name = f.getName();
			this.isDir = f.isDirectory();
			this.length = f.length();
		}
		this.appName = null;
	}
	
	public MrpFile(String appName){
		this.appName = appName;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public int compareTo(MrpFile another) {
		if(this.isDir && !another.isDir)
			return -1;
		else if(!this.isDir && another.isDir)
			return 1;
		
		return 0;
	}
	
	public File toFile(String path) {
		return new File(path + name);
	}
	
	public boolean isDir() {
		return isDir;
	}
	
	public String getName() {
		return name;
	}
	
	public boolean isFile() {
		return !isDir;
	}
	
	public long getLength() {
		return length;
	}
	
	@Override
	public String toString() {
		return "[name=" + name + ", appName=" + appName + "]";
	}
}
