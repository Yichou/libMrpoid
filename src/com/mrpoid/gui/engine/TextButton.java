package com.mrpoid.gui.engine;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;


/**
 * 显示文本按钮
 * 
 * @author Yichou
 */
public class TextButton extends Actor {
	private String title;
	private Bitmap bmpNormal, bmpPressed;
	
	
	public TextButton(Director am, 
			Bitmap bmpNormal, Bitmap bmpPressed,
			String title, int id, float x, float y,
			ClickCallback cb) {
		super(am);
		
		this.title = title;
		this.id = id;
		this.x = x;
		this.y = y;
		this.bmpNormal = bmpNormal;
		this.bmpPressed = bmpPressed;
		this.w = bmpNormal.getWidth();
		this.h = bmpNormal.getHeight();
		this.clickCallback = cb;
	}
	
	public TextButton(Director am, 
			Bitmap bmpNormal, Bitmap bmpPressed,
			String title, int id) {
		this(am, bmpNormal, bmpPressed, title, id, 0, 0, null);
	}

	private static Rect rBounds = new Rect();
	
	public void draw(Canvas canvas, Paint paint) {
		canvas.drawBitmap(pressed? bmpPressed : bmpNormal, x, y, paint);

		if(title != null){
			paint.getTextBounds(title, 0, title.length(), rBounds);
//			paint.setShadowLayer(3, 3.0f, 3.0f, 0x80000000);
			canvas.drawText(title, x+(w-rBounds.width())/2, y+h-(h-rBounds.height())/2, paint);
//			paint.setShadowLayer(0, 3.0f, 3.0f, 0x80000000);
		}
	}
	
	public boolean touchDown(float fx, float fy) {
		invalida();
		clicked(id, true);
		return true;
	}
	
	public void touchUp(float fx, float fy){
		invalida();
		clicked(id, false);
	}

	@Override
	public void touchMove(float fx, float fy) {
		
	}
	
	public String getTitle() {
		return title;
	}
}