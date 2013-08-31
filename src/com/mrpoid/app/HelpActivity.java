package com.mrpoid.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.mrpoid.core.Prefer;

/**
 * 
 * @author Yichou
 * 
 */
public class HelpActivity extends Activity {
	private WebView webView;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(Prefer.THEME); // Used for theme switching in samples

		super.onCreate(savedInstanceState);

		webView = new WebView(this);
		webView.setWebChromeClient(new WebChromeClient() {
			public void onReceivedTitle(WebView view, String title) {
				HelpActivity.this.setTitle(title);
			}
		});
		setContentView(webView);

		// view.getSettings().setJavaScriptEnabled(true);
		// view.getSettings().setDefaultTextEncodingName("utf-8");
		// view.loadDataWithBaseURL(getIntent().getData().toString(), null,
		// "text/html", "utf-8", null);
		webView.loadUrl(getIntent().getData().toString());

		// getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		webView.setWebViewClient(new WebViewClient() {
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				view.loadUrl(url);
				return true;
			}
		});
	}

	@Override
	public void onBackPressed() {
		if (webView.canGoBack()) {
			webView.goBack();
		} else {
			super.onBackPressed();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, "前进");
		menu.add(0, 1, 1, "后退");
		menu.add(0, 2, 2, "刷新");
		menu.add(0, 3, 3, "退出");
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
			
		case 0:
			if(webView.canGoForward())
				webView.goForward();
			break;
			
		case 1:
			if (webView.canGoBack())
				webView.goBack();
			break;
			
		case 2:
			webView.reload();
			break;
			
		case 3:
			finish();
			break;
			
		default:
			return super.onOptionsItemSelected(item);
		}
		
		return true;
	}
}
