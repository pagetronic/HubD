/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.system.db;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import live.page.web.utils.Fx;
import live.page.web.system.Settings;
import live.page.web.system.json.Json;
import live.page.web.system.json.JsonProvider;
import org.apache.commons.lang3.RandomStringUtils;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@WebListener
public class Db implements ServletContextListener {

	public static final int DB_KEY_LENGTH = 26;
	private static int keyDiff = 0;
	private static final int TIME_OUT = 10000;

	public static final CodecRegistry codecRegistry = CodecRegistries.fromRegistries(CodecRegistries.fromProviders(new JsonProvider()), MongoClientSettings.getDefaultCodecRegistry());

	private static final MongoClient client = MongoClients.create(MongoClientSettings.builder()
			.applicationName(Settings.SITE_TITLE)
			.retryReads(true).retryWrites(true)
			.applyToConnectionPoolSettings(builder -> builder.maxSize(100).minSize(0).maxConnectionIdleTime(60000, TimeUnit.MILLISECONDS))
			.applyToSocketSettings(builder -> builder.connectTimeout(TIME_OUT, TimeUnit.MILLISECONDS))
			.applyToClusterSettings(builder -> builder.hosts(Arrays.asList(new ServerAddress("localhost"))))
			.credential(MongoCredential.createCredential(Settings.DB_USER, Settings.DB_NAME, Settings.DB_PASS))
			.writeConcern(WriteConcern.JOURNALED).codecRegistry(codecRegistry).build());

	private static final MongoDatabase db = client.getDatabase(Settings.DB_NAME);

	public synchronized static String getKey() {
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

	public synchronized static MongoDatabase getDb() {
		return db;
	}

	public synchronized static MongoCollection<Json> getDb(String collection) {
		return db.getCollection(collection, Json.class);
	}

	public synchronized static Json findByIdUser(String collection, String _id, String user_id) {
		Bson filter = Filters.and(Filters.eq("_id", _id), Filters.eq("user", user_id));
		return Db.find(collection, filter).first();
	}

	public synchronized static Json findById(String collection, String _id) {
		if (_id == null) {
			return null;
		}
		return find(collection, Filters.and(Filters.eq("_id", _id), Filters.ne("_id", null))).first();
	}

	public synchronized static boolean save(String collection, Json document) {
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
				getDb(collection).replaceOne(Filters.eq("_id", document.get("_id")), document);
			} catch (Exception e) {
				return false;
			}
		}
		return true;
	}

	public synchronized static boolean save(String collection, List<Json> documents) {
		try {
			// Can't "update" when insertMany used, so no Ids needed.
			documents.forEach(doc -> doc.put("_id", getKey()));
			getDb(collection).insertMany(documents);
			return true;
		} catch (Exception e) {
			if (Fx.IS_DEBUG) {
				e.printStackTrace();
			}
			return false;
		}
	}

	public synchronized static FindIterable<Json> find(String collection, Bson filter) {
		if (filter == null) {
			return getDb(collection).find();
		}
		return getDb(collection).find(filter);
	}

	public synchronized static FindIterable<Json> find(String collection, Bson filter, Bson sort) {
		return find(collection, filter).sort(sort);
	}

	public synchronized static AggregateIterable<Json> aggregate(String collection, List<? extends Bson> pipeline) {
		return getDb(collection).aggregate(pipeline);
	}

	public synchronized static FindIterable<Json> find(String collection) {
		return find(collection, null);
	}

	public synchronized static long count(String collection, Bson filter) {
		if (filter == null) {
			return getDb(collection).countDocuments();
		}
		return getDb(collection).countDocuments(filter);
	}

	public synchronized static long countLimit(String collection, Bson filter, int limit) {
		return getDb(collection).countDocuments(filter, new CountOptions().limit(limit));
	}

	public synchronized static long count(String collection) {
		return count(collection, Filters.exists("_id", true));
	}

	public synchronized static boolean deleteOne(String collection, Bson filter) {
		return getDb(collection).deleteOne(filter).getDeletedCount() > 0L;
	}

	public synchronized static DeleteResult deleteMany(String collection, Bson filter) {
		return getDb(collection).deleteMany(filter);
	}

	public synchronized static boolean exists(String collection, Bson filter) {
		return countLimit(collection, filter, 1) > 0;
	}

	public synchronized static UpdateResult updateMany(String collection, Bson filter, Bson update) {
		return getDb(collection).updateMany(filter, update);
	}

	public synchronized static UpdateResult updateOne(String collection, Bson filter, Bson update) {
		return getDb(collection).updateOne(filter, update);
	}

	public synchronized static UpdateResult updateOne(String collection, Bson filter, List<Bson> update) {
		return getDb(collection).updateOne(filter, update);
	}

	public synchronized static UpdateResult updateOne(String collection, Bson filter, Bson update, UpdateOptions options) {
		return getDb(collection).updateOne(filter, update, options);
	}

	public synchronized static Json findOneAndUpdate(String collection, Bson filter, Bson update, FindOneAndUpdateOptions options) {
		return getDb(collection).findOneAndUpdate(filter, update, options);
	}

	public synchronized static Json findOneAndUpdate(String collection, Bson filter, Bson update) {
		return getDb(collection).findOneAndUpdate(filter, update, new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
	}

	@Override
	public synchronized void contextInitialized(ServletContextEvent sce) {
		try {
			IndexBuilder.seed();
			IndexBuilder.buildIndexes();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public synchronized void contextDestroyed(ServletContextEvent sce) {
		client.close();
	}
}
