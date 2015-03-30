package org.activecheck.common.net;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import org.activecheck.common.nagios.NagiosServiceReport;

public class TcpActivecheckClient extends ActivecheckClient {
	public TcpActivecheckClient(String address, int port) {
		super(address, port);
	}

	@Override
	public void send(NagiosServiceReport report) {
		try {
			Socket socket = new Socket(address, port);
			try {
				ObjectOutputStream objectOutput = new ObjectOutputStream(
					socket.getOutputStream());
				objectOutput.writeObject(report);
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				socket.close();
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
