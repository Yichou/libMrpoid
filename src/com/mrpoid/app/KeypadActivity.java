package com.mrpoid.app;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.util.Xml;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.mrpoid.R;
import com.mrpoid.core.Keypad;
import com.mrpoid.core.MrDefines;
import com.mrpoid.gui.Actor;
import com.mrpoid.gui.ActorGroup;
import com.mrpoid.gui.Director;
import com.mrpoid.gui.DragTextButton;
import com.mrpoid.gui.Position;
import com.mrpoid.gui.TextButton;

/**
 * 虚拟键盘编辑
 * 
 * @author Yichou
 *
 * 
 */
public class KeypadActivity extends Activity implements 
	OnSeekBarChangeListener, 
	AnimationListener,
	OnCheckedChangeListener, 
	OnClickListener {
	public Bitmap bmp_dpad_n, bmp_dpad_p, bmp_btn_bg_n, bmp_btn_bg_p;
	
	public void loadBmp(Resources res) {
		bmp_dpad_n = BitmapFactory.decodeResource(res, R.drawable.dpad_n);
		bmp_dpad_p = BitmapFactory.decodeResource(res, R.drawable.dpad_p);

		bmp_btn_bg_n = BitmapFactory.decodeResource(res, R.drawable.btn_bg_n);
		bmp_btn_bg_p = BitmapFactory.decodeResource(res, R.drawable.btn_bg_p);
	} 
	
	public void releaseBmp() {
		bmp_dpad_n.recycle();
		bmp_dpad_p.recycle();
		bmp_btn_bg_n.recycle();
		bmp_btn_bg_p.recycle();
	}

	MyView view;
	SmpKeyboard mKeyboard;
	SeekBar mOpacitySeekBar;
	CheckBox mOriCheckBox;
	ImageView mTogImageView;
	boolean bShow;
	LinearLayout mLinearLayout;
	Animation animIn, animOut, rotaLeft, rotaRight;
	TextView mTextView;
	Rect mInvalidRect = new Rect();
	
	
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if(seekBar.getId() == R.id.seekBar1){
		}else if (seekBar.getId() == R.id.seekBar2) {
			if(progress < 10)
				progress = 10;
			else if(progress > 255)
				progress = 255;
			mKeyboard.setOpacity(progress);
		}
	}
	
	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if(buttonView.getId() == R.id.checkBox1){
//			saveKeyboard();
			setRequestedOrientation(isChecked? 
					ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}else if (buttonView.getId() == R.id.checkBox2) {
//			saveKeyboard();
			mKeyboard.setMod(isChecked? 2 : 1);
		}
	}
	
	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.button1){
			saveKeyboard();
		}else if (v.getId() == R.id.togBtn) {
			bShow = !bShow;
			
			mTogImageView.startAnimation(bShow? rotaRight : rotaLeft);
			mLinearLayout.startAnimation(bShow? animIn : animOut);
		}
	}
	
	@Override
	public void onAnimationStart(Animation animation) {
		if(animation == animIn){
			mLinearLayout.setVisibility(View.VISIBLE);
		}
	}
	
	@Override
	public void onAnimationRepeat(Animation animation) {
	}
	
	@Override
	public void onAnimationEnd(Animation animation) {
		if(animation == animOut){
			mLinearLayout.setVisibility(View.INVISIBLE);
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.activity_keypad);
		
		animIn = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);
		animIn.setAnimationListener(this);
		animOut = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right);
		animOut.setAnimationListener(this);
		rotaLeft = AnimationUtils.loadAnimation(this, R.anim.rota_left);
		rotaLeft.setAnimationListener(this);
		rotaRight = AnimationUtils.loadAnimation(this, R.anim.rota_right);
		rotaRight.setAnimationListener(this);
		
		((SeekBar)findViewById(R.id.seekBar1)).setOnSeekBarChangeListener(this);

		mOpacitySeekBar = ((SeekBar)findViewById(R.id.seekBar2));
		mOpacitySeekBar.setOnSeekBarChangeListener(this);
		
		mOriCheckBox = ((CheckBox)findViewById(R.id.checkBox1));
		DisplayMetrics display = getResources().getDisplayMetrics();
		if(display.widthPixels > display.heightPixels)
			mOriCheckBox.setChecked(true);
		mOriCheckBox.setOnCheckedChangeListener(this);
		
		((CheckBox)findViewById(R.id.checkBox2)).setOnCheckedChangeListener(this);
		findViewById(R.id.button1).setOnClickListener(this);
		
		mLinearLayout = (LinearLayout) findViewById(R.id.linerLayout1);
		
		findViewById(R.id.togBtn).setOnClickListener(this);

		bShow = true;
		mTogImageView = (ImageView) findViewById(R.id.imageView1);
		
		mTextView = (TextView) findViewById(R.id.textView1);
		mTextView.getWindowVisibleDisplayFrame(mInvalidRect);
		
		loadBmp(getResources());
		
		FrameLayout layout = (FrameLayout) findViewById(R.id.root);
		FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		view = new MyView(this);
		layout.addView(view, p);
	}
	
	private void saveKeyboard() {
		try {
			mKeyboard.save();
			Toast.makeText(this, "保存成功！", Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, "保存失败！", Toast.LENGTH_SHORT).show();
		}
	}
	
	@Override
	protected void onDestroy() {
		saveKeyboard();
		releaseBmp();
		
		super.onDestroy();
	}
	
	
	private static final int[] ids = {
		MrDefines.MR_KEY_1, MrDefines.MR_KEY_2, MrDefines.MR_KEY_3,
		MrDefines.MR_KEY_4, MrDefines.MR_KEY_5, MrDefines.MR_KEY_6,
		MrDefines.MR_KEY_7, MrDefines.MR_KEY_8, MrDefines.MR_KEY_9,
		MrDefines.MR_KEY_STAR, MrDefines.MR_KEY_0, MrDefines.MR_KEY_POUND
	};
	
	private static final String[] titles = {
		"1", "2", "3",
		"4", "5", "6", 
		"7", "8", "9", 
		"*", "0", "#"
	};
	
	
	class SmpKeyboard extends Director {
		MyView view;
		Paint paint;
		DragTextButton pad, btnOk, btnCancel;
		ActorGroup numGroup;
		int mode = 1;
		boolean isLandscape;
		
		
		int MARGIN = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                8, 
                getResources().getDisplayMetrics());
		
		public void setMod(int mod) {
			this.mode = mod;
			
			reset();
		}
		
		private void createXhidp() {
			pad = new DragTextButton(this, bmp_dpad_n, bmp_dpad_p, 
					null, MrDefines.MR_KEY_SELECT, 0, 0);
			pad.setPosition((viewW - pad.w - MARGIN), (viewH - pad.h - MARGIN));
			addChild(pad);
			
			int y0 = viewH - (MARGIN + bmp_btn_bg_n.getHeight())*4;
			
			// 确定
			btnOk = new DragTextButton(this, bmp_btn_bg_n, bmp_btn_bg_p,
					getString(R.string.ok), 
					MrDefines.MR_KEY_SOFTLEFT, 0, 0);
			addChild(btnOk);
			btnOk.setPosition(MARGIN, y0 - btnOk.h - MARGIN);
			
			// 返回
			btnCancel = new DragTextButton(this, bmp_btn_bg_n, bmp_btn_bg_p,
					getString(R.string.cancel), 
					MrDefines.MR_KEY_SOFTRIGHT, 0, 0);
			addChild(btnCancel);
			btnCancel.setPosition(btnOk.getRight() + MARGIN*2 + btnOk.w, y0 - btnCancel.h - MARGIN);
			
			
			numGroup = new ActorGroup(this);
			addChild(numGroup);
			int x = 0;
			int y = 0;
			
			for (int i = 0; i < 4; i++) {
				for (int j = 0; j < 3; j++) {
					DragTextButton button = new DragTextButton(this, bmp_btn_bg_n, bmp_btn_bg_p,
							titles[i * 3 + j], 
							ids[i * 3 + j], x, y
							);
					numGroup.addChild(button);
					
					x += bmp_btn_bg_n.getWidth() + MARGIN;
				}
				x = 0;
				y += bmp_btn_bg_n.getHeight() + MARGIN;
			}
			numGroup.setPosition(MARGIN, y0);
		}

		public SmpKeyboard(MyView view) {
			super();
			this.view = view;
			
			paint = new Paint(Paint.ANTI_ALIAS_FLAG);
			paint.setColor(0x80808080);
			int size = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
	                20, 
	                view.getResources().getDisplayMetrics());
			paint.setTextSize(size);
			paint.setStyle(Paint.Style.FILL);
//			paint.setShadowLayer(3, 3.0f, 3.0f, 0x80000000);
		}
		
		private void reset() {
			removeAllChild();
			try {
				readFromXml();
			} catch (Exception e) {
				e.printStackTrace();
				createXhidp();
			}
			view.invalidate();
		}
		
		@Override
		public void setViewSize(int width, int height) {
			super.setViewSize(width, height);

			isLandscape = width>height;
			reset();
		}
		
		@Override
		public void draw(Canvas canvas, Paint paint) {
			super.draw(canvas, this.paint);
		}

		@Override
		public void invalida(Actor a) {
			view.invalidate();
		}
		
		
		
		public void readFromXml() throws Exception {
			InputStream is = new FileInputStream(Keypad.getXml(isLandscape, mode));
			
			if(is.available() <= 0)
				throw new RuntimeException();

			XmlPullParser pullParser = Xml.newPullParser();
			pullParser.setInput(is, "UTF-8"); // 为Pull解释器设置要解析的XML数据
			int event = pullParser.getEventType();
			ActorGroup group = null;

			while (event != XmlPullParser.END_DOCUMENT) {
				switch (event) {
				case XmlPullParser.START_TAG: {
					if ("root".equals(pullParser.getName())) {
						// pullParser.getAttributeValue(null, "opacity")
						int opacity = Integer.valueOf(pullParser.getAttributeValue(0));
						mOpacitySeekBar.setProgress(opacity);
//						setOpacity(opacity);

//						float scale = Float.valueOf(pullParser.getAttributeValue(1));
					}
					else if ("group".equals(pullParser.getName())) {
						group = numGroup = new ActorGroup(this);
						boolean visable = Boolean.valueOf(pullParser.getAttributeValue(0));
						int x = Integer.valueOf(pullParser.getAttributeValue(1));
						int y = Integer.valueOf(pullParser.getAttributeValue(2));
						group.setPosition(x, y);
						group.setVisible(visable);
						
						this.addChild(group);
					}
					else if ("key".equals(pullParser.getName())) {
						String title = pullParser.getAttributeValue(0);
						int value = Integer.valueOf(pullParser.getAttributeValue(1));
//						boolean visable = Boolean.valueOf(pullParser.getAttributeValue(2));
						int x = Integer.valueOf(pullParser.getAttributeValue(3));
						int y = Integer.valueOf(pullParser.getAttributeValue(4));
						

						DragTextButton button = new DragTextButton(this, bmp_btn_bg_n, bmp_btn_bg_p, title, value, x, y);
//						button.setVisible(visable);
						
						if(value == MrDefines.MR_KEY_SOFTLEFT)
							btnOk = button;
						else if(value == MrDefines.MR_KEY_SOFTRIGHT)
							btnCancel = button;
						
						if (group != null) {
							group.addChild(button);
						} else {
							this.addChild(button);
						}
					}
					else if ("dpad".equals(pullParser.getName())) {
						boolean visable = Boolean.valueOf(pullParser.getAttributeValue(0));
						int x = Integer.valueOf(pullParser.getAttributeValue(1));
						int y = Integer.valueOf(pullParser.getAttributeValue(2));

						pad = new DragTextButton(this, bmp_dpad_n, bmp_dpad_p, 
								null, MrDefines.MR_KEY_SELECT, x, y);
						pad.setVisible(visable);
						
						this.addChild(pad);
					}

					break;
				}

				case XmlPullParser.END_TAG: {
					if ("group".equals(pullParser.getName())){
						group = null;
					}
					break;
				}

				}
				
				event = pullParser.next();
			}
		}
		
		Position position = new Position();
		
		private void writeActor(XmlSerializer s, TextButton button) throws Exception {
			button.getPositionInWorld(position);
			button.setVisible((position.y > mTextView.getBottom() || position.y+button.getH() < mTextView.getTop()));
			
			s.startTag(null, "key");
	        s.attribute(null, "title", button.getTitle());
	        s.attribute(null, "value", String.valueOf(button.getId()));
	        s.attribute(null, "visable", String.valueOf(button.isShow()));
	        s.attribute(null, "x", String.valueOf((int)button.getX()));
	        s.attribute(null, "y", String.valueOf((int)button.getY()));
	        s.endTag(null, "key");
		}
	    
	    public void save() throws Exception {
	    	FileOutputStream out = new FileOutputStream(Keypad.getXml(isLandscape, mode));
	        XmlSerializer s = Xml.newSerializer();
	       
	        s.setOutput(out, "UTF-8");
	        s.startDocument("UTF-8", true);
	       
	        s.startTag(null, "root");
	        s.attribute(null, "opacity", String.valueOf(mOpacitySeekBar.getProgress()));
	        s.attribute(null, "scale", "1.0");
	       
	        writeActor(s, btnOk);
	        writeActor(s, btnCancel);
	        
	        s.startTag(null, "dpad");
	        s.attribute(null, "visable", String.valueOf(pad.isShow()));
	        s.attribute(null, "x", String.valueOf((int)pad.getX()));
	        s.attribute(null, "y", String.valueOf((int)pad.getY()));
	        s.endTag(null, "dpad");
	        
	        s.startTag(null, "group");
	        s.attribute(null, "visable", String.valueOf(numGroup.isShow()));
	        s.attribute(null, "x", String.valueOf((int)numGroup.getX()));
	        s.attribute(null, "y", String.valueOf((int)numGroup.getY()));
	        
	        int c = numGroup.getChildCount();
	        for (int i = 0; i < c; i++) {
				writeActor(s, (TextButton) numGroup.getChild(i));
			}
	        
	        s.endTag(null, "group");
	        
	        s.endTag(null, "root");
	        s.endDocument();
	        out.flush();
	        out.close();
	    }
	    
	    public void setOpacity(int o) {
			paint.setAlpha(o);
			view.invalidate();
		}
	}
	
	public class MyView extends View {
		public MyView(Context context) {
			super(context);
			
			mKeyboard = new SmpKeyboard(this);
		}
		
		@Override
		protected void onSizeChanged(int w, int h, int oldw, int oldh) {
			super.onSizeChanged(w, h, oldw, oldh);

			mKeyboard.setViewSize(w, h);
			mTextView.getWindowVisibleDisplayFrame(mInvalidRect);
		}
		
		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
				
			mKeyboard.draw(canvas, null);
		}
		
		@Override
		public boolean onTouchEvent(MotionEvent event) {
			return mKeyboard.dispatchTouchEvent(event);
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}
}
