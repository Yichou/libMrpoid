package com.mrpoid.gui;

import android.graphics.Canvas;
import android.graphics.Paint;


/**
 * 演员类
 * 
 * @author Yichou
 *
 */
public abstract class Actor {
	public static interface ClickCallback {
		public void onCLick(int key, boolean down);
	}
	
	public float x, y, w, h;
	public int id;
	public boolean pressed;
	public Object tag;
	public float scale;
	
	protected boolean isShow;
	protected Director am;
	protected ClickCallback clickCallback;
	protected ActorGroup mParent;
	
	
	public Actor(Director am) {
		super();
		this.am = am;
		isShow = true;
		x = y = w = h = 0;
	}

	public float getRight() {
		return x+w-1;
	}
	
	public float getButtom() {
		return y+h-1;
	}
	
	public void setClickCallback(ClickCallback clickCallback) {
		this.clickCallback = clickCallback;
	}
	
	public int getId() {
		return id;
	}
	
	public float getX() {
		return x;
	}

	public void setX(float x) {
		this.x = x;
	}

	public float getY() {
		return y;
	}

	public void setY(float y) {
		this.y = y;
	}
	
	public float getW() {
		return w;
	}
	
	public float getH() {
		return h;
	}
	
	public void setSize(float w, float h) {
		this.w = w;
		this.h = h;
	}
	
	public void setPosition(float x, float y) {
		this.x = x;
		this.y = y;
	}
	
	public void move(float xOff, float yOff) {
		this.x += xOff;
		this.y += yOff;
	}

	public void invalida() {
		am.invalida(this);
	}
	
	public boolean isHit(float fx, float fy) {
		if (fx < 0 || fx > w || fy < 0 || fy > h)
			return false;
		return true;
	}
	
	public boolean isShow() {
		return isShow;
	}
	
	public void setVisible(boolean isShow) {
		this.isShow = isShow;
	}
	
	public void setTag(Object tag) {
		this.tag = tag;
	}
	
	public Object getTag() {
		return tag;
	}
	
	protected void clicked(int key, boolean down) {
		if(clickCallback != null){
			clickCallback.onCLick(key, down);
		}
	}
	
	protected void setParent(ActorGroup parent) {
		this.mParent = parent;
	}
	
	public ActorGroup getParent() {
		return mParent;
	}
	
	public Position getPositionInWorld(Position pos) {
		return am.getPositionInWorld(this, pos);
	}
	
	public abstract void draw(Canvas canvas, Paint paint);
	public abstract boolean touchDown(float fx, float fy);
	public abstract void touchMove(float fx, float fy);
	public abstract void touchUp(float fx, float fy);
}