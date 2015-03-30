package org.activecheck.common.net;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.activecheck.common.nagios.NagiosServiceReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TcpActivecheckServer extends ActivecheckServer {
	private static final Logger logger = LoggerFactory
			.getLogger(TcpActivecheckServer.class);
	private final ServerSocket serverSocket;

	public TcpActivecheckServer(InetAddress bindAddress, int bindPort)
			throws IOException {
		serverSocket = new ServerSocket(bindPort, 0, bindAddress);
		logger.info("Listening on " + bindAddress + ":" + bindPort);
	}

	@Override
	public void run() {
		while (true) {
			try {
				Socket socket = serverSocket.accept();
				try {
					ObjectInputStream objectInput = new ObjectInputStream(
							socket.getInputStream());
					Object object = objectInput.readObject();
					if (object instanceof NagiosServiceReport) {
						logger.debug("Received packet from "
								+ socket.getRemoteSocketAddress());
						setChanged();
						notifyObservers(object);
					} else {
						logger.debug("Received unknown payload");
					}
				} catch (IOException e) {
					logger.error("Unable to receive Object: " + e.getMessage());
					logger.trace(e.getMessage(), e);
				} catch (ClassNotFoundException e) {
					logger.error("Received unknown payload: " + e.getMessage());
					logger.trace(e.getMessage(), e);
				} finally {
					socket.close();
				}
			} catch (IOException e) {
				logger.error("Unable to create a server socket: "
						+ e.getMessage());
				logger.trace(e.getMessage(), e);
			}
		}
	}
}
