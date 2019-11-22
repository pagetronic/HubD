/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.db;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.BsonField;
import live.page.web.utils.json.Json;

import java.util.ArrayList;
import java.util.List;

public class Aggregator extends ArrayList<BsonField> {

	private final List<String> keys = new ArrayList<>();

	public Aggregator(String... keys_) {
		for (String key : keys_) {
			if (key != null) {
				keys.add(key);
			}
		}
	}

	public List<BsonField> getGrouper(BsonField... groups) {
		List<BsonField> grouper = new ArrayList<>();
		List<String> addkeys = new ArrayList<>();
		for (BsonField group : groups) {
			grouper.add(group);
			addkeys.add(group.getName());
		}
		for (String key : keys) {
			if (!addkeys.contains(key)) {
				grouper.add(Accumulators.first(key, "$" + key));
			}
		}
		return grouper;
	}

	public Json getProjection() {
		Json projection = new Json();
		for (String key : keys) {
			projection.put(key, true);
		}
		return projection;
	}

	public Json getProjectionOrder() {
		Json projection = new Json("_id", "$_id");
		for (String key : keys) {
			projection.put(key, "$" + key);
		}
		return projection;
	}

	@Override
	public Aggregator clone() {
		return new Aggregator(keys.toArray(new String[0]));
	}

	public Aggregator addKey(String key) {
		keys.add(key);
		return this;
	}
}
