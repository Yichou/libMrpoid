package com.mrpoid.gui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;

import com.mrpoid.core.EmuLog;

/**
 * 导演累，管理 Actor
 * 
 * @author Yichou
 *
 */
public abstract class Director {
	private static final String TAG = "Director";
	
	/**
	 * 视图大小，快速访问，设计成 public
	 */
	public int viewW, viewH;
	private ActorGroup rootGroup;
	
	
	public Director() {
		rootGroup = new ActorGroup(this);
	}
	
	public void setViewSize(int width, int height) {
		EmuLog.i(TAG, "setViewSize(" + width + ", " + height + ")");
		this.viewW = width;
		this.viewH = height;
	}
	
	public void addChild(Actor actor, int zOrder) {
		rootGroup.addChild(actor, zOrder);
	}
	
	public void addChild(Actor actor) {
		rootGroup.addChild(actor);
	}
	
	public void removeAllChild() {
		rootGroup.removeAllChild();
	}
	
	public void removeChild(Actor actor) {
		rootGroup.removeChild(actor);
	}
	
	public void removeChild(int index) {
		rootGroup.removeChild(index);
	}
	
	public ActorGroup getRoot() {
		return rootGroup;
	}
	
	public Position getPositionInWorld(Actor actor, Position pos) {
		pos.set(actor.x, actor.y);
		ActorGroup parent = actor.getParent();
		while(parent != null){
			pos.add(parent.x, parent.y);
			parent = parent.getParent();
		}
		return pos;
	}
	
	private boolean touchDowned = false;
	
	public boolean dispatchTouchEvent(MotionEvent event) {
		float fx = event.getX();
		float fy = event.getY();
		
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN: {
			if(rootGroup.touchDown(fx, fy)){
				touchDowned = true;
				return true;
			}
			break;
		}
		
		case MotionEvent.ACTION_MOVE: {
			if(touchDowned){
				rootGroup.touchMove(fx, fy);
				return true;
			}
			break;
		}
		
		case MotionEvent.ACTION_UP: {
			if(touchDowned){
				touchDowned = false;
				rootGroup.touchUp(fx, fy);
				return true;
			}
			break;
		}
		}
		
		return false;
	}
	
	public void draw(Canvas canvas, Paint paint) {
		rootGroup.draw(canvas, paint);
	}
	
	public abstract void invalida(Actor a);
}
