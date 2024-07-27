/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.sessions;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import live.page.hubd.content.users.RelationsUtils;
import live.page.hubd.content.users.UsersUtils;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.db.utils.Pipeline;
import live.page.hubd.system.db.utils.paginer.Paginer;
import live.page.hubd.system.json.Json;
import live.page.hubd.utils.Fx;

import java.util.Date;

public class AccountsUtils {

    public static Json getSubAccounts(String parent, String paging) {
        Paginer paginer = new Paginer(paging, "-join", 20);

        Pipeline pipeline = new Pipeline();

        pipeline.add(Aggregates.match(Filters.eq("parent", parent)));

        pipeline.add(paginer.getFirstSort());
        pipeline.add(paginer.getLimit());
        pipeline.add(paginer.getLastSort());

        pipeline.add(Aggregates.project(new Json("name", true).put("key", true).put("email", true).put("subjects", "0").put("series", "0").put("templates", "0").put("accounts", "0")));

        return paginer.getResult("Users", pipeline);

    }

    public static Json generateUser(String name, String email, String user_id) {
        Json subuser = UsersBase.getBase();
        subuser.put("key", Fx.getSecureKey().toLowerCase());
        subuser.put("parent", user_id);
        subuser.put("join", new Date());
        //TODO duplicate email ?
        subuser.put("email", email);
        subuser.put("name", UsersUtils.uniqueName(name));
        Db.save("Users", subuser);
        RelationsUtils.addRelation(user_id, subuser.getId(), true);
        return subuser;
    }

    public static Json initUser(String user_id_toinit, String user_id) {
        Json user = Db.findOneAndUpdate("Users", Filters.and(Filters.eq("_id", user_id_toinit), Filters.eq("parent", user_id)),
                new Json("$set", new Json("key", Fx.getSecureKey().toLowerCase()))
                        .put("$unset", new Json("email", "").put("password", "")),
                new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
        if (user != null) {
            BaseSession.deleteUserSessions(user.getId());
            return new Json("ok", true);
        }
        return new Json("ok", false);
    }

    public static Json renameUser(String name, String user_id, String parent_id) {
        name = UsersUtils.uniqueName(user_id, name);
        if (Db.updateOne("Users", Filters.and(Filters.eq("parent", parent_id), Filters.eq("_id", user_id)), new Json("$set", new Json("name", name))).getModifiedCount() > 0L) {
            return new Json("ok", true).put("name", name);
        }
        return new Json("ok", false).put("name", name);
    }

    public static Json delete(String from, String to, Users user) {
        //TODO reafect all datas

        RelationsUtils.removeRelation(from, user.getId());

        return new Json("ok", Db.deleteOne("Users", Filters.and(Filters.eq("_id", from), Filters.eq("parent", user.getId()))));

    }

}
