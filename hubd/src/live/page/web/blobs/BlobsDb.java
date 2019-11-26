/*
 * Copyright (c) 2019. PAGE and Sons
 */

package live.page.web.blobs;

import com.mongodb.client.model.*;
import live.page.web.system.Settings;
import live.page.web.system.db.Aggregator;
import live.page.web.system.db.Db;
import live.page.web.system.db.paginer.Paginer;
import live.page.web.system.json.Json;
import live.page.web.system.sessions.Users;
import live.page.web.utils.Fx;
import org.bson.BsonUndefined;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BlobsDb {


	/**
	 * Get user files in DB
	 */
	public static Json getFiles(String user_id, String paging_str) {


		Aggregator grouper = new Aggregator("name", "type", "size", "date", "url");

		Paginer paginer = new Paginer(paging_str, "-date", 30);

		Bson paging_filter = paginer.getFilters();

		List<Bson> pipeline = new ArrayList<>();

		List<Bson> filters = new ArrayList<>();

		filters.add(Filters.eq("user", user_id));

		if (paging_filter != null) {
			filters.add(paging_filter);
		}

		pipeline.add(Aggregates.match(Filters.and(Filters.and(filters))));

		pipeline.add(paginer.getFirstSort());
		pipeline.add(paginer.getLimit());

		pipeline.add(Aggregates.project(grouper.getProjection().put("url", new Json("$concat", Arrays.asList(Settings.getCDNHttp(), "/files/", "$_id")))));

		pipeline.add(Aggregates.project(grouper.getProjectionOrder()));

		pipeline.add(paginer.getLastSort());


		return paginer.getResult("BlobFiles", pipeline);

	}

	/**
	 * Simple update file infos
	 */
	public static Json updateText(Json data, Users user) {
		List<Bson> filters = new ArrayList<>();
		filters.add(Filters.eq("_id", data.getId()));
		if (!user.getEditor()) {
			filters.add(Filters.eq("user", user.getId()));
		}
		return new Json("ok", Db.updateOne("BlobFiles", Filters.and(filters), new Json("$set", new Json("text", Fx.normalizePost(data.getString("text"))))).getModifiedCount() > 0);
	}

	/**
	 * Remove file
	 */
	public static boolean remove(Users user, String id) {
		Json file = Db.findByIdUser("BlobFiles", id, user.getId());
		if (file == null) {
			return false;
		}
		Db.deleteOne("BlobFiles", Filters.eq("_id", file.getId()));
		Db.deleteMany("BlobChunks", Filters.eq("f", file.getId()));
		return true;
	}

	/**
	 * Get aggregate pipeline for blob
	 */
	public static List<Bson> getBlobsPipeline(Aggregator grouper) {
		List<Bson> pipeline = new ArrayList<>();
		pipeline.add(Aggregates.unwind("$docs", new UnwindOptions().preserveNullAndEmptyArrays(true).includeArrayIndex("pos_doc")));
		pipeline.add(Aggregates.lookup("BlobFiles", "docs", "_id", "docs"));
		pipeline.add(Aggregates.unwind("$docs", new UnwindOptions().preserveNullAndEmptyArrays(true)));
		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("docs", new Json("_id", true).put("size", true).put("text", true))
		));
		pipeline.add(Aggregates.sort(Sorts.ascending("pos_doc")));

		pipeline.add(Aggregates.group("$_id", grouper.getGrouper(Accumulators.push("docs", "$docs"))));
		pipeline.add(Aggregates.project(grouper.getProjection()
				.put("docs", new Json("$filter", new Json("input", "$docs").put("as", "docs").put("cond", new Json("$ne", Arrays.asList("$$docs._id", new BsonUndefined())))))
		));
		return pipeline;
	}
}
