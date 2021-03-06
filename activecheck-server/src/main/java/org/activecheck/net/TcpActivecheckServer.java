package org.activecheck.net;

import org.activecheck.common.nagios.NagiosServiceReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class TcpActivecheckServer extends ActivecheckServer {
    private static final Logger logger = LoggerFactory.getLogger(TcpActivecheckServer.class);
    private final ServerSocket serverSocket;

    public TcpActivecheckServer(InetAddress bindAddress, int bindPort)
            throws IOException {
        serverSocket = new ServerSocket(bindPort, 0, bindAddress);
        logger.info("Listening on {}:{}", bindAddress, bindPort);
    }

    @Override
    @SuppressWarnings("InfiniteLoopStatement")
    public void run() {
        while (true) {
            try (final Socket socket = serverSocket.accept()) {
                try {
                    final ObjectInputStream objectInput = new ObjectInputStream(
                            socket.getInputStream());
                    final Object object = objectInput.readObject();
                    if (object instanceof NagiosServiceReport) {
                        logger.debug("Received packet from {}", socket.getRemoteSocketAddress());
                        setChanged();
                        notifyObservers(object);
                    } else {
                        logger.debug("Received unknown payload");
                    }
                } catch (IOException e) {
                    logger.error("Unable to receive Object: {}", e.getMessage());
                    logger.trace(e.getMessage(), e);
                } catch (ClassNotFoundException e) {
                    logger.error("Received unknown payload: {}", e.getMessage());
                    logger.trace(e.getMessage(), e);
                }
            } catch (IOException e) {
                logger.error("Unable to create a server socket: {}", e.getMessage());
                logger.trace(e.getMessage(), e);
            }
        }
    }
}
