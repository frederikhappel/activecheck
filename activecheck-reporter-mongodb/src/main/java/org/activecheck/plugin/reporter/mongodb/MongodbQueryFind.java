package org.activecheck.plugin.reporter.mongodb;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;

import org.activecheck.common.nagios.NagiosServiceStatus;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.bson.Document;

import com.mongodb.Block;
import com.mongodb.DBObject;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;
import com.mongodb.async.client.MongoIterable;
import com.mongodb.util.JSON;

public class MongodbQueryFind extends MongodbQuery {
	private final Document queryWhere;
	private final Document queryFields;
	private boolean emptyResult = true;

	@SuppressWarnings("unchecked")
	public MongodbQueryFind(String queryWhere, String queryFields,
			String warning, String critical) {
		super(warning, critical);

		Object parsed = JSON.parse(queryWhere);
		if (parsed instanceof DBObject) {
			this.queryWhere = new Document(((DBObject) parsed).toMap());
		} else {
			this.queryWhere = new Document();
		}

		parsed = JSON.parse(queryFields);
		if (parsed instanceof DBObject) {
			this.queryFields = new Document(((DBObject) parsed).toMap());
		} else {
			this.queryFields = new Document();
		}
	}

	@Override
	public void execute(MongoDatabase db, String collection,
			final CountDownLatch latch) {
		Validate.notNull(collection);
		final List<String> nameArray = new ArrayList<String>();

		nameArray.add(db.getName());
		nameArray.add(collection);

		MongoCollection<Document> mongoCollection = db
				.getCollection(collection);
		MongoIterable<Document> documentList = mongoCollection.find(queryWhere)
				.projection(queryFields);

		emptyResult = true;
		documentList.forEach(new Block<Document>() {
			@Override
			public void apply(Document document) {
				for (Entry<String, Object> entry : document.entrySet()) {
					String name = StringUtils.join(nameArray, "_") + "_"
							+ entry.getKey();
					Object value = entry.getValue();

					// set status message and perfdata
					checkResult.setStatusMoreSevere(compare(value));
					checkResult.addMessage(name + "=" + value);
					addPerformanceData(name, value);
					emptyResult = false;
				}
			}
		}, new SingleResultCallback<Void>() {
			@Override
			public void onResult(Void arg0, Throwable t) {
				if (t != null) {
					checkResult
							.setStatusMoreSevere(NagiosServiceStatus.WARNING);
					checkResult.addMessage(t.getMessage());
				} else if (emptyResult) {
					checkResult
							.setStatusMoreSevere(NagiosServiceStatus.WARNING);
					checkResult.addMessage(String.format(
							"%s.find(%s) did not return a result",
							StringUtils.join(nameArray, "."), getQuery()));
				}
				latch.countDown();
			}
		});
	}

	@Override
	public String getQuery() {
		return queryWhere.toJson() + "," + queryFields.toJson();
	}
}
