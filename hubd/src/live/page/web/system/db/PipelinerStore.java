package live.page.web.system.db;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Filters;
import live.page.web.content.pages.PagesAggregator;
import live.page.web.content.posts.utils.ThreadsAggregator;
import live.page.web.system.db.paginer.Paginer;
import live.page.web.system.db.paginer.PolyPaginer;
import live.page.web.system.json.Json;
import org.bson.conversions.Bson;

import java.util.*;

/**
 * A store of method to query standards and specials collections
 */
public class PipelinerStore {

	private static final Map<String, Class<? extends Pipeliner>> methods = new HashMap<>();

	static {
		addPipeliner("posts", ThreadsAggregator.PostsPipeliner.class);
		addPipeliner("pages", PagesAggregator.PagesPipelines.class);
	}

	/**
	 * Add a method to this store
	 *
	 * @param key where store this method
	 * @param cls class of the method
	 */
	public static void addPipeliner(String key, Class<? extends Pipeliner> cls) {
		methods.put(key, cls);
	}

	/**
	 * Get all methods for special interrogations
	 *
	 * @return all methods stored
	 */
	public static Map<String, Class<? extends Pipeliner>> getMethods() {
		return methods;
	}

	/**
	 * Get standard construcor for a specific method
	 *
	 * @param key where is the method wanted
	 * @return the method
	 */
	public static Pipeliner getConstructor(String key) {
		try {
			return methods.get(key.toLowerCase()).getConstructor(String.class, String.class, Paginer.class).newInstance(null, null, null);
		} catch (Exception e) {
			return null;
		}
	}


	/**
	 * Class of method to abstract
	 */
	public abstract static class Pipeliner {

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
}
