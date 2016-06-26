package org.activecheck.plugin.reporter.graylog;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.activecheck.plugin.reporter.graylog.api.GraylogApiStreamObject;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.stream.JsonReader;

public class GraylogAlertCollector {
	private static final Logger logger = LoggerFactory
			.getLogger(GraylogAlertCollector.class);
	private PoolingHttpClientConnectionManager connectionManager = null;
	private CredentialsProvider credsProvider = null;

	public GraylogAlertCollector(int maxConcurrentRequests) {
		connectionManager = new PoolingHttpClientConnectionManager();
		connectionManager.setDefaultMaxPerRoute(maxConcurrentRequests);
		connectionManager.setMaxTotal(maxConcurrentRequests);
	}

	public void setCredentials(String username, String password) {
		if (username != null) {
			credsProvider = new BasicCredentialsProvider();
			credsProvider.setCredentials(AuthScope.ANY,
					new UsernamePasswordCredentials(username, password));
		} else {
			credsProvider = null;
		}
	}

	public void cleanUp() {
		connectionManager.closeExpiredConnections();
		connectionManager.shutdown();
		return;
	}

	private final HttpClient getHttpClient() {
		HttpClient httpClient = null;
		if (credsProvider != null) {
			httpClient = HttpClients.custom()
					.setConnectionManager(connectionManager)
					.setDefaultCredentialsProvider(credsProvider).build();
		} else {
			httpClient = HttpClients.custom()
					.setConnectionManager(connectionManager).build();
		}
		return httpClient;
	}

	// request number of alerts for all streams
	public Collection<GraylogApiStreamObject> getStreams(String url,
			long timestamp) throws ClientProtocolException, IOException,
			InterruptedException {
		// retrieve all streams
		Collection<GraylogApiStreamObject> streams = getStreamsList(url);
		List<GraylogAlertRequest> requests = new ArrayList<GraylogAlertRequest>();
		CountDownLatch latch = new CountDownLatch(streams.size());

		// loop through all streams
		for (GraylogApiStreamObject stream : streams) {
			if (!stream.isDisabled()) {
				GraylogAlertRequest request = new GraylogAlertRequest(
						getHttpClient(), url, stream, timestamp, latch);
				request.setName("httpClient for stream: " + stream.getId());
				requests.add(request);
				request.start();
			}
		}
		if (!latch.await(30, TimeUnit.SECONDS)) {
			logger.error(latch.getCount() + " of " + streams.size()
					+ " URLs did not return a result");
		}

		return streams;
	}

	public Collection<GraylogApiStreamObject> getStreamsList(String url)
			throws ClientProtocolException, IOException {
		Collection<GraylogApiStreamObject> streams = new ArrayList<GraylogApiStreamObject>();

		String url_stream_list = url + "/streams";
		HttpGet httpget = new HttpGet(url_stream_list);
		HttpClient httpClient = getHttpClient();

		// actually execute the request
		logger.debug("executing request " + httpget.getRequestLine());
		HttpResponse response = httpClient.execute(httpget);
		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode == 200) {
			JsonReader reader = new JsonReader(new InputStreamReader(response
					.getEntity().getContent(), "UTF-8"));
			reader.beginObject();
			while (reader.hasNext()) {
				String name = reader.nextName();
				if (name.equals("streams")) {
					reader.beginArray();
					while (reader.hasNext()) {
						reader.beginObject();
						GraylogApiStreamObject stream = new GraylogApiStreamObject();
						try {
							stream.fromReader(reader);
							streams.add(stream);
						} catch (IOException e) {
							logger.error("failed to parse stream list"
									+ e.getMessage());
							logger.debug(e.getMessage(), e);
						}
						reader.endObject();
					}
					reader.endArray();
				} else {
					reader.skipValue();
				}
			}
			reader.endObject();
			reader.close();
		} else {
			throw new HttpResponseException(statusCode,
					"unexpected HTTP status code (" + statusCode
							+ ") for request: " + httpget.getRequestLine());
		}

		return streams;
	}
}
