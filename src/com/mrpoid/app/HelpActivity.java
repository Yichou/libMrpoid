package com.mrpoid.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.mrpoid.core.Prefer;

/**
 * 
 * @author Yichou
 *
 */
public class HelpActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(Prefer.THEME); //Used for theme switching in samples
		
		super.onCreate(savedInstanceState);

		WebView view = new WebView(this);
		view.setWebChromeClient(new WebChromeClient() {
			public void onReceivedTitle(WebView view, String title) {
				HelpActivity.this.setTitle(title);
			}
		});
		setContentView(view);

//		view.getSettings().setJavaScriptEnabled(true); 
//		view.getSettings().setDefaultTextEncodingName("utf-8");
//		view.loadDataWithBaseURL(getIntent().getData().toString(), null, "text/html", "utf-8", null);
		view.loadUrl(getIntent().getData().toString());

//		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
