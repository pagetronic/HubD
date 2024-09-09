/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.db;

import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import live.page.hubd.system.Settings;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.json.JsonProvider;
import live.page.hubd.system.utils.Fx;
import org.apache.commons.lang3.RandomStringUtils;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * General class for Db connections
 * used as a static functions provider for comfortable/wellness coding
 */
@WebListener
public class Db implements ServletContextListener {

    // DB key length used somewhere for detection
    public static final int DB_KEY_LENGTH = 26;
    /**
     * Generate support for Json
     */
    public static final CodecRegistry codecRegistry = CodecRegistries.fromRegistries(CodecRegistries.fromProviders(new JsonProvider()), MongoClientSettings.getDefaultCodecRegistry());
    //Global timeout for DB operations
    private static final int TIME_OUT = 10000;
    /**
     * Permanently connect to DB
     */
    private static final MongoClient client = getClient(Settings.DB_USER, Settings.DB_NAME, Settings.DB_PASS);
    /**
     * Open Project Database
     */
    private static final MongoDatabase db = client.getDatabase(Settings.DB_NAME);
    // Be sure Db key are unique, at least for this machine
    private static int keyDiff = 0;

    /**
     * @return an unique key for DB objects
     */
    public static String getKey() {
        //Key difference incremented after
        String base = Long.toString(keyDiff, Character.MAX_RADIX);
        //live.page.hub time increment sys epoc
        base += Long.toString(System.currentTimeMillis(), Character.MAX_RADIX);
        //apache random number for bette unicity
        String key = (RandomStringUtils.randomAlphanumeric(DB_KEY_LENGTH - base.length()) + base).toUpperCase();
        //Key difference is under 36 chrs for one char identification
        keyDiff = (keyDiff + 1 >= Character.MAX_RADIX) ? 0 : keyDiff + 1;
        // perfect key isn't ? No id server for now, perhaps in futures db system
        return key;
    }

    /**
     * Get MongoClient with settings
     *
     * @return MongoClient  with settings
     */
    public static MongoClient getClient(String db_user, String db_name, char[] db_password) {
        try {

            List<ServerAddress> hosts = new ArrayList<>();
            if (Settings.DB_HOSTS == null) {
                hosts.add(new ServerAddress("localhost"));
            } else {
                for (String host : Settings.DB_HOSTS) {
                    hosts.add(new ServerAddress(host));
                }
            }
            MongoClientSettings.Builder clientbuilder = MongoClientSettings.builder()
                    .applicationName(Settings.SITE_TITLE)
                    .retryReads(false).retryWrites(false)
                    .applyToConnectionPoolSettings(builder -> builder.maxSize(100).minSize(0).maxConnectionIdleTime(60000, TimeUnit.MILLISECONDS))
                    .applyToSocketSettings(builder -> builder.connectTimeout(TIME_OUT, TimeUnit.MILLISECONDS))
                    .applyToClusterSettings(builder -> builder.hosts(hosts));
            if (db_user != null) {
                clientbuilder.credential(MongoCredential.createCredential(db_user, db_name, db_password));
            }
            clientbuilder.readConcern(ReadConcern.MAJORITY);
            clientbuilder.writeConcern(WriteConcern.MAJORITY);
            clientbuilder.readPreference(ReadPreference.nearest());
            clientbuilder.codecRegistry(codecRegistry);
            return MongoClients.create(clientbuilder.build());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Return base Db function from MongoDb library
     *
     * @return MongoDatabase for the current project
     */
    public static MongoDatabase getDb() {
        return db;
    }

    /**
     * Return base Db function from MongoDb library
     *
     * @param collection collection to select
     * @return MongoCollection for the current project
     */
    public static MongoCollection<Json> getDb(String collection) {
        return db.getCollection(collection, Json.class);
    }

    /**
     * Return db object for ID and USER
     *
     * @param collection where search
     * @param _id        of the object
     * @param user_id    of the object
     * @return Db object
     */
    public static Json findByIdUser(String collection, String _id, String user_id) {
        Bson filter = Filters.and(Filters.eq("_id", _id), Filters.eq("user", user_id));
        return Db.find(collection, filter).first();
    }

    /**
     * Return db object for ID
     *
     * @param collection where search
     * @param _id        of the object
     * @return Db object
     */
    public static Json findById(String collection, String _id) {
        if (_id == null) {
            return null;
        }
        return find(collection, Filters.and(Filters.eq("_id", _id), Filters.ne("_id", null))).first();
    }

    /**
     * Save one db object
     *
     * @param collection where search
     * @param document   to save
     * @return true if saved
     */
    public static boolean save(String collection, Json document) {
        if (document.get("_id") == null) {
            try {
                document.put("_id", getKey());
                getDb(collection).insertOne(document);
            } catch (Exception e) {
                document.remove("_id");
                return false;
            }
        } else {
            try {
                getDb(collection).replaceOne(Filters.eq("_id", document.get("_id")), document, new ReplaceOptions().upsert(true));
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    /**
     * Save multiple db objects
     *
     * @param collection where search
     * @param documents  to save
     * @return true if saved
     */
    public static boolean save(String collection, List<Json> documents) {
        try {
            // Can't "update" when insertMany used, so no Ids needed.
            List<Json> inserts = new ArrayList<>();
            List<Json> updates = new ArrayList<>();
            for (Json document : documents) {
                if (document.getId() == null) {
                    inserts.add(document.put("_id", Db.getKey()));
                } else {
                    updates.add(document);
                }
            }
            if (!inserts.isEmpty()) {
                getDb(collection).insertMany(documents);
            }
            if (!updates.isEmpty()) {
                for (Json document : updates) {
                    save(collection, document);
                }
            }
            return true;
        } catch (Exception e) {
            if (Fx.IS_DEBUG) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Find multiple db objects
     *
     * @param collection where search
     * @param filter     restrictions
     * @return iterable response
     */
    public static FindIterable<Json> find(String collection, Bson filter) {
        if (filter == null) {
            return getDb(collection).find().limit(1000);
        }
        return getDb(collection).find(filter).limit(1000);
    }

    /**
     * Find multiple db objects
     *
     * @param collection where search
     * @param filter     restrictions
     * @param sort       order
     * @return iterable response
     */
    public static FindIterable<Json> find(String collection, Bson filter, Bson sort) {
        return find(collection, filter).sort(sort);
    }


    /**
     * Find all db objects with aggregation
     *
     * @param collection where search
     * @return iterable response
     */
    public static FindIterable<Json> find(String collection) {
        return find(collection, null);
    }

    /**
     * Find multiple db objects with aggregation
     *
     * @param collection where search
     * @param pipeline   operations
     * @return iterable response
     */
    public static AggregateIterable<Json> aggregate(String collection, List<? extends Bson> pipeline) {
        return getDb(collection).aggregate(pipeline);
    }

    /**
     * Count db objects
     *
     * @param collection where search
     * @param filter     restrictions
     * @return long result of counts
     */
    public static long count(String collection, Bson filter) {
        if (filter == null) {
            return getDb(collection).countDocuments();
        }
        return getDb(collection).countDocuments(filter);
    }

    /**
     * Count db objects with limitation
     *
     * @param collection where search
     * @param filter     restrictions
     * @param limit      count maximum
     * @return long result of counts
     */
    public static long countLimit(String collection, Bson filter, int limit) {
        return getDb(collection).countDocuments(filter, new CountOptions().limit(limit));
    }

    /**
     * Count all db objects in a collection
     *
     * @param collection where search
     * @return long result of counts
     */
    public static long count(String collection) {
        return count(collection, Filters.exists("_id", true));
    }

    /**
     * Delete one db object
     *
     * @param collection where search
     * @param filter     restrictions
     * @return true|false deleted or not
     */
    public static boolean deleteOne(String collection, Bson filter) {
        return getDb(collection).deleteOne(filter).getDeletedCount() > 0L;
    }

    /**
     * Delete db objects
     *
     * @param collection where search
     * @param filter     restrictions
     * @return delete informations
     */
    public static DeleteResult deleteMany(String collection, Bson filter) {
        return getDb(collection).deleteMany(filter);
    }

    /**
     * Test if db objects exists
     *
     * @param collection where search
     * @param filter     restrictions
     * @return true|false exists or not
     */
    public static boolean exists(String collection, Bson filter) {
        return countLimit(collection, filter, 1) > 0;
    }

    /**
     * Update db objects
     *
     * @param collection where search
     * @param filter     restrictions
     * @param update     operations
     * @return update result class
     */
    public static UpdateResult updateMany(String collection, Bson filter, Bson update) {
        return getDb(collection).updateMany(filter, update);
    }


    /**
     * Update one db object
     *
     * @param collection where search
     * @param filter     restrictions
     * @param update     operations
     * @return update result class
     */
    public static UpdateResult updateOne(String collection, Bson filter, Bson update) {
        return getDb(collection).updateOne(filter, update);
    }

    /**
     * Update one db object
     *
     * @param collection where search
     * @param filter     restrictions
     * @param update     multiple and consecutive operations
     * @return update result class
     */
    public static UpdateResult updateOne(String collection, Bson filter, List<Bson> update) {
        return getDb(collection).updateOne(filter, update);
    }

    /**
     * Update one db object
     *
     * @param collection where search
     * @param filter     restrictions
     * @param update     operations
     * @param options    operations
     * @return update result class
     */
    public static UpdateResult updateOne(String collection, Bson filter, Bson update, UpdateOptions options) {
        return getDb(collection).updateOne(filter, update, options);
    }


    /**
     * Find one db object and update
     *
     * @param collection where search
     * @param filter     restrictions
     * @param update     operations
     * @param options    operations
     * @return db object
     */
    public static Json findOneAndUpdate(String collection, Bson filter, Bson update, FindOneAndUpdateOptions options) {
        return getDb(collection).findOneAndUpdate(filter, update, options);
    }


    /**
     * Find one db object, update and return after operations
     *
     * @param collection where search
     * @param filter     restrictions
     * @param update     operations
     * @return updated db object
     */
    public static Json findOneAndUpdate(String collection, Bson filter, Bson update) {
        return getDb(collection).findOneAndUpdate(filter, update, new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
    }

    public static ClientSession getSession() {
        return client.startSession();
    }

    /**
     * Seed DB and build indexes
     *
     * @param unused as named
     */
    @Override
    public void contextInitialized(ServletContextEvent unused) {
        try {
            IndexBuilder.seed();
            IndexBuilder.buildIndexes();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Close Db to avoid memory leaks
     *
     * @param unused as named
     */
    @Override
    public void contextDestroyed(ServletContextEvent unused) {
        client.close();
    }
}
