/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.IndexOptions;
import live.page.web.utils.Fx;
import live.page.web.utils.Settings;
import live.page.web.utils.json.Json;
import org.bson.conversions.Bson;

import java.util.*;
import java.util.concurrent.TimeUnit;


public class IndexBuilder {

	private static IndexesStore indexes = baseIndexes();

	private static IndexesStore baseIndexes() {

		String def_lang = Settings.getLangs().get(0);

		IndexesStore indexes = new IndexesStore();
		indexes.addIndex("Ratings",
				IndexData.get(new Json("src", 1), "src"),
				IndexData.get(new Json("src", 1).put("ip", 1), "src_ip"),
				IndexData.get(new Json("date", -1), "date")
		);


		indexes.addIndex("SysLog",
				IndexData.get(new Json("date", -1), "date")
		);

		indexes.addIndex("Consents",
				IndexData.get(new Json("uid", 1), "uid"),
				IndexData.get(new Json("consent", 1).put("type", 1), "consent"),
				IndexData.get(new Json("date", 1), "expire", 390L, TimeUnit.DAYS)
		);

		indexes.addIndex("Sessions",
				IndexData.get(new Json("expire", -1), "expire", Settings.COOKIE_DELAY.longValue(), TimeUnit.SECONDS)
		);

		indexes.addIndex("Notices",
				IndexData.get(new Json("config", 1).put("delay", 1).put("received", 1).put("read", 1).put("date", 1), "control"),
				IndexData.get(new Json("user", 1), "user"),
				IndexData.get(new Json("date", -1), "date"),
				IndexData.get(new Json("user", 1).put("read", 1), "user_read"),
				IndexData.get(new Json("grouper", 1), "grouper"),
				IndexData.get(new Json("read", 1), "expire_read", 15L, TimeUnit.DAYS),
				IndexData.get(new Json("date", 1), "expire", 90L, TimeUnit.DAYS),
				IndexData.get(new Json("received", 1), "received")
		);
		indexes.addIndex("Follows",
				IndexData.get(new Json("lng", 1), "lng"),
				IndexData.get(new Json("user", 1), "user"),
				IndexData.get(new Json("objuser", 1), "obj@user"),
				IndexData.get(new Json("obj", 1).put("user", -1), "obj_user"),
				IndexData.get(new Json("obj", 1).put("config", 1), "obj_config")
		);
		indexes.addIndex("Reports",
				IndexData.get(new Json("processed", 1), "processed"),
				IndexData.get(new Json("date", -1), "date")
		);
		indexes.addIndex("Teams",
				IndexData.get(new Json("visible", 1), "visible"),
				IndexData.get(new Json("date", -1), "date")
		);


		indexes.addIndex("Users",
				IndexData.getUnique(new Json("name", 1), "name"),
				IndexData.get(new Json("join", -1), "join"),
				IndexData.get(new Json("coins", -1), "coins"),
				IndexData.get(new Json("providers.id", -1).put("providers.provider", 1), "provider"),
				IndexData.get(new Json("email", 1), "email"),
				IndexData.get(new Json("teams", 1), "teams"),
				IndexData.get(new Json("login", 1), "login"),
				IndexData.get(new Json("activate", 1), "activate"),
				IndexData.get(new Json("parent", 1), "parent"),
				IndexData.get(new Json("key", 1), "key"),
				IndexData.get(new Json("cash.EUR", 1), "cash_EUR"),
				IndexData.get(new Json("cash.USD", 1), "cash_USD")
		);


		indexes.addIndex("Forums",
				IndexData.getUnique(new Json("url.0", 1).put("lng", 1), "url"),
				IndexData.get(new Json("url", 1), "urls"),
				IndexData.get(new Json("parents", 1), "parents"),
				IndexData.get(new Json("title", 1), "title"),
				IndexData.get(new Json("teams", 1), "teams"),
				IndexData.get(new Json("parent", 1), "parent"),
				IndexData.get(new Json("lng", 1), "lng")
		);


		indexes.addIndex("Pages",
				IndexData.getText(new Json("text", 1).put("title", 8).put("intro", 4), "search", def_lang),
				IndexData.get(new Json("parents", 1), "parents"),
				IndexData.getUnique(new Json("url", 1).put("lng", 1), "url"),
				IndexData.get(new Json("update", -1).put("_id", 1), "update"),
				IndexData.get(new Json("questions", 1), "questions"),
				IndexData.get(new Json("title", 1), "title"),
				IndexData.get(new Json("lng", 1), "lng")
		);

		indexes.addIndex("Revisions",
				IndexData.get(new Json("url", 1), "url"),
				IndexData.get(new Json("remove", 1), "remove"),
				IndexData.get(new Json("origine", 1).put("edit", -1), "origine")
		);


		indexes.addIndex("Posts",
				IndexData.get(new Json("parents", 1).put("remove", 1).put("date", 1).put("_id", -1), "parents_date"),
				IndexData.get(new Json("parents", 1).put("remove", 1).put("date", -1).put("_id", 1), "-parents_date"),
				IndexData.get(new Json("parents", 1).put("last.date", 1).put("_id", -1), "last_parents"),
				IndexData.get(new Json("parents", 1).put("last.date", -1).put("_id", 1), "-last_parents"),

				IndexData.get(new Json("user", 1), "user"),

				IndexData.get(new Json("remove", 1), "remove"),
				IndexData.get(new Json("parents", 1), "parents"),

				IndexData.get(new Json("parents.0", 1), "parents_0"),

				IndexData.get(new Json("sysid", 1), "sysid"),

				IndexData.get(new Json("update", 1), "update"),
				IndexData.get(new Json("update", -1), "-update"),

				IndexData.get(new Json("replies", 1), "replies"),
				IndexData.get(new Json("index", 1), "index"),
				IndexData.get(new Json("last.date", 1).put("_id", -1), "last"),
				IndexData.get(new Json("last.date", -1).put("_id", 1), "-last"),
				IndexData.get(new Json("date", 1).put("_id", -1), "date"),
				IndexData.get(new Json("date", -1).put("_id", 1), "-date"),
				IndexData.get(new Json("lng", 1), "lng"),
				IndexData.get(new Json("link.url", 1), "link_url"),

				IndexData.getText(new Json("text", 2).put("title", 3).put("link.title", 1).put("link.description", 1), "search", def_lang)
		);

		indexes.addIndex("BlobFiles",
				IndexData.get(new Json("date", -1), "date"),
				IndexData.get(new Json("user", 1), "user"),
				IndexData.get(new Json("type", 1), "type")
		);

		indexes.addIndex("BlobChunks",
				IndexData.get(new Json("f", 1).put("o", 1), "file")
		);

		indexes.addIndex("BlobCache",
				IndexData.get(new Json("date", -1), "expire", 10L * 24 * 3600, TimeUnit.SECONDS),
				IndexData.get(new Json("blob", 1).put("width", 1).put("height", 1).put("format", 1), "blob")
		);
		indexes.addIndex("Logs",
				IndexData.get(new Json("d", -1), "expire", 90L, TimeUnit.DAYS)
		);


		indexes.addIndex("Relations",
				IndexData.get(new Json("users", 1), "users"),
				IndexData.get(new Json("relations", 1), "relations")
		);

		indexes.addIndex("Messages",
				IndexData.getText(new Json("subject", 3).put("messages.text", 1), "search", def_lang),
				IndexData.get(new Json("recipients", 1), "recipients"),
				IndexData.get(new Json("messages.unread", 1), "unread"),
				IndexData.get(new Json("date", -1), "date"),
				IndexData.get(new Json("last", -1), "last"),
				IndexData.get(new Json("user", 1), "user")
		);

		indexes.addIndex("Countries",
				IndexData.get(new Json("name", 1), "name")
		);


		indexes.addIndex("Stats",
				IndexData.get(new Json("date", 1), "date"),
				IndexData.get(new Json("ip", 1).put("ua", 1), "ipua")
		);


		indexes.addIndex("Verify",
				IndexData.get(new Json("date", 1), "date"),
				IndexData.get(new Json("type", 1), "type")
		);


		indexes.addIndex("ApiApps",
				IndexData.get(new Json("user", 1), "user"),
				IndexData.get(new Json("client_id", 1), "client_id"),
				IndexData.get(new Json("client_secret", 1), "client_secret"),
				IndexData.get(new Json("redirect_uri", 1), "redirect_uri")
		);
		indexes.addIndex("ApiAccess",
				IndexData.get(new Json("user", 1), "user"),
				IndexData.get(new Json("client_id", 1), "client_id"),
				IndexData.get(new Json("access_token", 1), "access_token"),
				IndexData.get(new Json("refresh_token", 1), "refresh_token"),
				IndexData.get(new Json("scopes", 1), "scopes"),
				IndexData.get(new Json("expire", -1), "expire"),
				IndexData.get(new Json("date", -1), "date")
		);

		indexes.addIndex("Scraps",
				IndexData.get(new Json("lng", 1), "lng"),
				IndexData.get(new Json("last", 1), "last"),
				IndexData.get(new Json("date", -1), "date")
		);
		indexes.addIndex("DejaVu",
				IndexData.get(new Json("date", 1), "date", 5, TimeUnit.DAYS)
		);

		return indexes;

	}

	public static Bson getHint(String collection, String name) {
		return indexes.getIndex(collection, name).getKeys();
	}


	public static void addIndex(String collection, IndexModel... index) {
		indexes.addIndex(collection, index);
	}

	public static void buildIndexes() {
		buildIndexes(indexes);
	}

	private static void buildIndexes(IndexesStore indexes_store) {
		try {

			List<String> index_collections = indexes_store.getCollections();
			List<String> db_collections = Db.getDb().listCollectionNames().into(new ArrayList<>());
			List<String> collectionsIndexesNames = indexes_store.getCollectionsIndexesNames();

			for (String index_collection : index_collections) {
				if (!db_collections.contains(index_collection)) {
					Db.getDb().createCollection(index_collection);
					Fx.log("Create collection " + index_collection);
				}

				MongoCollection<Json> col = Db.getDb(index_collection);
				for (Json colindex : col.listIndexes(Json.class).into(new ArrayList<>())) {
					String index_name = colindex.getString("name", "");
					String index_collection_name = index_collection + "@" + colindex.getString("name", "");
					if (!index_name.equals("_id_") && !collectionsIndexesNames.contains(index_collection_name)) {
						Fx.log("drop index " + index_collection_name);
						col.dropIndex(index_name);
					}
				}
			}

			for (String index_collection : index_collections) {
				List<String> indo = new ArrayList<>();
				MongoCollection<Json> col = Db.getDb(index_collection);

				for (Json colindex : col.listIndexes(Json.class).into(new ArrayList<>())) {
					String index_name = colindex.getString("name", "");
					indo.add(index_name);

					if (!index_name.equals("_id_")) {

						if (!indexes_store.contains(index_collection, index_name)) {
							Fx.log("drop index " + index_collection + "@" + index_name);
							col.dropIndex(index_name);
						} else {
							IndexModel index = indexes_store.getIndex(index_collection, index_name);

							if (!compare(colindex.getJson("key"), index.getKeys(), colindex.getJson("weights"), colindex.getString("default_language", ""), index.getOptions()) ||
									colindex.getBoolean("unique", false) != index.getOptions().isUnique()) {
								col.dropIndex(index_name);
								Fx.log("create index " + index_collection + "@" + index_name);
								try {
									col.createIndex(index.getKeys(), index.getOptions());
								} catch (Exception e) {
									Fx.log("Error on create index " + index_collection + "@" + index_name);
									Fx.log(e.getMessage());
								}

							}
						}
					}
				}

				for (IndexModel index : indexes_store.getIndexes(index_collection)) {
					if (!indo.contains(index.getOptions().getName())) {
						Fx.log("create index " + index_collection + "@" + index.getOptions().getName());
						try {
							col.createIndex(index.getKeys(), index.getOptions());
						} catch (Exception e) {
							Fx.log("Error on create index " + index_collection + "@" + index.getOptions().getName());
							Fx.log(e.getMessage());
						}
					}
				}
			}
			Fx.log("indexes controls done");
		} catch (Exception e) {
			Fx.log("indexes controls error");
			e.printStackTrace();
		}
	}

	public static void seed() {

		List<String> collections = Db.getDb().listCollectionNames().into(new ArrayList<>());
		if (!collections.contains("Teams")) {
			Db.save("Teams", new Json("name", "ADMIN").put("color", "red").put("visible", false).put("admin", true));
			Db.save("Teams", new Json("name", "EDITOR").put("color", "blue").put("visible", true).put("editor", true));
		}


	}

	private static boolean compare(Json orig, Bson indb, Json weights, String default_language, IndexOptions options) {
		if (weights != null) {
			if (options.getDefaultLanguage() != null && !options.getDefaultLanguage().equals(default_language)) {
				return false;
			}
			for (Map.Entry<String, Object> entry : new Json(options.getWeights()).entrySet()) {
				if (((int) entry.getValue()) != weights.getInteger(entry.getKey(), -1)) {
					return false;
				}
			}
			return true;
		}
		Json indb_ = new Json(indb);
		if (indb_.size() != orig.size()) {
			return false;
		}
		for (Map.Entry<String, Object> entry : indb_.entrySet()) {
			if (!entry.getValue().equals(orig.get(entry.getKey()))) {
				return false;
			}
		}
		return true;
	}

	public static class IndexesStore {
		private List<String> colindex = new ArrayList<>();
		private Map<String, List<IndexModel>> collection_indexes = new HashMap<>();


		public IndexesStore() {

		}

		public IndexesStore addIndex(String collection, IndexModel index) {
			String name = index.getOptions().getName();
			if (contains(collection, name)) {
				Fx.log("Duplicate indexes " + collection + "@" + name);
			}
			List<IndexModel> current_collection_indexes = (collection_indexes.containsKey(collection)) ? collection_indexes.get(collection) : new ArrayList<>();
			current_collection_indexes.add(index);
			collection_indexes.put(collection, current_collection_indexes);
			colindex.add(collection + "@" + name);
			return this;
		}

		public IndexesStore addIndex(String collection, IndexModel... indexes) {
			return addIndex(collection, Arrays.asList(indexes));
		}

		public IndexesStore addIndex(String collection, List<IndexModel> indexes) {
			for (IndexModel index : indexes) {
				addIndex(collection, index);
			}
			return this;

		}

		public boolean contains(String collection, String name) {
			return colindex.contains(collection + "@" + name);
		}

		public List<IndexModel> getIndexes(String collection) {
			if (!collection_indexes.containsKey(collection)) {
				return new ArrayList<>();
			}
			return collection_indexes.get(collection);
		}

		public IndexModel getIndex(String collection, String name) {
			for (IndexModel index : collection_indexes.get(collection)) {
				if (name.equals(index.getOptions().getName())) {
					return index;
				}
			}
			return null;
		}

		public List<String> getCollections() {
			return Arrays.asList((String[]) collection_indexes.keySet().toArray(new String[0]));
		}

		public List<String> getCollectionsIndexesNames() {
			return colindex;
		}
	}

	public static class IndexData {

		public static IndexModel get(Json key, String name) {
			return new IndexModel(key, new IndexOptions().name(name));
		}

		public static IndexModel getUnique(Json key, String name) {
			return new IndexModel(key, new IndexOptions().name(name).unique(true));
		}

		public static IndexModel get(Json key, String name, long delay, TimeUnit unit) {
			return new IndexModel(key, new IndexOptions().name(name).expireAfter(delay, unit));
		}

		public static IndexModel getText(Json weights, String name) {
			return getText(new Json(), weights, name, null);
		}

		public static IndexModel getText(Json weights, String name, String lang) {
			return getText(new Json(), weights, name, lang);
		}

		public static IndexModel getText(Json base, Json weights, String name, String lang) {

			for (String key : weights.keySet()) {
				base.put(key, "text");
			}
			IndexOptions options = new IndexOptions().name(name).weights(weights);
			if (lang != null) {
				options.defaultLanguage(lang);
			}
			return new IndexModel(base, options);
		}
	}


}
