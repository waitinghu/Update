package com.seuic.update;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;

import android.util.Xml;

public class DownloadXMLParser {

	public static UpdateInfo parse(InputStream inputStream) {
	    UpdateInfo update = null;
	    ArrayList<String> description = null;
		// 获得XmlPullParser解析器
		XmlPullParser xmlParser = Xml.newPullParser();
		try {
			xmlParser.setInput(inputStream, "UTF-8");
			// 获得解析到的事件类别，这里有开始文档，结束文档，开始标签，结束标签，文本等等事件。
			int evtType = xmlParser.getEventType();
			// 一直循环，直到文档结束
			while (evtType != XmlPullParser.END_DOCUMENT) {
				String tag = xmlParser.getName();
				switch (evtType) {
				case XmlPullParser.START_TAG:
					// 通知信息
					if (tag.equalsIgnoreCase("update")) {
					    update = new UpdateInfo();
					} else if (update != null) {
					    
						if (tag.equalsIgnoreCase("version")) {
						    update.setVersion(Integer.parseInt(xmlParser.nextText()));
						} else if (tag.equalsIgnoreCase("name")) {
						    update.setName(xmlParser.nextText());
						} else if (tag.equalsIgnoreCase("description")) {
						    description = new ArrayList<String>();
						} else if (tag.equalsIgnoreCase("item")) {
						    description.add(xmlParser.nextText());
						} else if (tag.equalsIgnoreCase("size")){
						    update.setSize(xmlParser.nextText());
						}
					}
					break;
				case XmlPullParser.END_TAG:
				    
				    if (tag.equalsIgnoreCase("description")) {
				        update.setDescription(description);
				    }
					break;
				}
				evtType = xmlParser.next();
			}
		} catch (Exception e) {

		} finally {
		    
			try {
				inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return update;
	}
}
