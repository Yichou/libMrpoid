package com.mrpoid.gui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import com.mrpoid.core.MrDefines;

/**
 * 方向键盘
 * 
 * @author Yichou
 * 
 */
public class DPad extends Actor {
	private Bitmap bmpNormal, bmpPressed;

	private int keyWidth, keyHeight;
	private int px, py;
	private int curPressKey = -1;
	private final Rect rSrc = new Rect(), rDst = new Rect();

	public DPad(Director am, Bitmap bmpNormal, Bitmap bmpPressed) {
		super(am);

		this.bmpNormal = bmpNormal;
		this.bmpPressed = bmpPressed;
		this.w = bmpNormal.getWidth();
		this.h = bmpNormal.getHeight();
		keyWidth = (int) (w / 3);
		keyHeight = (int) (h / 3);
		this.id = MrDefines.MR_KEY_SELECT;
	}

	@Override
	public boolean isHit(float fx, float fy) {
		if (super.isHit(fx, fy)) {
			if (getPressKey(fx, fy) != -1) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void draw(Canvas canvas, Paint paint) {
		canvas.drawBitmap(bmpNormal, x, y, paint);
		if (curPressKey != -1) {
			rSrc.set((px * keyWidth), py * keyHeight, (px + 1) * keyWidth, (py + 1) * keyHeight);
			rDst.set(rSrc);
			rDst.offset((int) x, (int) y);
			canvas.drawBitmap(bmpPressed, rSrc, rDst, paint);
		}
	}

	/**
	 * 为省事而定义的按键映射数组
	 */
	private static final byte[][] keyMap = { 
		{ -1, MrDefines.MR_KEY_UP, -1 }, 
		{ MrDefines.MR_KEY_LEFT, MrDefines.MR_KEY_SELECT, MrDefines.MR_KEY_RIGHT }, 
		{ -1, MrDefines.MR_KEY_DOWN, -1 } 
	};

	private int getPressKey(float fx, float fy) {
		px = (int) (fx / keyWidth);
		py = (int) (fy / keyHeight);

		if (px > 2)
			px = 2;
		else if (px < 0)
			px = 0;
		if (py > 2)
			py = 2;
		else if (py < 0)
			py = 0;

		return keyMap[py][px];
	}

	@Override
	public boolean touchDown(float fx, float fy) {
		curPressKey = getPressKey(fx, fy);

		if (curPressKey != -1) {
			invalida();
			clicked(curPressKey, true);
		}

		return true;
	}

	@Override
	public void touchMove(float fx, float fy) {
		int tmp = getPressKey(fx, fy);

		if (tmp != -1 && tmp != curPressKey) {
			invalida();
			clicked(curPressKey, false);
			curPressKey = tmp;
			clicked(curPressKey, true);
		}
	}

	@Override
	public void touchUp(float fx, float fy) {
		if (curPressKey != -1) {
			clicked(curPressKey, false);
			curPressKey = -1;
			invalida();
		}
	}
}
