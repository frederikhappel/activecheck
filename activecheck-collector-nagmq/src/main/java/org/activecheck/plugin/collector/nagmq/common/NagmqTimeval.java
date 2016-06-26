package org.activecheck.plugin.collector.nagmq.common;

public class NagmqTimeval {
	private long tv_sec = 0;
	private long tv_usec = 0;

	public NagmqTimeval() {
		long millis = System.currentTimeMillis();
		tv_sec = millis / 1000;
		tv_usec = millis * 1000;
	}

	public NagmqTimeval(long millis) {
		tv_sec = millis / 1000;
		tv_usec = millis * 1000;
	}

	public NagmqTimeval(long tv_sec, long tv_usec) {
		this.tv_sec = tv_sec;
		this.tv_usec = tv_usec;
	}

	public long getTv_sec() {
		return tv_sec;
	}

	public void setTv_sec(long tv_sec) {
		this.tv_sec = tv_sec;
	}

	public long getTv_usec() {
		return tv_usec;
	}

	public void setTv_usec(long tv_usec) {
		this.tv_usec = tv_usec;
	}
}
