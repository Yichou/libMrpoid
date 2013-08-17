package com.mrpoid.core;

import android.content.res.Resources;
import android.util.TypedValue;

import com.mrpoid.core.Keypad.KeyLayouter;
import com.mrpoid.gui.ActorGroup;
import com.mrpoid.gui.DPad;
import com.mrpoid.gui.TextButton;

public class DefKeyLayouter implements KeyLayouter {
	private Resources mRes;
	private int viewW, viewH;
	
	
	public DefKeyLayouter(Resources mRes) {
		this.mRes = mRes;
	}

	protected void createLand(Keypad keypad) { //横屏
		boolean dpadAtLeft = Prefer.dpadAtLeft;
		boolean smpleMode = mode == 2;
		int MARGIN = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                8, 
                mRes.getDisplayMetrics());
		
		DPad pad = new DPad(keypad, Keypad.bmp_dpad_n, Keypad.bmp_dpad_p);
		pad.setClickCallback(keypad);
		root.addChild(pad);
		
		// 确定
		TextButton btnOk = new TextButton(keypad, Keypad.bmp_btn_bg_n, Keypad.bmp_btn_bg_p,
				"左软", 
				MrDefines.MR_KEY_SOFTLEFT, 0, 0,
				keypad);
		root.addChild(btnOk);
		
		// 返回
		TextButton btnCancel = new TextButton(keypad, Keypad.bmp_btn_bg_n, Keypad.bmp_btn_bg_p,
				"右软", 
				MrDefines.MR_KEY_SOFTRIGHT, 0, 0,
				keypad);
		root.addChild(btnCancel);
		
		ActorGroup numGroup = null;
		if(!smpleMode){
			numGroup = new ActorGroup(keypad);
			root.addChild(numGroup);
			
			int x = 0;
			int y = 0;
			
			for (int i = 0; i < 4; i++) {
				for (int j = 0; j < 3; j++) {
					TextButton button = new TextButton(keypad, Keypad.bmp_btn_bg_n, Keypad.bmp_btn_bg_p,
							titles[i * 3 + j], 
							ids[i * 3 + j], x, y,
							keypad);
					numGroup.addChild(button);
					
					x += Keypad.bmp_btn_bg_n.getWidth() + MARGIN;
				}
				x = 0;
				y += Keypad.bmp_btn_bg_n.getHeight() + MARGIN;
			}
		}
		
		int y0 = viewH - (MARGIN + Keypad.bmp_btn_bg_n.getHeight()) * 4;
		if (dpadAtLeft) {
			pad.setPosition(MARGIN, (viewH - pad.h - MARGIN));

			int x0 = viewW - (Keypad.bmp_btn_bg_n.getWidth() + MARGIN) * 3;

			if(!smpleMode) 
				numGroup.setPosition(x0, y0);
		} else {
			pad.setPosition((viewW - pad.w - MARGIN), (viewH - pad.h - MARGIN));

			if(!smpleMode) 
				numGroup.setPosition(MARGIN, y0);
		}

		btnOk.setPosition(pad.getX(), pad.getY() - btnOk.h - MARGIN);
		btnCancel.setPosition(pad.getRight() - btnCancel.w - MARGIN, pad.getY() - btnCancel.h - MARGIN);
	}
	
	protected void createXhidp(Keypad keypad) {
		boolean dpadAtLeft = Prefer.dpadAtLeft;// || viewW > viewH;
		boolean smpleMode = mode == 2;
		int MARGIN = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                8, 
                mRes.getDisplayMetrics());

		
		DPad pad = new DPad(keypad, Keypad.bmp_dpad_n, Keypad.bmp_dpad_p);
		pad.setClickCallback(keypad);
		root.addChild(pad);
		
		// 确定
		TextButton btnOk = new TextButton(keypad, Keypad.bmp_btn_bg_n, Keypad.bmp_btn_bg_p,
				"左软", 
				MrDefines.MR_KEY_SOFTLEFT, 0, 0,
				keypad);
		root.addChild(btnOk);
		
		// 返回
		TextButton btnCancel = new TextButton(keypad, Keypad.bmp_btn_bg_n, Keypad.bmp_btn_bg_p,
				"右软", 
				MrDefines.MR_KEY_SOFTRIGHT, 0, 0,
				keypad);
		root.addChild(btnCancel);
		
		if (smpleMode) {
			pad.setPosition((viewW - pad.w)/2, (viewH - pad.h - MARGIN));
			
			btnOk.setPosition(MARGIN*2, (viewH - btnOk.h - 2*MARGIN));
			btnCancel.setPosition((viewW - btnCancel.w - 2*MARGIN), btnOk.y);
			
			return;
		}
		
		ActorGroup numGroup = new ActorGroup(keypad);
		root.addChild(numGroup);
		int x = 0;
		int y = 0;
		
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 3; j++) {
				TextButton button = new TextButton(keypad, Keypad.bmp_btn_bg_n, Keypad.bmp_btn_bg_p,
						titles[i * 3 + j], 
						ids[i * 3 + j], x, y,
						keypad);
				numGroup.addChild(button);
				
				x += Keypad.bmp_btn_bg_n.getWidth() + MARGIN;
			}
			x = 0;
			y += Keypad.bmp_btn_bg_n.getHeight() + MARGIN;
		}
		
		{
			pad.setPosition(dpadAtLeft? MARGIN : (viewW - pad.w - MARGIN), (viewH - pad.h - MARGIN));
			
			int x0;
			
			x0 = dpadAtLeft? (viewW - (Keypad.bmp_btn_bg_n.getWidth() + MARGIN)*3) : MARGIN;
			y = viewH - (MARGIN + Keypad.bmp_btn_bg_n.getHeight())*4;
			numGroup.setPosition(x0, y);
			
			btnOk.setPosition(pad.getX(), y);
			btnCancel.setPosition(pad.getRight() - Keypad.bmp_btn_bg_n.getWidth(), y);
		}
	}
	
	int mode;
	ActorGroup root;
	
	@Override
	public void layout(Keypad keypad, ActorGroup root, boolean landscape, int mode) {
		this.mode = mode;
		this.root = root;
		
		if(landscape)
			createLand(keypad);
		else 
			createXhidp(keypad);
	}
	
	@Override
	public void resize(int neww, int newh) {
		viewW = neww;
		viewH = newh;
	}

	private static final String[] titles = {
		"1", "2", "3",
		"4", "5", "6", 
		"7", "8", "9", 
		"*", "0", "#"
	};

	private static final int[] ids = {
		MrDefines.MR_KEY_1, MrDefines.MR_KEY_2, MrDefines.MR_KEY_3,
		MrDefines.MR_KEY_4, MrDefines.MR_KEY_5, MrDefines.MR_KEY_6, 
		MrDefines.MR_KEY_7, MrDefines.MR_KEY_8, MrDefines.MR_KEY_9, 
		MrDefines.MR_KEY_STAR, MrDefines.MR_KEY_0, MrDefines.MR_KEY_POUND
	};
}
