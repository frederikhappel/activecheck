package org.activecheck.plugin.reporter.mongodb;

import java.util.concurrent.CountDownLatch;

import org.activecheck.common.nagios.NagiosCheck;

import com.mongodb.async.client.MongoDatabase;

public abstract class MongodbQuery extends NagiosCheck {
	public MongodbQuery(String warning, String critical) {
		setWarning(warning);
		setCritical(critical);
	}

	public abstract void execute(MongoDatabase db, String collection,
			final CountDownLatch latch);
}
