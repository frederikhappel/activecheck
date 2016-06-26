package org.activecheck.plugin.reporter.nrpe;

import it.jnrpe.net.JNRPEResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class NrpeCommandInvoker {
	private static final String[] ADH_CIPHER_SUITES = new String[] { "TLS_DH_anon_WITH_AES_128_CBC_SHA" };

	private static final NrpeCommandInvoker instance = new NrpeCommandInvoker();

	private SocketFactory socketFactory;
	private SocketFactory sslSocketFactory;

	/**
	 * Creates a custom TrustManager that trusts any certificate.
	 * 
	 * @return The custom trustmanager
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 */
	private SocketFactory getSocketFactory() throws NoSuchAlgorithmException,
			KeyManagementException {
		if (socketFactory == null) {
			socketFactory = SocketFactory.getDefault();
		}
		return socketFactory;
	}

	private SocketFactory getSslSocketFactory()
			throws NoSuchAlgorithmException, KeyManagementException {
		if (sslSocketFactory == null) {
			SSLContext sslContext = SSLContext.getInstance("SSL");

			// Trust all certificates
			TrustManager[] tm = { new X509TrustManager() {
				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				@Override
				public void checkServerTrusted(final X509Certificate[] chain,
						final String authType) throws CertificateException {

				}

				@Override
				public void checkClientTrusted(final X509Certificate[] chain,
						final String authType) throws CertificateException {
				}
			} };

			sslContext.init(null, tm, new SecureRandom());

			sslSocketFactory = sslContext.getSocketFactory();
		}
		return sslSocketFactory;
	}

	private Socket getSocket(boolean useSsl) throws KeyManagementException,
			NoSuchAlgorithmException, IOException {
		Socket socket = null;
		if (useSsl) {
			socket = getSslSocketFactory().createSocket();
			((SSLSocket) socket).setEnabledCipherSuites(ADH_CIPHER_SUITES);
		} else {
			socket = getSocketFactory().createSocket();
		}
		return socket;
	}

	/**
	 * Inovoke a command installed in JNRPE.
	 * 
	 * @param sCommandName
	 *            The name of the command to be invoked
	 * @param arguments
	 *            The arguments to pass to the command (will substitute the
	 *            $ARGSx$ parameters)
	 * @return The value returned by the server
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 * @throws IOException
	 * @throws JNRPEClientException
	 *             Thrown on any communication error.
	 */
	public static void execute(String host, int port, boolean useSsl,
			int timeout, NrpeCommand nrpeCommand)
			throws KeyManagementException, NoSuchAlgorithmException,
			IOException {
		Socket socket = instance.getSocket(useSsl);
		socket.setSoTimeout(timeout * 1000);
		socket.connect(new InetSocketAddress(host, port));

		// submit query
		socket.getOutputStream().write(nrpeCommand.getRequest().toByteArray());

		// get answer
		InputStream in = socket.getInputStream();
		nrpeCommand.parseResponse(new JNRPEResponse(in));

		// close connection to nrpe
		socket.close();
	}
}
