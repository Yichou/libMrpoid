package com.mrpoid.keysprite;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

import android.util.Xml;

public class SampleKeySprite implements KeySprite {
	private ArrayList<Sprite> spriteList;
	
	
	public SampleKeySprite() {
		spriteList = new ArrayList<Sprite>();
	}
	
	public ArrayList<Sprite> getSpriteList() {
		return spriteList;
	}
	
	public void add(Sprite sprite) {
		spriteList.add(sprite);
	}
	
	public void remove(int index) {
		spriteList.remove(index);
	}
	
	public void run(int repeatTimes) {
		
	}
	
	public void toXml(File file) throws Exception{
		FileOutputStream out = new FileOutputStream(file);
        XmlSerializer s = Xml.newSerializer();
       
        s.setOutput(out, "UTF-8");
        s.startDocument("UTF-8", true);
       
        s.startTag(null, "root");
        s.attribute(null, "count", String.valueOf(count()));
       
        
        for(Sprite sprite : spriteList){
        	s.startTag(null, "key");
        	s.attribute(null, "title", sprite.title);
        	s.attribute(null, "value", String.valueOf(sprite.value));
        	s.attribute(null, "time", String.valueOf(sprite.time));
        	s.endTag(null, "key");
        }
        
        s.endTag(null, "root");
        s.endDocument();
        out.flush();
        out.close();
	}
	
	public void fromXml(File file) throws Exception{
		InputStream is = new FileInputStream(file);

		if (is.available() <= 0)
			throw new RuntimeException("save file is invalid!");

		XmlPullParser parser = Xml.newPullParser();
		parser.setInput(is, "UTF-8"); // 为Pull解释器设置要解析的XML数据
		int event = parser.getEventType();

		spriteList.clear();
		
		while (event != XmlPullParser.END_DOCUMENT) {
			switch (event) {
			case XmlPullParser.START_TAG: {
				if("key".equals(parser.getName())){
					Sprite sprite = new Sprite(parser.getAttributeValue(0), 
							Integer.valueOf(parser.getAttributeValue(1)), 
							Integer.valueOf(parser.getAttributeValue(2)));
					
					spriteList.add(sprite);
				}
				
				break;
			}
			}
			
			event = parser.next();
		}
	}

	@Override
	public int count() {
		return spriteList.size();
	}

	@Override
	public Sprite get(int index) {
		return spriteList.get(index);
	}
}
