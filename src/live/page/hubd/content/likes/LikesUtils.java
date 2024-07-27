package live.page.hubd.content.likes;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Filters;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.db.utils.Updater;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.sessions.Users;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class LikesUtils {

    public static List<Bson> addPipelineLiked(Users user) {
        List<Bson> pipeline = new ArrayList<>();

        if (user != null) {
            pipeline.add(new Json("$lookup",
                    new Json("from", "Likes")
                            .put("localField", "_id")
                            .put("foreignField", "parent")
                            .put("pipeline", List.of(Aggregates.match(Filters.eq("user", user.getId()))))
                            .put("as", "liked")
            ));
            pipeline.add(Aggregates.addFields(new Field<>("liked",
                    new Json("$cond", new Json()
                            .put("if", new Json("$gt", Arrays.asList(new Json("$size", "$liked"), 0)))
                            .put("then", true)
                            .put("else", false)
                    )
            )));
        }

        return pipeline;
    }

    public static Json like(String type, String id, boolean like, Users user) {
        if (like) {
            if (Db.save("Likes", new Json().put("parent", id).put("user", user.getId()).put("date", new Date()))) {

                if (type.equals("post")) {
                    Db.updateOne("Posts", Filters.eq("_id", id), new Updater().inc("likes", 1).get());
                    return new Json("ok", true);
                }

            }
        } else if (Db.deleteOne("Likes", Filters.and(Filters.eq("parent", id), Filters.eq("user", user.getId())))) {

            if (type.equals("post")) {
                Db.updateOne("Posts", Filters.eq("_id", id), new Updater().inc("likes", -1).get());
                return new Json("ok", true);
            }
        }
        return new Json("ok", false);
    }

}
