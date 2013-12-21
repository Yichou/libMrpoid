/*
 * Copyright (C) 2013 The Mrpoid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mrpoid.gui.keypad;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.xmlpull.v1.XmlPullParser;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.TypedValue;
import android.util.Xml;
import android.view.MotionEvent;
import android.view.View;

import com.mrpoid.R;
import com.mrpoid.app.EmulatorApplication;
import com.mrpoid.core.Prefer;
import com.mrpoid.gui.engine.Actor;
import com.mrpoid.gui.engine.ActorGroup;
import com.mrpoid.gui.engine.DPad;
import com.mrpoid.gui.engine.Director;
import com.mrpoid.gui.engine.FloatMenuButton;
import com.mrpoid.gui.engine.TextButton;
import com.mrpoid.gui.engine.Actor.ClickCallback;

/**
 * 虚拟键盘
 * 
 * @author Yichou
 *
 */
public class Keypad extends Director implements ClickCallback {
	public static Bitmap bmp_dpad_n, bmp_dpad_p, bmp_btn_bg_n, bmp_btn_bg_p;
	public static Bitmap bmp_float_menu, bmp_float_menu_p;

	
	public static void loadBmp(Resources res) {
		if(bmp_dpad_n != null && !bmp_dpad_n.isRecycled())
			return;
		
		releaseBmp();
		bmp_dpad_n = BitmapFactory.decodeResource(res, R.drawable.dpad_n);
		bmp_dpad_p = BitmapFactory.decodeResource(res, R.drawable.dpad_p);
		
		bmp_btn_bg_n = BitmapFactory.decodeResource(res, R.drawable.btn_bg_n);
		bmp_btn_bg_p = BitmapFactory.decodeResource(res, R.drawable.btn_bg_p);
		
		bmp_float_menu = BitmapFactory.decodeResource(res, R.drawable.float_menu);
		bmp_float_menu_p = BitmapFactory.decodeResource(res, R.drawable.float_menu_press);
	} 
	
	public static void releaseBmp() {
		if(bmp_dpad_n != null) bmp_dpad_n.recycle();
		if(bmp_dpad_p != null) bmp_dpad_p.recycle();
		if(bmp_btn_bg_n != null) bmp_btn_bg_n.recycle();
		if(bmp_btn_bg_p != null) bmp_btn_bg_p.recycle();
		if(bmp_float_menu != null) bmp_float_menu.recycle();
		if(bmp_float_menu_p != null) bmp_float_menu_p.recycle();
	}
	
	/**
	 * 键盘布局器
	 * 
	 * @author Yichou
	 *
	 */
	public static interface KeyLayouter {
		/**
		 * 自定义键盘布局
		 * 
		 * @param keypad
		 * @param parent 容器
		 * @param landscape
		 * @param mode
		 */
		public void layout(Keypad keypad, ActorGroup root, boolean landscape, int mode);
		
		/**
		 * 视图尺寸改变通知
		 * 
		 * @param neww
		 * @param newh
		 */
		public void resize(int neww, int newh);
	}
	
	public static interface OnKeyEventListener {
		public boolean onKeyDown(int key);

		public boolean onKeyUp(int key);
	}
	
	private static Keypad instance;
	
	private static final class InstanceHolder{
		static final Keypad INSTANCE = new Keypad();
	}
	
	public static Keypad getInstance() {
		if(instance == null){
			instance = InstanceHolder.INSTANCE;
		}
		return instance;
	}
	
	private View view;
	private Paint paint;
	private KeyLayouter layouter;
	private FloatMenuButton floatMenuBtn;
	private ActorGroup rootGroup;
	private int mode;
	private OnKeyEventListener mListener;
	
	
	private Keypad() {
		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(0xfff0f0f0);
		paint.setStyle(Paint.Style.FILL);
		paint.setShadowLayer(0, 0, 0, 0);
		
		rootGroup = new ActorGroup(this);
		addChild(rootGroup);
	}
	
	public void setLayouter(KeyLayouter layouter) {
		this.layouter = layouter;
	}
	
	public void setOnKeyEventListener(OnKeyEventListener l) {
		this.mListener = l;
	}
	
	public int getMode() {
		return mode;
	}
	
	public void setMode(int mode) {
		if(this.mode != mode){
			this.mode = mode;
//			reset();
		}
	}
	
	public void switchMode() {
		mode = (mode + 1) % 3;
		reset();
	}
	
	@Override
	public void draw(Canvas canvas, Paint paint) {
		super.draw(canvas, this.paint);
	}
	
	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		return super.dispatchTouchEvent(event);
	}
	
	public static File getXml(boolean isLandscape, int mode) {
//		return EmuPath.getPublicFilePath(isLandscape ? 
		return EmulatorApplication.getContext().getFileStreamPath(isLandscape ? 
				String.format("keypad_land_%d.xml", mode) : String.format("keypad_%d.xml", mode));
	}
	
	public void readFromXml(boolean landscape) throws Exception {
		InputStream is = new FileInputStream(getXml(landscape, mode));
		
		if(is.available() <= 0)
			throw new RuntimeException();

		XmlPullParser pullParser = Xml.newPullParser();
		pullParser.setInput(is, "UTF-8"); // 为Pull解释器设置要解析的XML数据
		int event = pullParser.getEventType();

		ActorGroup group = null;
		
		Rect rect= new Rect();
		view.getWindowVisibleDisplayFrame(rect);
		int yAdd = -rect.top;
		
		while (event != XmlPullParser.END_DOCUMENT) {
			switch (event) {
			case XmlPullParser.START_TAG: {
				if ("root".equals(pullParser.getName())) {
					// pullParser.getAttributeValue(null, "opacity")
					int opacity = Integer.valueOf(pullParser.getAttributeValue(0));
					setOpacity(opacity);

//					float scale = Float.valueOf(pullParser.getAttributeValue(1));
				}
				else if ("group".equals(pullParser.getName())) {
					group = new ActorGroup(this);
					boolean visable = Boolean.valueOf(pullParser.getAttributeValue(0));
					int x = Integer.valueOf(pullParser.getAttributeValue(1));
					int y = Integer.valueOf(pullParser.getAttributeValue(2));
					group.setPosition(x, y+yAdd);
					group.setVisible(visable);
					rootGroup.addChild(group);
				}
				else if ("key".equals(pullParser.getName())) {
					boolean visable = Boolean.valueOf(pullParser.getAttributeValue(2));
					
					if(!visable) break;
					
					String title = pullParser.getAttributeValue(0);
					int value = Integer.valueOf(pullParser.getAttributeValue(1));
					int x = Integer.valueOf(pullParser.getAttributeValue(3));
					int y = Integer.valueOf(pullParser.getAttributeValue(4));

					TextButton button = new TextButton(this, bmp_btn_bg_n, bmp_btn_bg_p, title, value);
					button.setVisible(visable);
					button.setClickCallback(this);
					
					if (group != null) {
						group.addChild(button);
						button.setPosition(x, y);
					} else {
						button.setPosition(x, y+yAdd);
						rootGroup.addChild(button);
					}
				}
				else if ("dpad".equals(pullParser.getName())) {
					DPad pad = new DPad(this, bmp_dpad_n, bmp_dpad_p);
					boolean visable = Boolean.valueOf(pullParser.getAttributeValue(0));
					int x = Integer.valueOf(pullParser.getAttributeValue(1));
					int y = Integer.valueOf(pullParser.getAttributeValue(2));
					pad.setPosition(x, y+yAdd);
					pad.setVisible(visable);
					pad.setClickCallback(this);
					
					rootGroup.addChild(pad);
				}
				break;
			}

			case XmlPullParser.END_TAG: {
				if ("group".equals(pullParser.getName())) {
					group = null;
				}
				break;
			}

			}
			event = pullParser.next();
		}
	}
	
	public void reset() {
		if(mode == 0){
			rootGroup.setVisible(false);
		} else {
			rootGroup.setVisible(true);
			
			rootGroup.removeAllChild();
			boolean landScape = (viewW > viewH);
			
			try {
				readFromXml(landScape);
			} catch (Exception e) {
				if (layouter != null) {
					layouter.resize(viewW, viewH);
					layouter.layout(this, rootGroup, landScape, mode);
				}
			}
		}
		
		if(floatMenuBtn != null){
			removeChild(floatMenuBtn);
			floatMenuBtn = null;
		}
		
		//添加拖拽按钮
		floatMenuBtn = new FloatMenuButton(this, 
				bmp_float_menu, bmp_float_menu_p,
				1025);
		floatMenuBtn.setClickCallback(this);
		addChild(floatMenuBtn);
		
		floatMenuBtn.setVisible(Prefer.showFloatButton);
		
		invalida(null);
	}
	
	public void setOpacity(int a) {
		paint.setAlpha(a);
	}
	
	public int getOpacity() {
		return paint.getAlpha();
	}
	
	private DefKeyLayouter defKeyLayouter;
	
	public void attachView(View view) {
		this.view = view;
		
		int size = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                20, 
                view.getResources().getDisplayMetrics());
		paint.setTextSize(size);
		
		if(defKeyLayouter == null)
			defKeyLayouter = new DefKeyLayouter(view.getResources());
		setLayouter(defKeyLayouter);
	}
	
	@Override
	public void setViewSize(int width, int height) {
		super.setViewSize(width, height);
		
		reset();
	}

	@Override
	public void invalida(Actor a) {
		view.invalidate();
	}

	@Override
	public void onClick(int key, boolean down) {
		if( mListener != null) {
			if(down)
				mListener.onKeyDown(key);
			else
				mListener.onKeyUp(key);
		}
	}
}
