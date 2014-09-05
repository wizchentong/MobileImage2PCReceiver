package com.czt.mobileimage2pc.receiver2;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
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

public class ParseThread2 extends Thread {
	@Override
	public void run() {
		while(true){
			//没有数据暂时休息
			if(mAddress.size() <= 0){
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			}
			try {
				// 客户端请求与本机在19586端口建立TCP连接
				Socket client = new Socket(mAddress.remove(0), 19586);
				client.setSoTimeout(10000);
				// 获取Socket的输入流，用来接收从服务端发送过来的数据
				BufferedReader buf = new BufferedReader(new InputStreamReader(
						client.getInputStream()));
				while (true) {
					try {
						String str = buf.readLine();
						if ("bye".equals(str)) {
							break;
						} 
						// 从服务器端接收数据有个时间限制（系统自设，也可以自己设置），超过了这个时间，便会抛出该异常
						String xml = str;
						WizFile data = parseXML(xml);
						if(data == null){
							//多尝试一遍
							data = parseXML(xml);
						}
						if(data == null){
							//两次出错，丢弃
							continue;
						}
						
						int count = data.count;
						String fileName = data.name;
						long fileLength = data.length;
						
						//单块就可以的情况下不必加入到多块处理策略中
						if(count == 1){
							generateFile(data.data, fileName, fileLength);
							continue;
						}
						//多块处理策略
						String guid = data.guid;
						List<WizFile> dataList = mDataMap.get(guid);
						if(dataList == null) {
							dataList = new ArrayList<WizFile>();
							mDataMap.put(guid, dataList);
						}
						
						dataList.add(data);
						
						if(dataList.size() >= count) {
							
							//已满
							//先排序
							Collections.sort(dataList, new Comparator<WizFile>() {

								@Override
								public int compare(WizFile o1, WizFile o2) {
									return o1.index - o2.index;
								}
							});
							
							//输出文件
							byte[] finalData = dataList.get(0).data;
							for(int i = 1; i < dataList.size() ; i++){
								byte[] tempData = dataList.get(i).data;
								finalData = byteMerge(finalData, tempData);
							}
							generateFile(finalData, fileName, fileLength);
							mDataMap.remove(guid);
							dataList.clear();
							dataList = null;
						}
					} catch (SocketTimeoutException e) {
						System.out.println("Time out, No response");
					}
				}
				if (client != null) {
					// 如果构造函数建立起了连接，则关闭套接字，如果没有建立起连接，自然不用关闭
					client.close(); // 只关闭socket，其关联的输入输出流也会被关闭
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	private List<InetAddress> mAddress = new ArrayList<InetAddress>();
	public void add(InetAddress ip, String userName){
		//当前账户有效,非当前用户不接受
		if(userName.contains("xyzchy2@wiz.cn")){
			mAddress.add(ip);
		}
	}
	
	private void generateFile(byte[] bytesArray, String fileName, long fileLength) {
		BufferedOutputStream fos = null;
		File file = new File("d:/" + fileName);
		try {
			// Specify the file path here
			fos = new BufferedOutputStream(new FileOutputStream(file));
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
	private WizFile parseXML(String xml) {
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

	private class MySAXHandler extends DefaultHandler{
		String currentTagName = "";
		WizFile mData = null ;
		@Override
		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			currentTagName = qName ;
			mStringBuilder = new StringBuilder();
			if("file".equals(qName)){
				mData = new WizFile();
			}
		}
		private StringBuilder mStringBuilder;
		@Override
		public void characters(char[] ch, int start, int length)
				throws SAXException {
			mStringBuilder.append(ch, start, length);
		}
	
		@Override
		public void endElement(String uri, String localName, String qName)
				throws SAXException {
			String str = mStringBuilder.toString();
			if("guid".equals(currentTagName)){
				mData.guid = str;
			}else if("name".equals(currentTagName)){
				mData.name = str;
			}else if("type".equals(currentTagName)){
				mData.type = str;
			}else if("length".equals(currentTagName)){
				mData.length = Long.parseLong(str);
			}else if("index".equals(currentTagName)){
				mData.index = Integer.parseInt(str);
			}else if("count".equals(currentTagName)){
				mData.count = Integer.parseInt(str);
			}else if("data".equals(currentTagName)){
				mData.data = Base64.decode(str);
			}
			currentTagName = "";
		}
	
		public WizFile getData(){
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
	private Map<String, List<WizFile>> mDataMap = new HashMap<String, List<WizFile>>();
	private class WizFile{
		private String guid;
		private String name;
		private String type;
		private long length;
		private int index;
		private int count;
		private byte[] data;
	}
}
