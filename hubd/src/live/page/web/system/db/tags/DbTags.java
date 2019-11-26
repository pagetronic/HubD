/*
 * Copyright (c) 2019. PAGE and Sons
 */
package live.page.web.system.db.tags;

import live.page.web.system.Settings;

/**
 * Simple parser for DbTags
 */
public class DbTags {
	private String parent;
	private String collection;
	private String _id;

	public DbTags(String collection, String _id) {
		this.collection = collection;
		this._id = _id;

	}

	public DbTags(String parent) {
		this.parent = parent;
		if (parent != null) {
			try {
				String[] parsed = parent.split("([()])");
				if (parsed.length == 2) {
					if (Settings.VALID_PARENTS.contains(parsed[0])) {
						collection = parsed[0];
						_id = parsed[1];
						return;
					}
				}
				throw new Exception("Invalid parent spec -> DataBase(id_key)");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public String toString() {
		return parent;
	}

	public String getCollection() {
		return collection;
	}

	public String getId() {
		return _id;
	}
}
