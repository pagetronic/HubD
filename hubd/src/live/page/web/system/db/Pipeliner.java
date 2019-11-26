/*
 * Copyright (c) 2019. PAGE and Sons
 */
package live.page.web.system.db;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Filters;
import live.page.web.system.json.Json;
import live.page.web.system.db.paginer.Paginer;
import live.page.web.system.db.paginer.PolyPaginer;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class Pipeliner {

	protected final Paginer paginer;
	protected String type = null;
	protected String lng = null;

	private final List<Bson> filters = new ArrayList<>();


	public Pipeliner(String type, Paginer paginer) {
		this.type = type;
		this.paginer = paginer;
	}

	public Pipeliner setLng(String lng) {
		this.lng = lng;
		return this;
	}

	public Pipeliner addFilter(Bson filter) {
		filters.add(filter);
		return this;
	}

	protected Bson getFilter() {
		if (filters.size() > 0) {
			return Filters.and(filters);
		} else {
			return null;
		}
	}

	public List<Bson> getSearcher() {
		List<Bson> pipeline = new ArrayList<>();
		pipeline.add(Aggregates.match(getFilter()));
		pipeline.add(Aggregates.addFields(new Field<>("score",
				new Json("$divide", Arrays.asList(new Json("$floor", new Json("$multiply", Arrays.asList(new Json("$meta", "textScore"), 10000))), 10000))))
		);
		pipeline.add(paginer.getFirstSort());
		Bson paging_filter = paginer.getClass().equals(PolyPaginer.class) ? ((PolyPaginer) paginer).getFilters(type) : paginer.getFilters();

		if (paging_filter != null) {
			pipeline.add(Aggregates.match(paging_filter));
		}
		pipeline.add(paginer.getLimit());
		pipeline.addAll(getSearchPipeline());
		pipeline.add(paginer.getLastSort());
		pipeline.add(Aggregates.project(new Json().put("title", "$title").put("intro", "$intro")
				.put("svg", "$svg").put("logo", "$logo").put("date", "$date").put("url", "$url").put("breadcrumb", "$breadcrumb")
				.put("score", "$score")
				.put("type", type).put("tag", "$tag"))
		);

		return pipeline;
	}


	abstract protected List<Bson> getSearchPipeline();

	abstract public List<Bson> getUrlDbTags(Aggregator grouper, String key);

}
