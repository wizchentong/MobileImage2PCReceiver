package com.czt.mobileimage2pc.receiver;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.czt.mobileimage2pc.receiver.util.Base64;

public class ParseThread extends Thread {
	@Override
	public void run() {
		while(true){
			//没有数据暂时休息
			if(mXmlStrs.size() <= 0){
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			}
			//有数据， 按插入时间顺序处理
			String xml = mXmlStrs.remove(0);
			WizData data = parseXML(xml);
			if(data == null){
				//多尝试一遍
				data = parseXML(xml);
			}
			if(data == null){
				//两次出错，丢弃
				continue;
			}
			
			int count = data.fileCount;
			String fileName = data.fileName;
			long fileLength = data.fileLength;
			
			//单块就可以的情况下不必加入到多块处理策略中
			if(count == 1){
				generateFile(data.fileData, fileName, fileLength);
				continue;
			}
			//多块处理策略
			String guid = data.fileGuid;
			List<WizData> dataList = mDataMap.get(guid);
			if(dataList == null) {
				dataList = new ArrayList<ParseThread.WizData>();
				mDataMap.put(guid, dataList);
			}
			
			dataList.add(data);
			
			if(dataList.size() >= count) {
				
				//已满
				//先排序
				Collections.sort(dataList, new Comparator<WizData>() {

					@Override
					public int compare(WizData o1, WizData o2) {
						return o1.fileIndex - o2.fileIndex;
					}
				});
				
				//输出文件
				byte[] finalData = dataList.get(0).fileData;
				for(int i = 1; i < dataList.size() ; i++){
					byte[] tempData = dataList.get(i).fileData;
					finalData = byteMerge(finalData, tempData);
				}
				generateFile(finalData, fileName, fileLength);
				mDataMap.remove(guid);
				dataList.clear();
				dataList = null;
			}
			
		}
	}

	private void generateFile(byte[] bytesArray, String fileName, long fileLength) {
		FileOutputStream fos = null;
		File file = new File("d:/" + fileName);
		try {
			// Specify the file path here
			fos = new FileOutputStream(file);
			fos.write(bytesArray);
			fos.flush();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			try {
				if (fos != null) {
					fos.close();
				}
			} catch (IOException ioe) {
				System.out.println("Error in closing the Stream");
			}
		}
		if(file.length() == fileLength){
			System.out.println("File Written Successfully");
		}
	}
	private WizData parseXML(String xml) {
        SAXParserFactory saxfac = SAXParserFactory.newInstance();

        try {
            SAXParser saxparser = saxfac.newSAXParser();
            InputStream is = new ByteArrayInputStream(xml.getBytes()); 
            MySAXHandler handler = new MySAXHandler();
            saxparser.parse(is, handler);
            return handler.getData();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
	}

class MySAXHandler extends DefaultHandler{
	String currentTagName = "";
	WizData mData = null ;
	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		currentTagName = qName ;
		if("wiz-data".equals(qName)){
			mData = new WizData();
		}
	}
	
	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		String str = new String(ch,start,length);
		if("user-name".equals(currentTagName)){
			mData.userName = str;
		}else if("guid".equals(currentTagName)){
			mData.fileGuid = str;
		}else if("name".equals(currentTagName)){
			mData.fileName = str;
		}else if("type".equals(currentTagName)){
			mData.fileType = str;
		}else if("length".equals(currentTagName)){
			mData.fileLength = Long.parseLong(str);
		}else if("index".equals(currentTagName)){
			mData.fileIndex = Integer.parseInt(str);
		}else if("count".equals(currentTagName)){
			mData.fileCount = Integer.parseInt(str);
		}else if("data".equals(currentTagName)){
			mData.fileData = Base64.decode(str);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		currentTagName = "";
	}

	public WizData getData(){
		return mData ;
	}
}
	//java 合并两个byte数组
	public byte[] byteMerge(byte[] byte_1, byte[] byte_2){
		byte[] byte_3 = new byte[byte_1.length+byte_2.length];
		System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);
		System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);
		return byte_3;
	}
	private List<String> mXmlStrs = new ArrayList<String>();
	private Map<String, List<WizData>> mDataMap = new HashMap<String, List<WizData>>();
	public void add(String xml){
		if(xml.contains("wizlxn006@wiz.cn")){
			mXmlStrs.add(xml);
		}
	}
	private class WizData{
		private String userName;
		private String fileGuid;
		private String fileName;
		private String fileType;
		private long fileLength;
		private int fileIndex;
		private int fileCount;
		private byte[] fileData;
	}
}
