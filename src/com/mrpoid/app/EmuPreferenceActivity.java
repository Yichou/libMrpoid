package com.mrpoid.app;

import java.io.File;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.MenuItem;
import android.widget.Toast;

import com.mrpoid.R;
import com.mrpoid.core.EmuPath;
import com.mrpoid.core.EmuPath.OnPathChangeListener;
import com.mrpoid.core.MrpScreen;
import com.mrpoid.core.Prefer;
import com.mrpoid.ui.PathPreference;
import com.yichou.common.sdk.SdkUtils;
import com.yichou.common.utils.FileUtils;

/**
 * 
 * @author Yichou
 *
 */
public class EmuPreferenceActivity extends PreferenceActivity implements 
	OnPreferenceChangeListener,
	OnPathChangeListener,
	OnSharedPreferenceChangeListener
	{
	static final String TAG = EmuPreferenceActivity.class.getSimpleName();
	
	private PathPreference epMythroad, epSD;
	private CheckBoxPreference chkpMulti;
	private CheckBoxPreference chkPrivate;
	private ListPreference lpScnSize;
	private String oldScnSize;
	private EmuPath emuPath;
	private SharedPreferences sp;
	
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		sp = PreferenceManager.getDefaultSharedPreferences(this);
		sp.registerOnSharedPreferenceChangeListener(this);

		addPreferencesFromResource(R.xml.preferences_new);
//		findPreference("version").setOnPreferenceClickListener(this);
//		findPreference("advanced").setOnPreferenceClickListener(this);
//		findPreference("checkUpdate").setOnPreferenceClickListener(this);
		
		try {
			findPreference("keypadLayout").setIntent(new Intent(this, KeypadActivity.class));
		} catch (Exception e) {
		}
		
		emuPath = EmuPath.getInstance();
		
		Intent intent = new Intent(this, HelpActivity.class);
		intent.setData(Uri.parse(getString(R.string.setup_wizard_uri)));
		findPreference("setupWizard").setIntent(intent);
		
		
		lpScnSize = (ListPreference) findPreference(Prefer.KEY_SCN_SIZE);
		lpScnSize.setValue(MrpScreen.getSizeTag());
		lpScnSize.setSummary(lpScnSize.getValue());
		lpScnSize.setOnPreferenceChangeListener(this);
		oldScnSize = lpScnSize.getValue();

		ListPreference lp2 = (ListPreference) findPreference(Prefer.KEY_SCALING_MODE);
		lp2.setValue(MrpScreen.getScaleModeTag());
		
		chkpMulti = (CheckBoxPreference) findPreference(Prefer.KEY_MULTI_PATH);
		chkpMulti.setOnPreferenceChangeListener(this);

		epSD = (PathPreference)findPreference(Prefer.KEY_SDCARD_PATH);
		epSD.setSummary(emuPath.getSDPath());
		epSD.setOnPreferenceChangeListener(this);
		epSD.setEnabled(FileUtils.isSDMounted());
		epSD.setDefaultValue(EmuPath.DEF_SD_PATH);
//		epSD.setPath(emuPath.getSDPath());
		{
			String sd = EmuPath.DEF_SD_PATH;
			
			int l = sd.length();
			if(sd.charAt(l - 1) == File.separatorChar){
				sd = sd.substring(0, l-1);
			}

			String path = sd;
			String dir = null;
			int i = sd.lastIndexOf(File.separatorChar);
			if(i != -1){
				path = sd.substring(0, i+1); //保留 /结尾
				dir = sd.substring(i+1);
				dir.concat(File.separator);
			}
			
			epSD.setDefRoot(path);
			epSD.setDefDir(dir);
		}
		
		epMythroad = (PathPreference) findPreference(Prefer.KEY_MYTHROAD_PATH);
		epMythroad.setSummary(emuPath.getMythroadPath());
		epMythroad.setOnPreferenceChangeListener(this);
		epMythroad.setEnabled(!chkpMulti.isChecked());
		epMythroad.setDefaultValue(EmuPath.DEF_MYTHROAD_DIR);
//		if(!chkpMulti.isChecked())
//			epMythroad.setPath(emuPath.getMythroadPath());
		{
			epMythroad.setDefRoot(emuPath.getSDPath());
			epMythroad.setDefDir(emuPath.getMythroadPath());
		}
		
		chkPrivate = (CheckBoxPreference) findPreference(Prefer.KEY_USE_PRIVATE_DIR);
		chkPrivate.setOnPreferenceChangeListener(this);
		
		emuPath.addOnPathChangeListener(this);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		emuPath.removeOnPathChangeListener(this);
		sp.unregisterOnSharedPreferenceChangeListener(this);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onBackPressed() {
		if(chkpMulti.isChecked() && (oldScnSize!=null && !oldScnSize.equals(lpScnSize.getValue()))){
			showDialog(2);
		}else {
			super.onBackPressed();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
//		case android.R.id.home:
//			finish();
//			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onPreferenceChange(Preference p, Object v) {
//		EmuLog.i(TAG, "changed("+p+", "+v+")");
		
		/*if (p == epMythroad) {
			epMythroad.setSummary((CharSequence) v);
		} else if (p == epSD) {
			epSD.setSummary((CharSequence) v);
			epMythroad.setDefRoot(emuPath.getSDPath());
		} else*/ 
		if (Prefer.KEY_MEM_SIZE.equals(p.getKey())) {
			p.setSummary((CharSequence) v + " M");
			
			return true;
		} 
		else if (Prefer.KEY_SCN_SIZE.equals(p.getKey())) {
			p.setSummary((CharSequence) v);
		} 
		else if (Prefer.KEY_MULTI_PATH.equals(p.getKey())) {
			epMythroad.setEnabled(!(Boolean)v);
//			String path = "";
//			
//			if((Boolean)v){//选中后
//				path = emuPath.getMythroadPath() + MrpScreen.getSizeTag();
//			}else {
//				path = EmuPath.DEF_MYTHROAD_DIR;
//			}
//
//			epMythroad.setPath(path);
//			epMythroad.setSummary(path);
			return true;
		} 

		return true;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected Dialog onCreateDialog(int id) {
		if(id == 1){
			return new AlertDialog.Builder(this)
				.setTitle(R.string.warn)
				.setMessage("如果你不知道自己在干什么请离开这里！")
				.setNegativeButton(R.string.ok, null)
				.setPositiveButton("不在提醒", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Prefer.notHintAdvSet = true;
					}
				})
				.create();
		}else if(id == 2){
			return new AlertDialog.Builder(this)
				.setTitle(R.string.hint)
				.setMessage("您修改了屏幕尺寸，并且选择了 \"不同分辨率在不同目录下运行\"意味着之前下载过的冒泡游戏都需要重新下载！")
				.setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				})
				.create();
		}else if (id == 3) {
			try {
				PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_ACTIVITIES);
				
				return new AlertDialog.Builder(this)
					.setTitle(R.string.app_name)
					.setMessage("版本 " + packageInfo.versionName + "\n\n"
							+ getString(R.string.cpoy_right))
					.setPositiveButton("确定", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							///...
						}
					})
					.create();
					
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}
				
		}
		
		return super.onCreateDialog(id);
	}
	
	private Context getActivity() {
		return this;
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen ps, Preference p) {
		if ("checkUpdate".equals(p.getKey())) {
			SdkUtils.checkUpdate(this);
			System.out.println("check update!");
			return true;
		} else if("advanced".equals(p.getKey())){
			if(!Prefer.notHintAdvSet)
				showDialog(1);
		} else if ("version".equals(p.getKey())) {
			showDialog(3);
		} else if (p == epMythroad) {
			if(Prefer.differentPath){
				Toast.makeText(this, "请取消勾选 " + getString(R.string.run_under_multi_path) + " 选项!", Toast.LENGTH_SHORT).show();
			}else {
			}
			
			return true;
		} 
		
		return super.onPreferenceTreeClick(ps, p);
	}

	@Override
	public void onPathChanged(String newPath, String oldPath) {
		epMythroad.setSummary(emuPath.getMythroadPath());
		epSD.setSummary(emuPath.getSDPath());
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		// TODO Auto-generated method stub
		
	}
}
