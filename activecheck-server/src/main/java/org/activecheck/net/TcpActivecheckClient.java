package org.activecheck.net;

import org.activecheck.common.nagios.NagiosServiceReport;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

@SuppressWarnings("unused")
public class TcpActivecheckClient extends ActivecheckClient {
    public TcpActivecheckClient(String address, int port) {
        super(address, port);
    }

    @Override
    public void send(NagiosServiceReport report) {
        try (final Socket socket = new Socket(address, port)) {
            try {
                final ObjectOutputStream objectOutput = new ObjectOutputStream(
                        socket.getOutputStream());
                objectOutput.writeObject(report);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
