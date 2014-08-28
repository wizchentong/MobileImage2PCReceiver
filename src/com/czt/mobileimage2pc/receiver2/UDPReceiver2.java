package com.czt.mobileimage2pc.receiver2;
import java.net.DatagramPacket;
import java.net.DatagramSocket;


public class UDPReceiver2 {

	public static void main(String args[]) throws Exception {

		new UDPReceiver2().lanchApp();
	}

	private void lanchApp() {
		ReceiveThread receiveThread = new ReceiveThread();
		receiveThread.start();
	}
	private class ReceiveThread extends Thread {
		@Override 
		public void run() {
			try {
				DatagramSocket dgSocket = new DatagramSocket(18695);
				byte[] by = new byte[2*1024];
				ParseThread2 parseThread = new ParseThread2();
				parseThread.start();
				while (true) {
					DatagramPacket packet = new DatagramPacket(by, by.length);
					dgSocket.receive(packet);
					String userName = new String(packet.getData(), 0, packet.getLength());
					parseThread.add(packet.getAddress(), userName);
					System.out.println("数据长度：" + userName.length());
					System.out.println("数据内容：" + userName);
				}
//				dgSocket.close();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}

}
