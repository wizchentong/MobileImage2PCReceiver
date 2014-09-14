package com.czt.mobileimage2pc.receiver2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class TCPReceiverThread extends Thread {
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

			try {
				mTCPClient = new Socket(mAddress.remove(0), 19586);
				mTCPClient.setSoTimeout(10000);
				// 获取Socket的输入流，用来接收从服务端发送过来的数据
				BufferedReader buf = new BufferedReader(new InputStreamReader(
						mTCPClient.getInputStream()));
				while (true) {
					String str = buf.readLine();
					if ("bye".equals(str)) {
						break;
					}
					parseThread.add(str);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (mTCPClient != null) {
					// 如果构造函数建立起了连接，则关闭套接字，如果没有建立起连接，自然不用关闭
					try {
						mTCPClient.close();// 只关闭socket，其关联的输入输出流也会被关闭
						mTCPClient = null;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

		}
	}

	private List<InetAddress> mAddress = new ArrayList<InetAddress>();
	private Socket mTCPClient;

	public void add(InetAddress ip, String userGuid) {
		// 当前账户有效,非当前用户不接受
		if (userGuid.equals("6f5de636-1d09-4876-9457-d03063b39adf")
				&& !isTCPRunning()) {
			mAddress.add(ip);
		}
	}

	private boolean isTCPRunning() {
		if (mTCPClient != null && mTCPClient.isConnected()) {
			return true;
		}
		return false;
	}
}
