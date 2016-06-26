package org.activecheck.plugin.collector.graphite;

import com.codahale.metrics.Gauge;

public class NagiosGauge<T> implements Gauge<T> {
	private volatile T value;
	private long lastUpdatedTime = 0;
	private long ttl = 604800; // one week

	public NagiosGauge(final T value) {
		this.value = value;
	}

	public final void setValue(final T value) {
		this.value = value;

		// recalculate time to live
		long currentTime = System.currentTimeMillis();
		ttl = (lastUpdatedTime - currentTime) * 5;
		lastUpdatedTime = currentTime;
	}

	@Override
	public T getValue() {
		return value;
	}

	public final boolean isDead() {
		long currentTime = System.currentTimeMillis();
		if (currentTime - lastUpdatedTime > ttl) {
			return true;
		}
		return false;
	}
}
