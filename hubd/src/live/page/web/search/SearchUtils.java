/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.search;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.TextSearchOptions;
import live.page.web.db.Pipeliner;
import live.page.web.utils.Fx;
import live.page.web.utils.ObjsUtils;
import live.page.web.utils.json.Json;
import live.page.web.utils.paginer.Paginer;
import live.page.web.utils.paginer.PolyPaginer;
import org.bson.conversions.Bson;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SearchUtils {
	private static final int limit = 10;

	public static Json search(String query, String lng, String type, String paging_str) {
		try {
			if (type != null && !type.equals("")) {
				if (!ObjsUtils.getSearchers().containsKey(type)) {
					return null;
				}
				return SearchUtils.searchOne(query, lng, type, paging_str);
			}
			return SearchUtils.searchAll(query, lng, paging_str, ObjsUtils.getSearchers().keySet());

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static Json searchAll(String query, String lng, String paging_str, Set<String> keys) throws Exception {

		PolyPaginer paginer = new PolyPaginer(paging_str, "-score", limit, keys);

		List<Bson> pipeline = new ArrayList<>();

		pipeline.add(Aggregates.limit(1));

		pipeline.add(Aggregates.project(new Json("_id", false).put("empty", true)));

		for (String collection : ObjsUtils.getSearchers().keySet()) {

			Pipeliner searcher = ObjsUtils.getSearchers().get(collection).getConstructor(String.class, String.class, Paginer.class).newInstance(collection, lng, paginer);
			pipeline.add(Aggregates.lookup(Fx.ucfirst(collection), searcher
							.addFilter(Filters.text(query, new TextSearchOptions().language(lng)))
							.getSearcher(),
					collection));
		}


		List<String> keys_exps = new ArrayList<>();
		for (String key : ObjsUtils.getSearchers().keySet()) {
			keys_exps.add("$" + key);
		}
		pipeline.add(Aggregates.project(new Json("result", new Json("$concatArrays", keys_exps))));

		pipeline.add(Aggregates.unwind("$result"));
		pipeline.add(Aggregates.replaceRoot("$result"));

		pipeline.add(paginer.getLastSort());

		pipeline.add(paginer.getLimit());

		return paginer.getResult("Users", pipeline);

	}

	private static Json searchOne(String query, String lng, String collection, String paging_str) throws Exception {
		Paginer paginer = new Paginer(paging_str, "-score", limit);
		Pipeliner searcher = ObjsUtils.getSearchers().get(collection).getConstructor(String.class, String.class, Paginer.class).newInstance(collection, lng, paginer);
		return paginer.getResult(Fx.ucfirst(collection), searcher.addFilter(Filters.text(query, new TextSearchOptions().language(lng))).setLng(lng).getSearcher());
	}


	public static String cleanQuery(String query) {
		if (query == null) {
			return null;
		}
		query = Normalizer.normalize(query, Normalizer.Form.NFKC).replaceAll("(</?[^>]+>)", "");
		return Fx.truncate(query, 150);
	}
}
