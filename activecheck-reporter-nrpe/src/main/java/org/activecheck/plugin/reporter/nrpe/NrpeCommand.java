package org.activecheck.plugin.reporter.nrpe;

import it.jnrpe.net.JNRPERequest;
import it.jnrpe.net.JNRPEResponse;
import it.jnrpe.net.PacketVersion;

import org.activecheck.common.nagios.NagiosCheck;
import org.activecheck.common.nagios.NagiosServiceStatus;

public class NrpeCommand extends NagiosCheck {
	private final String command;
	private final String arguments;
	private final JNRPERequest request;

	public NrpeCommand(String command, String arguments) {
		this.command = command;
		this.arguments = arguments;
		request = new JNRPERequest(command, arguments);
		request.setPacketVersion(PacketVersion.VERSION_2);

		checkResult.setStatus(NagiosServiceStatus.UNKNOWN);
		checkResult.addPerfDataReplacement("_", ".");
	}

	public JNRPERequest getRequest() {
		return request;
	}

	public void parseResponse(JNRPEResponse response) {
		String message = response.getStringMessage();
		checkResult.parseMessage(message, NagiosServiceStatus
				.statusCodeToStatus(response.getResultCode()));
	}

	@Override
	public String getQuery() {
		return command + " " + arguments;
	}
}
