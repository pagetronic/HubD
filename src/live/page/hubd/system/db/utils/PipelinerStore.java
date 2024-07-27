package live.page.hubd.system.db.utils;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Filters;
import live.page.hubd.content.pages.PagesPipelines;
import live.page.hubd.content.threads.PostsPipeliner;
import live.page.hubd.system.db.utils.paginer.Paginer;
import live.page.hubd.system.db.utils.paginer.PolyPaginer;
import live.page.hubd.system.json.Json;
import org.bson.conversions.Bson;

import java.util.*;

/**
 * A store of method to query standards and specials collections
 */
public class PipelinerStore {

    private static final Map<String, Class<? extends PipelineMaker>> methods = new HashMap<>();

    static {
        addPipeliner("posts", PostsPipeliner.class);
        addPipeliner("pages", PagesPipelines.class);
    }

    /**
     * Add a method to this store
     *
     * @param key where store this method
     * @param cls class of the method
     */
    public static void addPipeliner(String key, Class<? extends PipelineMaker> cls) {
        methods.put(key, cls);
    }

    /**
     * Get all methods for special interrogations
     *
     * @return all methods stored
     */
    public static Map<String, Class<? extends PipelineMaker>> getMethods() {
        return methods;
    }

    /**
     * Get standard construcor for a specific method
     *
     * @param key where is the method wanted
     * @return the method
     */
    public static PipelineMaker getConstructor(String key) {
        try {
            return methods.get(key.toLowerCase()).getConstructor(String.class, String.class, Paginer.class).newInstance(null, null, null);
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * Class of method to abstract
     */
    public abstract static class PipelineMaker {

        protected final Paginer paginer;
        private final List<Bson> filters = new ArrayList<>();
        protected String type = null;
        protected String lng = null;


        public PipelineMaker(String type, Paginer paginer) {
            this.type = type;
            this.paginer = paginer;
        }

        public PipelineMaker setLng(String lng) {
            this.lng = lng;
            return this;
        }

        public PipelineMaker addFilter(Bson filter) {
            filters.add(filter);
            return this;
        }

        protected Bson getFilter() {
            if (!filters.isEmpty()) {
                return Filters.and(filters);
            } else {
                return null;
            }
        }

        public List<Bson> getSearcher() {
            Pipeline pipeline = new Pipeline();
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
                    .put("logo", "$logo").put("date", "$date").put("url", "$url").put("breadcrumb", "$breadcrumb")
                    .put("score", "$score")
                    .put("type", type).put("tag", "$tag"))
            );

            return pipeline;
        }


        abstract protected List<Bson> getSearchPipeline();


    }
}
