package com.mrpoid.gui.engine;

import com.mrpoid.core.Prefer;

import android.graphics.Bitmap;

/**
 * 浮动菜单按钮
 * 
 * @author Yichou
 *
 */
public class FloatMenuButton extends TextButton {
	public FloatMenuButton(Director am, Bitmap bmpNormal, Bitmap bmpPressed, int id) {
		super(am, bmpNormal, bmpPressed, null, id);
		
		// 防止他到屏幕外面去了
		if (Prefer.lrbX < 0)
			Prefer.lrbX = 0;
		else if (Prefer.lrbX + w > am.viewW)
			Prefer.lrbX = (int) (am.viewW - w);

		if (Prefer.lrbY < 0)
			Prefer.lrbY = 0;
		else if (Prefer.lrbY + h > am.viewH)
			Prefer.lrbY = (int) (am.viewH - h);
		this.x = Prefer.lrbX;
		this.y = Prefer.lrbY;
	}

	private boolean moved;
	private float lastX, lastY; 
	
	@Override
	public boolean touchDown(float fx, float fy) {
		moved = false;
		lastX = fx+this.x;
		lastY = fy+this.y;
		
		invalida();
		
		return true;
	}

	@Override
	public void touchMove(float fx, float fy) {
		fx += this.x;
		fy += this.y;
		
		if((Math.abs(lastX - fx) > 5 && Math.abs(lastY - fy) > 5)){
			moved = true;
		}
			x += (fx - lastX);
			y += (fy - lastY);
			lastX = fx;
			lastY = fy;
			
			if(x < 0) 
				x=0;
			else if(x+w > am.viewW) 
				x = am.viewW - w;
			
			if(y < 0) 
				y=0;
			else if(y+h > am.viewH) 
				y = am.viewH-h;
			
			invalida();
	}

	@Override
	public void touchUp(float fx, float fy) {
		Prefer.lrbX = (int) x;
		Prefer.lrbY = (int) y;

		if(!moved){
			clicked(id, false);
		}
		moved = false;

		invalida();
	}
}
