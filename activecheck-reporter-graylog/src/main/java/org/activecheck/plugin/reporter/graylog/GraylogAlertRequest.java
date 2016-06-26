package org.activecheck.plugin.reporter.graylog;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CountDownLatch;

import org.activecheck.plugin.reporter.graylog.api.GraylogApiStreamObject;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.stream.JsonReader;

public class GraylogAlertRequest extends Thread {
	private static final Logger logger = LoggerFactory
			.getLogger(GraylogAlertRequest.class);
	private final GraylogApiStreamObject stream;
	private final HttpClient httpClient;
	private final HttpGet httpget;
	private final CountDownLatch latch;

	public GraylogAlertRequest(HttpClient httpClient, String url,
			GraylogApiStreamObject stream, long since, CountDownLatch latch) {
		this.stream = stream;
		this.latch = latch;
		this.httpClient = httpClient;
		String url_stream_alerts = url + "/streams/" + stream.getId()
				+ "/alerts?since=" + since;
		httpget = new HttpGet(url_stream_alerts);
	}

	@Override
	public void run() {
		logger.debug("executing request " + httpget.getRequestLine());
		long numAlerts = 0;
		try {
			HttpResponse response = httpClient.execute(httpget);
			if (response.getStatusLine().getStatusCode() == 200) {
				JsonReader reader = new JsonReader(new InputStreamReader(
						response.getEntity().getContent(), "UTF-8"));

				reader.beginObject();
				while (reader.hasNext()) {
					String name = reader.nextName();
					if (name.equals("alerts")) {
						reader.beginArray();
						while (reader.hasNext()) {
							numAlerts++;
							reader.skipValue();
						}
						reader.endArray();
					} else {
						reader.skipValue();
					}
				}
				reader.endObject();
				reader.close();
				stream.setNumAlerts(numAlerts);
			}
		} catch (IOException e) {
			logger.error("error executing request " + httpget.getRequestLine()
					+ ": " + e.getMessage());
			logger.debug(e.getMessage(), e);
		}
		latch.countDown();
	}
}
