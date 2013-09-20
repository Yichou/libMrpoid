package com.mrpoid.core;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Environment;
import android.view.MotionEvent;
import android.widget.Toast;

/**
 * MRP 屏幕
 * 
 * @author Yichou
 * 
 * 2013-4-19 13:20:02
 */
public class MrpScreen {
	private static final String TEST = "1.你好，Hellogfa"; 
	
	private Emulator emulator;
	
	public Bitmap bitmap, cacheBitmap;
	public Canvas cacheCanvas = new Canvas();
	private RectF region = new RectF(); //绘制区域
	private float scaleX, scaleY;
	public Point size = new Point(); //大小
	private int viewW, viewH;
	private int screenW, screenH;
	
	private float CHR_H;
	private Paint paint;
	private Rect textRect = new Rect(); //测量字符用
	private Rect textRectD = new Rect(); //测量字符用
	private char[] tmpBuf = new char[2];
	
	private Typeface mTypeface;
	
	
	public MrpScreen(Emulator emulator) {
		super();
		this.emulator = emulator;
		
		paint = new Paint();
		paint.setTextSize(16);
		paint.setAntiAlias(true);
//		paint.setStyle(Paint.Style.FILL);
		
		mTypeface = Typeface.createFromAsset(emulator.getContext().getAssets(), "fonts/COUR.TTF");
		paint.setTypeface(mTypeface);
	}
	
	public void setTextSize(int size) {
		paint.setTextSize(size);
		paint.getTextBounds(TEST, 0, TEST.length(), textRect);
		tmpBuf[0] = '鼎'; 
		tmpBuf[1] = 0;
		paint.getTextBounds(tmpBuf, 0, 1, textRectD);
		CHR_H = textRect.height();
	}
	
	public void dispose() {
		if(bitmap != null) {
			bitmap.recycle();
			bitmap = null;
		}
		
		if(cacheBitmap != null){
			cacheBitmap.recycle();
			cacheBitmap = null;
		}
	}
	
	public void onTouchEvent(MotionEvent event) {
		float fx =  event.getX();
		float fy =  event.getY();
		
		if(region.contains(fx, fy)){
			int x = (int) ((fx-region.left)/scaleX);
			int y = (int) ((fy-region.top)/scaleY);
			
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				emulator.postMrpEvent(MrDefines.MR_MOUSE_DOWN, x, y);
				break;
			case MotionEvent.ACTION_MOVE:
				emulator.postMrpEvent(MrDefines.MR_MOUSE_MOVE, x, y);
				break;
			case MotionEvent.ACTION_UP:
				emulator.postMrpEvent(MrDefines.MR_MOUSE_UP, x, y);
				break;
			}
		}
	}
	
//	private int lastY = 0, buttom = 0;
	public void N2J_drawChar(short c, int x, int y, int color) {
		tmpBuf[0] = (char) c; 
		tmpBuf[1] = 0;
		paint.setColor(color);
		
		//顶、左 对齐 +charH-2
		cacheCanvas.drawText(tmpBuf, 0,1, x, y+CHR_H-2, paint);
		
//		paint.getTextBounds(tmpBuf, 0, 1, textRect);
//		if(lastY != y){ //新的一行
//			lastY = y;
//			buttom = y;
//		}
		
//		int tmp = 0;
		
//		if(c >= 33 && c <= 126){ //ascii可见字符
//			tmp = y - textRectD.top + textRect.top;
//		}else {
//			tmp = y - textRect.top;
//		}
		
		//顶、左 对齐 +charH-2
//		cacheCanvas.drawText(tmpBuf, 0,1, x+textRect.left, tmp, paint);
	}
	
	public void N2J_measureChar(short ch) {
		tmpBuf[0] = (char)ch;
		tmpBuf[1] = 0;
		
		emulator.N2J_charW = (int)Math.ceil(paint.measureText(tmpBuf, 0, 1));
		paint.getTextBounds(tmpBuf, 0, 1, textRect);
		emulator.N2J_charH = textRect.height();
		if(emulator.N2J_charH > CHR_H)
			CHR_H = emulator.N2J_charH;
//		Log.i("---", "measure" + String.valueOf(tmpBuf));
	}
	
	public void init() {
		screenW = screenSize.width;
		screenH = screenSize.height;
		setMrpScreenSize(screenW, screenH);
		clear(Color.WHITE);
	}
	
	/**
	 * emulatorView 的尺寸改变后应该重新设置屏幕缩放比例
	 * 
	 * @param width
	 * @param height
	 */
	public void setViewSize(int width, int height) {
		viewW = width;
		viewH = height;
		initScale();
	}
	
	public void initScale() {
		switch (screenSize.scaleMode) {
		case SCALE_STRE:
			this.scaleX = viewW/(float)screenW;
			this.scaleY = viewH/(float)screenH;
			break;
		case SCALE_2X:
			this.scaleX = this.scaleY = 2;
			break;
		case SCALE_PRO: //取最小值
			this.scaleY = this.scaleX = Math.min(viewW/(float)screenW, viewH/(float)screenH);
			break;
			
		default:
			this.scaleY = this.scaleX = 1;
			break;
		}
		
		setScale(this.scaleX, this.scaleY);
	}
	
	/**
	 * 设置 mrp 屏幕大小
	 * 
	 * @param w
	 * @param h
	 */
	private void setMrpScreenSize(int w, int h) {
		if(size.equals(w, h)) return;
		
		switch (w) {
		case 176:
			setTextSize(14);
			break;
			
		case 240:
			setTextSize(16);
			break;
			
		case 320:
			setTextSize(20);
			break;
			
		case 480:
			setTextSize(40);
			break;
		}
		
		size.set(w, h);
		createBitmap();
	}
	
	private void createBitmap() {
		//重新创建屏幕位图
		if(bitmap != null)
			bitmap.recycle();
		if(cacheBitmap != null)
			cacheBitmap.recycle();
		
		cacheBitmap = Bitmap.createBitmap(size.x, size.y, Config.RGB_565);
		bitmap = Bitmap.createBitmap(size.x, size.y, Config.RGB_565);
		cacheCanvas.setBitmap(cacheBitmap);
		
		native_reset(cacheBitmap, bitmap, size.x, size.y);
	}
	
	public void setPosition(int x, int y) {
		region.offsetTo(x, y);
	}
	
	public void clear(int color) {
		cacheCanvas.drawColor(color);
		bitmap.eraseColor(color);
	}
	
	/**
	 * 设置屏幕 x,y 缩放比
	 * 
	 * @param sx
	 * @param sy
	 */
	public void setScale(float sx, float sy) {
		this.scaleX = sx;
		this.scaleY = sy;
		
		float w = size.x*scaleX;
		region.left = (emulator.emulatorView.getWidth() - w)/2;
		region.right = region.left + w;
		region.top = 0;
		region.bottom = size.y*scaleY;
		
		//竖直居中
//		if(Prefer.keypadMode == 0 && region.height() < viewH){
//			region.offset(0, (viewH-region.height())/2);
//		}
	}
	
	public void pause() {
		
	}
	
	public void resume() {
		if(emulator.isRunning()){
			if(cacheBitmap == null || cacheBitmap.isRecycled()){ //无效了，重启mrp
				
			}
			
			if(bitmap == null || bitmap.isRecycled()){
				bitmap = Bitmap.createBitmap(size.x, size.y, Config.RGB_565);
				native_reset(cacheBitmap, bitmap, size.x, size.y);
			}
		}
	}
	
	public void draw(Canvas canvas) {
//		bitmap.setPixel(0, 0, Color.BLACK);
//		bitmap.setPixel(size.x-1, 0, Color.BLACK);
//		ByteBuffer buffer = ByteBuffer.wrap(screenBuf);
//		bitmap.copyPixelsFromBuffer(buffer);
		
		/**
		 * 刷屏非主线程，这里涉及到 bitmap 同时占有的问题，所以先进底层锁住
		 */
		native_lockBitmap();
		canvas.drawBitmap(bitmap, null, region, null);
		native_unLockBitmap();
	}
	
	private HashMap<String, Bitmap> map = new HashMap<String, Bitmap>(5);
	
	public void freeRes() {
		Set<String> set = map.keySet();
		for(String key : set){
			Bitmap bmp = map.get(key);
			if(bmp != null)
				bmp.recycle();
		}
		map.clear();
	}
	
	private Bitmap N2J_decodeBitmap(String path) {
		Bitmap bmp = map.get(path);
		if(bmp == null){
			bmp = BitmapFactory.decodeFile(path);
			map.put(path, bmp);
		}
		return bmp;
	}
	
	private Rect dRectSrc = new Rect(), dRectDst = new Rect();
	private void N2J_drawBitmap(Bitmap bitmap, int sx, int sy, int sw, int sh) {
		dRectSrc.set(0, 0, bitmap.getWidth()-1, bitmap.getHeight()-1);
		dRectDst.set(sx, sy, sw, sh);
		cacheCanvas.drawBitmap(bitmap, dRectSrc, dRectDst, null);
	}
	
	public enum ScaleMode {
		/** 全屏 */
		SCALE_STRE("stretch"),
		/** 等比拉伸 */
		SCALE_PRO("proportional"),
		/** 2倍 */
		SCALE_2X("2x"),
		SCALE_ORI("original");
		
		String tag;
		
		private ScaleMode(String s){
			tag = s;
		}
	}
	
	private static final class ScreenSize {
		private int width;
		private int height;
		private String size;
		private ScaleMode scaleMode;
	}
	
	private static ScreenSize screenSize;
	
	static {
		screenSize = new ScreenSize();
		screenSize.width = 240;
		screenSize.height = 320;
		screenSize.size = "240x320";
		screenSize.scaleMode = ScaleMode.SCALE_PRO;
	}
	
	public static String getSizeTag() {
		return screenSize.size;
	}
	
	public static String getScaleModeTag() {
		return screenSize.scaleMode.tag;
	}
	
	/**
	 * 解析屏幕尺寸
	 * 
	 * @param size
	 */
	public static void parseScreenSize(String size) {
		String[] ss = size.split("x");
		if(ss == null || ss.length!=2)
			return;
		screenSize.width = Integer.parseInt(ss[0]);
		screenSize.height = Integer.parseInt(ss[1]);
		screenSize.size = size;
	}
	
	/**
	 * 设置屏幕缩放模式
	 */
	public static void parseScaleMode(String mode) {
		ScaleMode scaleMode = ScaleMode.SCALE_STRE;
		for(ScaleMode m : ScaleMode.values()){
			if(m.tag.equals(mode)){
				scaleMode = m;
				break;
			}
		}

		screenSize.scaleMode = scaleMode;
	}
	
	/**
	 * 截屏
	 * 
	 * @param context
	 */
	public void screenShot(Context context) {
		// 获取系统图片存储路径
		File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		if(path == null){
			path = new File(Environment.getExternalStorageDirectory(), "Pictures");
		}
		path = new File(path, "screenshot");
		path.mkdirs();

		// 根据当前时间生成图片名称
		SimpleDateFormat sdf = new SimpleDateFormat("yyyymmddHHmmss", Locale.CHINA);
		String name = sdf.format(new Date()) + ".png";

		path = new File(path, name);
		if(EmuUtils.bitmapToFile(bitmap, path)){
			Toast.makeText(context, "截图成功！\n文件保存在：" + path.getPath(), Toast.LENGTH_SHORT).show();
		}else {
			Toast.makeText(context, "截图失败！", Toast.LENGTH_SHORT).show();
		}
	}
	
	public native void native_lockBitmap();
	public native void native_unLockBitmap();
	public native void hello();
	public native void native_reset(Bitmap cache, Bitmap real, int w, int h);
	public native void native_flushBitmap(Bitmap dstBitmap, int x, int y, int w, int h);
}
