/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.IndexOptions;
import live.page.hubd.system.Settings;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.utils.Fx;
import org.bson.conversions.Bson;

import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 * Class used to build on update indexes
 */
public class IndexBuilder {

    public static final IndexesStore indexes = baseIndexes();


    private static IndexesStore baseIndexes() {

        String def_lang = Settings.getLangs().get(0);

        IndexesStore indexes = new IndexesStore();


        indexes.addIndex("Likes",
                IndexData.getUnique(new Json("user", 1).put("parent", 1), "likes")
        );

        indexes.addIndex("Consents",
                IndexData.get(new Json("uid", 1), "uid"),
                IndexData.get(new Json("consent", 1).put("type", 1), "consent"),
                IndexData.getExpire(new Json("date", 1), "expire", 390L, TimeUnit.DAYS)
        );

        indexes.addIndex("Sessions",
                IndexData.getExpire(new Json("expire", -1), "expire", Settings.COOKIE_DELAY, TimeUnit.SECONDS),
                IndexData.get(new Json("code", 1), "code")
        );

        indexes.addIndex("Notices",
                IndexData.getExpire(new Json("date", -1), "expire", 90, TimeUnit.DAYS),
                IndexData.get(new Json("user", 1).put("type", 1).put("read", 1).put("received", 1).put("date", -1), "user")
        );

        indexes.addIndex("Subscriptions",
                IndexData.getUnique(new Json("channel", 1).put("user", 1).put("type", 1), "channel"),
                IndexData.get(new Json("date", -1), "date")
        );


        indexes.addIndex("Users",
                IndexData.getUnique(new Json("name", 1), "name"),
                IndexData.get(new Json("join", -1), "join"),
                IndexData.get(new Json("coins", -1), "coins"),
                IndexData.get(new Json("providers.id", -1).put("providers.provider", 1), "provider"),
                IndexData.get(new Json("email", 1), "email"),
                IndexData.get(new Json("groups", 1), "groups"),
                IndexData.get(new Json("login", 1), "login"),
                IndexData.get(new Json("activate", 1), "activate"),
                IndexData.get(new Json("parent", 1), "parent")
        );

        indexes.addIndex("Devices",
                IndexData.get(new Json("user", 1).put("device", "hashed").put("date", -1), "select")
        );


        indexes.addIndex("Groups",
                IndexData.get(new Json("date", -1), "date")
        );

        indexes.addIndex("Pages",
                IndexData.getText(new Json("text", 1).put("title", 8).put("intro", 4), "search", def_lang),
                IndexData.get(new Json("parents", 1), "parents"),
                IndexData.get(new Json("url", 1), "url"),
                IndexData.get(new Json("update", -1).put("_id", 1), "update"),
                IndexData.get(new Json("title", 1), "title"),
                IndexData.get(new Json("lng", 1), "lng")
        );


        indexes.addIndex("Posts",
                IndexData.get(new Json("parents.id", 1).put("parents.type", 1), "parents"),
                IndexData.get(new Json("update", -1), "-update"),
                IndexData.get(new Json("replies", 1), "replies"),
                IndexData.getText(new Json("text", 2).put("title", 3).put("link.title", 1).put("link.description", 1), "search", def_lang)
        );

        indexes.addIndex("BlobFiles",
                IndexData.get(new Json("date", -1), "date"),
                IndexData.get(new Json("parents", 1), "parents"),
                IndexData.get(new Json("type", 1), "type"),
                IndexData.get(new Json().put("user", 1).put("users", 1).put("shares", 1), "user")
        );

        indexes.addIndex("BlobChunks",
                IndexData.get(new Json("f", 1).put("o", 1), "file")
        );

        indexes.addIndex("BlobCache",
                IndexData.getExpire(new Json("date", -1), "expire_delete", 10L * 24 * 3600, TimeUnit.SECONDS),
                IndexData.get(new Json("blob", 1).put("width", 1).put("height", 1).put("format", 1), "blob")
        );


        indexes.addIndex("Stats",
                IndexData.get(new Json("ip", 1).put("platform", 1), "ip_platform"),
                IndexData.get(new Json("ip", 1).put("lng", 1).put("url", 1), "ip_url"),
                // RGPD - GDPR friendly, remove stats after one year
                IndexData.getExpire(new Json("date", 1), "date", 365, TimeUnit.DAYS)
        );


        indexes.addIndex("Logs",
                IndexData.getExpire(new Json("d", -1), "expire", 366, TimeUnit.DAYS)
        );


        indexes.addIndex("ApiApps",
                IndexData.get(new Json("user", 1), "user"),
                IndexData.get(new Json("client_id", 1), "client_id"),
                IndexData.get(new Json("client_secret", 1), "client_secret"),
                IndexData.get(new Json("redirect_uri", 1), "redirect_uri")
        );

        indexes.addIndex("ApiAccess",
                IndexData.get(new Json("user", 1), "user"),
                IndexData.get(new Json("access_token", 1), "access_token"),
                IndexData.get(new Json("refresh_token", 1), "refresh_token"),
                IndexData.get(new Json("code", 1), "code"),
                IndexData.get(new Json("app_id", 1), "app_id"),
                IndexData.get(new Json("scopes", 1), "scopes"),
                IndexData.get(new Json("expire", -1), "expire"),
                IndexData.get(new Json("date", -1), "date"),
                IndexData.getExpire(new Json("perishable", 1), "perishable", 1, TimeUnit.DAYS)
        );


        return indexes;

    }

    /**
     * Get hint aka information for using a specific index in a query
     *
     * @param collection collection where is the index
     * @param name       of the index
     * @return object to use as a .hint() in a DB query
     */
    public static Bson getHint(String collection, String name) {
        return indexes.getIndex(collection, name).getKeys();
    }


    /**
     * Build standard indexes
     */
    public static void buildIndexes() {

        try {

            List<String> index_collections = indexes.getCollections();
            List<String> db_collections = Db.getDb().listCollectionNames().into(new ArrayList<>());
            List<String> collectionsIndexesNames = indexes.getCollectionsIndexesNames();

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

                        if (!indexes.contains(index_collection, index_name)) {
                            Fx.log("drop index " + index_collection + "@" + index_name);
                            col.dropIndex(index_name);
                        } else {
                            IndexModel index = indexes.getIndex(index_collection, index_name);
                            IndexOptions options = index.getOptions();

                            if (
                                    !compare(colindex.getJson("key"), index.getKeys(), colindex.getJson("weights"), colindex.getString("default_language", ""), index.getOptions()) ||
                                            colindex.getBoolean("unique", false) != options.isUnique() ||
                                            !compareExpire(options.getExpireAfter(TimeUnit.SECONDS), (long) colindex.getDouble("expireAfterSeconds", -1)) ||
                                            !compareRestrict(colindex.getJson("partialFilterExpression"), options.getPartialFilterExpression())) {
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

                for (IndexModel index : indexes.getIndexes(index_collection)) {
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


    /**
     * Seeding special think in a Collection
     */
    public static void seed() {

        List<String> collections = Db.getDb().listCollectionNames().into(new ArrayList<>());
        if (!collections.contains("Groups")) {
            Db.save("Groups", new Json("name", "ADMIN").put("color", "red").put("visible", false).put("admin", true));
            Db.save("Groups", new Json("name", "EDITOR").put("color", "blue").put("visible", true).put("editor", true));
        }


    }

    /**
     * Compare indexes changes
     *
     * @param original         index to compare
     * @param other            index to compare
     * @param weights          search index weight
     * @param default_language of the search index
     * @param options          of the index
     * @return true|false index are same ?
     */
    private static boolean compare(Json original, Bson other, Json weights, String default_language, IndexOptions options) {
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
        Json indb_ = new Json(other);
        if (indb_.size() != original.size()) {
            return false;
        }
        for (Map.Entry<String, Object> entry : indb_.entrySet()) {
            if (!entry.getValue().equals(original.get(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compare expiration delay
     *
     * @param one   to compare
     * @param other to compare
     * @return true or false if not the same value
     */
    private static boolean compareExpire(Long one, Long other) {
        if (one == null) {
            one = -1L;
        }
        if (other == null) {
            other = -1L;
        }
        return one.equals(other);
    }

    /**
     * Compare partialFilterExpression
     *
     * @param one   to compare
     * @param other to compare
     * @return true or false if not the same value
     */
    private static boolean compareRestrict(Json one, Bson other) {
        if (one == null && other == null) {
            return true;
        }
        if (one != null && other == null) {
            return false;
        }
        if (one == null) {
            return false;
        }
        return Objects.equals(one.toString(true), new Json(other.toBsonDocument().toJson()).sort().toString(true));
    }

    /**
     * Store list of index
     */
    public static class IndexesStore {
        private final List<String> colindex = new ArrayList<>();
        private final Map<String, List<IndexModel>> collection_indexes = new HashMap<>();


        public IndexesStore() {

        }

        /**
         * @param collection where use the index
         * @param index      to use in the collection
         * @return this IndexesStore
         */
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

        /**
         * @param collection where use the index
         * @param indexes    to use in the collection
         * @return this IndexesStore
         */
        public IndexesStore addIndex(String collection, IndexModel... indexes) {
            return addIndex(collection, Arrays.asList(indexes));
        }


        /**
         * @param collection where use the index
         * @param indexes    to use in the collection
         * @return this IndexesStore
         */
        public IndexesStore addIndex(String collection, List<IndexModel> indexes) {
            for (IndexModel index : indexes) {
                addIndex(collection, index);
            }
            return this;

        }

        /**
         * Is there an index named in the collection ?
         *
         * @param collection where search the index
         * @param name       of the index
         * @return true|false contains the index ?
         */
        public boolean contains(String collection, String name) {
            return colindex.contains(collection + "@" + name);
        }

        /**
         * Get the indexes in this store for the collection
         *
         * @param collection where search the indexes
         * @return List of indexes
         */
        public List<IndexModel> getIndexes(String collection) {
            if (!collection_indexes.containsKey(collection)) {
                return new ArrayList<>();
            }
            return collection_indexes.get(collection);
        }

        /**
         * Get the index in this store for the name and the collection
         *
         * @param collection where search the indexes
         * @param name       of the index needed
         * @return the index or null if there no index
         */
        public IndexModel getIndex(String collection, String name) {
            for (IndexModel index : collection_indexes.get(collection)) {
                if (name.equals(index.getOptions().getName())) {
                    return index;
                }
            }
            return null;
        }

        /**
         * Get the collection in this store
         *
         * @return a list of the collections
         */
        public List<String> getCollections() {
            return Arrays.asList(collection_indexes.keySet().toArray(new String[0]));
        }

        /**
         * Get collection@name in this store
         *
         * @return a list of collection@name
         */
        public List<String> getCollectionsIndexesNames() {
            return colindex;
        }
    }

    /**
     * Class to simplify index model creation
     */
    public static class IndexData {

        /**
         * Get simple index
         *
         * @param keys to use in index
         * @param name of the index needed
         * @return Index model accepted for creation
         */
        public static IndexModel get(Json keys, String name) {
            return new IndexModel(keys, new IndexOptions().name(name));
        }

        /**
         * Get simple index
         *
         * @param keys    to use in index
         * @param partial restrict index to filter
         * @param name    of the index needed
         * @return Index model accepted for creation
         */
        public static IndexModel get(Json keys, Bson partial, String name) {
            return new IndexModel(keys, new IndexOptions().name(name).partialFilterExpression(partial));
        }

        /**
         * Get unique index
         *
         * @param keys to use in index
         * @param name of the index needed
         * @return Index model accepted for creation
         */
        public static IndexModel getUnique(Json keys, String name) {
            return new IndexModel(keys, new IndexOptions().name(name).unique(true));
        }

        /**
         * Get an index where objects expires
         *
         * @param keys  to use in index
         * @param name  of the index needed
         * @param delay when db object are removed
         * @param unit  to use in delay
         * @return Index model accepted for creation
         */
        public static IndexModel getExpire(Json keys, String name, long delay, TimeUnit unit) {
            return new IndexModel(keys, new IndexOptions().name(name).expireAfter(delay, unit));
        }

        /**
         * Get a text index for search
         *
         * @param weights of keys
         * @param name    of the index needed
         * @return Index model accepted for creation
         */
        public static IndexModel getText(Json weights, String name) {
            return getText(new Json(), weights, name, null);
        }

        /**
         * Get a text index for search with default language
         *
         * @param weights of keys
         * @param name    of the index needed
         * @param lang    as default language
         * @return Index model accepted for creation
         */
        public static IndexModel getText(Json weights, String name, String lang) {
            return getText(new Json(), weights, name, lang);
        }

        /**
         * Get a text index for search with default language
         *
         * @param base    used as preconfigured
         * @param weights of keys
         * @param name    of the index needed
         * @param lang    as default language
         * @return Index model accepted for creation
         */
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
