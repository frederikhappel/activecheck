package org.activecheck.plugin.reporter.graylog.api;

import java.io.IOException;

import com.google.gson.stream.JsonReader;

public class GraylogApiStreamObject {
	private String id = null;
	private String title = null;
	private String description = null;
	private String created = null;
	private String creator = null;
	private boolean disabled = false;
	private long numAlerts = 0;

	// {
	// "id":"5310e73b098e6113440ae744",
	// "title":"BigIP",
	// "description":"BigIP syslog",
	// "created_at":"2014-02-28T19:44:59.350Z",
	// "creator_user_id":"fh",
	// "rules":[],
	// "disabled":false
	// }
	public void fromReader(JsonReader reader) throws IOException {
		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals("id")) {
				id = reader.nextString();
			} else if (name.equals("title")) {
				title = reader.nextString();
			} else if (name.equals("description")) {
				description = reader.nextString();
			} else if (name.equals("created_at")) {
				created = reader.nextString();
			} else if (name.equals("creator_user_id")) {
				creator = reader.nextString();
			} else if (name.equals("disabled")) {
				disabled = reader.nextBoolean();
			} else {
				reader.skipValue();
			}
		}
	}

	public String getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public String getCreated() {
		return created;
	}

	public String getCreator() {
		return creator;
	}

	public boolean isDisabled() {
		return disabled;
	}

	public void setNumAlerts(long numAlerts) {
		this.numAlerts = numAlerts;
	}

	public long getNumAlerts() {
		return numAlerts;
	}
}
