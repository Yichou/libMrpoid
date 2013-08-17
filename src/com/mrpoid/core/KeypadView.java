package com.mrpoid.core;

import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;



/**
 * 虚拟键盘
 * 
 * @author Yichou
 *
 */
public class KeypadView extends View {
	private Keypad keypad;

	public KeypadView(Context context) {
		super(context);

		keypad = Keypad.getInstance();
		keypad.attachView(this);
		keypad.setOpacity(Prefer.keypadOpacity);
		keypad.reset();
	}
	
	public void switchKeypad() {
		keypad.switchMode();
	}
	
	public void setKeypadOpacity(int o) {
		keypad.setOpacity(o);
		invalidate();
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		keypad.setViewSize(w, h);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		keypad.draw(canvas, null);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return keypad.dispatchTouchEvent(event);
	}
	
//	@Override
//	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//	}
}
