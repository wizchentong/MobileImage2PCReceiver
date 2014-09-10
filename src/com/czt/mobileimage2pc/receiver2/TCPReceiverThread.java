package com.czt.mobileimage2pc.receiver2;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

public class TCPReceiverThread extends Thread {
	private boolean mIsTCPRunning = false;
	@Override
	public void run() {
		ParseThread2 parseThread = new ParseThread2();
		parseThread.start();
		while (true) {
			// 没有数据暂时休息
			if (mAddress.size() <= 0) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			}
			
			mIsTCPRunning = true;
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
						parseThread.add(str);
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
			mIsTCPRunning = false;
			
		}
	}

	private List<InetAddress> mAddress = new ArrayList<InetAddress>();

	public void add(InetAddress ip, String userGuid) {
		// 当前账户有效,非当前用户不接受
		if (userGuid.contains("6f5de636-1d09-4876-9457-d03063b39adf") && mIsTCPRunning) {
			mAddress.add(ip);
		}
	}
}
