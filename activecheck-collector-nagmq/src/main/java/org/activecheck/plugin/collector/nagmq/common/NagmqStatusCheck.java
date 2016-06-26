package org.activecheck.plugin.collector.nagmq.common;

public class NagmqStatusCheck {
	private final String type;
	private final String host_name;
	private final String service_description;
	private final String output;
	private NagmqTimeval start_time;
	private NagmqTimeval finish_time;
	private int early_timeout = 0;
	private int latency = 0;
	private int return_code = 0;
	private int exited_ok = 1;
	private int check_type = 1;
	private String check_options = null;
	private int scheduled_check = 0;
	private int reschedule_check = 0;

	public NagmqStatusCheck(String host_name, String service_description,
		String output, int return_code, long startTime, long finshTime) {
		this.type = "service_check_processed";
		this.host_name = host_name;
		this.service_description = service_description;
		this.output = output;
		this.return_code = return_code;

		start_time = new NagmqTimeval(startTime);
		finish_time = new NagmqTimeval(finshTime);
	}

	public String getHost_name() {
		return host_name;
	}

	public String getService_description() {
		return service_description;
	}

	public String getOutput() {
		return output;
	}

	public int getReturn_code() {
		return return_code;
	}

	public NagmqTimeval getStart_time() {
		return start_time;
	}

	public void setStart_time(NagmqTimeval start_time) {
		this.start_time = start_time;
	}

	public NagmqTimeval getFinish_time() {
		return finish_time;
	}

	public void setFinish_time(NagmqTimeval finish_time) {
		this.finish_time = finish_time;
	}

	public int getEarly_timeout() {
		return early_timeout;
	}

	public void setEarly_timeout(int early_timeout) {
		this.early_timeout = early_timeout;
	}

	public int getLatency() {
		return latency;
	}

	public void setLatency(int latency) {
		this.latency = latency;
	}

	public int getExited_ok() {
		return exited_ok;
	}

	public void setExited_ok(int exited_ok) {
		this.exited_ok = exited_ok;
	}

	public int getCheck_type() {
		return check_type;
	}

	public void setCheck_type(int check_type) {
		this.check_type = check_type;
	}

	public String getCheck_options() {
		return check_options;
	}

	public void setCheck_options(String check_options) {
		this.check_options = check_options;
	}

	public int getScheduled_check() {
		return scheduled_check;
	}

	public void setScheduled_check(int scheduled_check) {
		this.scheduled_check = scheduled_check;
	}

	public int getReschedule_check() {
		return reschedule_check;
	}

	public void setReschedule_check(int reschedule_check) {
		this.reschedule_check = reschedule_check;
	}

	public String getType() {
		return type;
	}
}
