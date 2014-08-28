package com.czt.mobileimage2pc.receiver;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UDPReceiver {

	public static void main(String args[]) throws Exception {

		new UDPReceiver().lanchApp();
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
				ParseThread parseThread = new ParseThread();
				parseThread.start();
				while (true) {
					DatagramPacket packet = new DatagramPacket(by, by.length);
					dgSocket.receive(packet);
					
					String xml = new String(packet.getData(), 0, packet.getLength());
					
					parseThread.add(xml);
					
					System.out.println("接收到数据大小:" + xml.length());
					System.out.println("接收到的数据为：" + xml);
					System.out.println("recevied message is ok.");
				}
//				dgSocket.close();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}

}
