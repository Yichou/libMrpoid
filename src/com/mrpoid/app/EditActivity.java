package com.mrpoid.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.mrpoid.R;
import com.mrpoid.core.MrDefines;

/**
 * 编辑框界面
 * 
 * @author Yichou
 *
 */
public class EditActivity extends Activity implements OnClickListener, TextWatcher {
	private EditText editText;
	private TextView tv_count;
	private int max;
	 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit);
		
		editText = (EditText)findViewById(R.id.editText1);
		editText.addTextChangedListener(this);

		findViewById(R.id.btn_ok).setOnClickListener(this);
		findViewById(R.id.btn_cancel).setOnClickListener(this);
		
		Intent intent = getIntent();
		editText.setText(intent.getStringExtra("content"));
		
		//长度   
		max = intent.getIntExtra("max", 0);
		
		editText.setFilters(new  InputFilter[]{ new  InputFilter.LengthFilter(max)});
		
		int type, newType;
		
		type = intent.getIntExtra("type", EditorInfo.TYPE_CLASS_TEXT);
		if (type == MrDefines.MR_EDIT_ALPHA)
			newType = EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_FLAG_CAP_CHARACTERS | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE;
		else if (type == MrDefines.MR_EDIT_NUMERIC)
			newType = EditorInfo.TYPE_CLASS_NUMBER | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE;
		else if (type == MrDefines.MR_EDIT_PASSWORD)
			newType = EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD;
		else
			newType = EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE;
		
		editText.setInputType(newType);
		
		tv_count = (TextView)findViewById(R.id.tv_count);
		tv_count.setText("" + max + "/" + max);
		((TextView)findViewById(R.id.tv_title)).setText(intent.getStringExtra("title"));
	}
	
	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.btn_ok){
			setResult(1, new Intent().putExtra("input", editText.getText().toString()));
			finish();
		}else if (v.getId() == R.id.btn_cancel) {
			setResult(0);
			finish();
		}
	}
	
	@Override
	public void onBackPressed() {
		setResult(0);
		finish();
	}
	
//	@Override
//	public boolean onKeyUp(int keyCode, KeyEvent event) {
//		if(keyCode == KeyEvent.KEYCODE_BACK){
//			setResult(0);
//			finish();
//			return true;
//		}
//		return super.onKeyUp(keyCode, event);
//	}
	
	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		
	}

	@Override
	public void afterTextChanged(Editable s) {
		//tv_count.setText(""+s.length() + "/" + max);
	}
}
