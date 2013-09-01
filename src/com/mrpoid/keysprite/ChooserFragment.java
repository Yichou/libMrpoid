package com.mrpoid.keysprite;

import java.io.File;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.mrpoid.R;

/**
 * 按键精灵选择界面
 * 
 * @author Yichou 2013-8-31
 *
 */
public class ChooserFragment extends DialogFragment {
	
	private void loadAndRun(File file) {
		KeySprite keySprite = new SampleKeySprite();
		try {
			keySprite.fromXml(file);
			// ...
			keySprite.run(-1);
		} catch (Exception e) {
		}
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final File dir = getActivity().getDir("keySprites", 0);
		final String[] files = dir.list();

		return new AlertDialog.Builder(getActivity())
			.setTitle(R.string.choose_one)
			.setItems(files, new DialogInterface.OnClickListener() {
	
				@Override
				public void onClick(DialogInterface dialog, int which) {
					loadAndRun(new File(dir, files[which]));
				}
			})
			.setPositiveButton(R.string.create, new DialogInterface.OnClickListener() {
	
				@Override
				public void onClick(DialogInterface dialog, int which) {
					startActivity(new Intent(getActivity(), KeySpriteEditorActivity.class));
				}
			})
			.create();
	}
}
