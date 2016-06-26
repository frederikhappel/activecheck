package org.activecheck.plugin.reporter.mongodb;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.activecheck.common.nagios.NagiosServiceStatus;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.bson.Document;

import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoDatabase;

public class MongodbQueryStats extends MongodbQuery {
	private final List<String> stats = new ArrayList<String>();

	public MongodbQueryStats(List<Object> stats, String warning, String critical) {
		super(warning, critical);
		Validate.notNull(stats);
		for (Object metric : stats) {
			if (metric instanceof String) {
				this.stats.add((String) metric);
			}
		}
	}

	@Override
	public void execute(MongoDatabase db, String collection,
			final CountDownLatch latch) {
		final List<String> nameArray = new ArrayList<String>();
		nameArray.add(db.getName());

		// define callback for handling the result
		SingleResultCallback<Document> callback = new SingleResultCallback<Document>() {
			@Override
			public void onResult(Document document, Throwable t) {
				if (t != null) {
					checkResult
							.setStatusMoreSevere(NagiosServiceStatus.WARNING);
					checkResult.addMessage(t.getMessage());
				} else if (document == null) {
					checkResult
							.setStatusMoreSevere(NagiosServiceStatus.WARNING);
					checkResult.addMessage("stats did not return a result");
				} else {
					for (String metric : stats) {
						String name = StringUtils.join(nameArray, ".") + "."
								+ metric;
						Object value = document.get(metric);
						if (value != null) {
							// set status message and perfdata
							// TODO: move to compare function?
							checkResult.setStatusMoreSevere(compare(value));
							checkResult.addMessage(name + "=" + value);
							addPerformanceData(name, value);
						} else {
							checkResult
									.setStatusMoreSevere(NagiosServiceStatus.WARNING);
							checkResult.addMessage(name + " does not exist");
						}
					}
				}
				latch.countDown();
			}
		};

		// retrieve stats document
		if (collection == null || collection.isEmpty()) {
			db.runCommand(new Document("dbStats", 1), callback);
		} else {
			nameArray.add(collection);
			db.runCommand(new Document("collStats", collection), callback);
		}
	}

	@Override
	public String getQuery() {
		return StringUtils.join(stats, ",");
	}
}
