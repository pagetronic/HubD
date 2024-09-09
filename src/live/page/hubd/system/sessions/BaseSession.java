/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.sessions;

import com.mongodb.client.model.*;
import com.mongodb.client.result.UpdateResult;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import live.page.hubd.content.users.UsersUtils;
import live.page.hubd.system.Settings;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.db.utils.Aggregator;
import live.page.hubd.system.db.utils.Pipeline;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.servlet.utils.BruteLocker;
import live.page.hubd.system.servlet.utils.ServletUtils;
import live.page.hubd.system.servlet.wrapper.ApiServletRequest;
import live.page.hubd.system.servlet.wrapper.ApiServletResponse;
import live.page.hubd.system.servlet.wrapper.BaseServletRequest;
import live.page.hubd.system.servlet.wrapper.BaseServletResponse;
import live.page.hubd.system.utils.Fx;
import live.page.hubd.system.utils.Mailer;
import org.bson.BsonUndefined;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@WebListener
public class BaseSession implements ServletContextListener {

    private static final ExecutorService service = Executors.newFixedThreadPool(10);

    public static Json buildSession(HttpServletRequest req, String user_id) {
        Json session = new Json();

        session.put("expire", new Date(System.currentTimeMillis() + (Settings.COOKIE_DELAY * 1000L)));
        session.put("_id", Fx.getSecureKey());
        session.put("ip", ServletUtils.realIp(req));
        session.put("ua", req.getHeader("User-Agent"));
        if (user_id != null) {
            session.put("user", user_id);
        }
        Db.getDb("Sessions").insertOne(session);
        return session;
    }

    public static void sendSession(BaseServletResponse resp, Json session) {
        BaseCookie gaia = new BaseCookie(session.getId());
        resp.addCookie(gaia);
    }

    private static String sendSession(BaseServletRequest req, BaseServletResponse resp, String user_id) {
        Json session = getOrCreateSession(req, resp);
        session.put("user", user_id);
        session.put("expire", new Date(System.currentTimeMillis() + (Settings.COOKIE_DELAY * 1000L)));
        Db.save("Sessions", session);
        return session.getId();

    }


    public static Users getOrCreateUser(ServletRequest request, ServletResponse response) {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;


        BaseCookie gaia = BaseCookie.getAuth(req);
        if (gaia == null) {
            return null;
        }
        gaia.setMaxAge(Settings.COOKIE_DELAY);
        //strange, have to clone
        resp.setHeader("Set-Cookie", ((BaseCookie) gaia.clone()).stringHeader());
        Json user = userForSession(gaia.getValue());


        if (user != null && user.getId() != null) {
            userAccess(user.getId());
            return new Users(user);
        } else if (user != null && !user.containsKey("provider")) {
            BaseCookie.clearAuth(req, resp);
        }
        return null;
    }


    public static boolean sessionValid(HttpServletRequest req) {
        BaseCookie gaia = BaseCookie.getAuth(req);
        if (gaia == null || gaia.getValue().isEmpty()) {
            return true;
        }
        return Db.exists("Sessions", Filters.eq("_id", gaia.getValue()));
    }


    public static void deleteUserSessions(String user_id) {
        Db.deleteMany("Sessions", Filters.eq("user", user_id));
    }


    public static Json getOrCreateSession(BaseServletRequest req, BaseServletResponse resp) {
        Json session = getSession(req);
        if (session == null) {
            session = buildSession(req, null);
            sendSession(resp, session);
        }
        return session;
    }


    public static Json getSession(HttpServletRequest req) {
        BaseCookie gaia = BaseCookie.getAuth(req);
        if (gaia == null) {
            return null;
        }
        Json session = Db.findById("Sessions", gaia.getValue());
        if (session == null) {
            BruteLocker.add(req);
        }
        return session;
    }

    public static Json getAuthorization(HttpServletRequest req) {
        if (req.getHeader("Authorization") == null) {
            return null;
        }
        Json session = Db.findById("Sessions", req.getHeader("Authorization"));
        if (session == null) {
            BruteLocker.add(req);
        }
        return session;
    }

    private static void userAccess(String user_id) {
        service.submit(() -> Db.updateOne("Users", Filters.eq("_id", user_id),
                new Json("$set", new Json("last", new Date()))));
    }

    public static void updateSession(String session_id) {
        service.submit(() ->
                Db.updateOne("Sessions",
                        Filters.eq("_id", session_id),
                        new Json("$set", new Json("expire", new Date(System.currentTimeMillis() + (Settings.COOKIE_DELAY * 1000L))))
                )
        );
    }

    public static Json register(BaseServletRequest req, BaseServletResponse resp, String name, String email, String new_password, Json settings, String key) {
        Json res = new Json();

        Json errors = new Json();

        if (email.isEmpty()) {
            errors.put("email", "EMPTY");
        } else if (!email.matches("^\\w+([-+.']\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$")) {
            errors.put("email", "INCONSISTENT");
        } else if (Db.exists("Users", Filters.eq("email", email))) {
            errors.put("email", "EXIST");
        } else if (email.length() > 80) {
            errors.put("email", "TOO_LONG");
        }
        if (new_password.length() < 5) {
            errors.put("password", "TOO_SHORT");
        } else if (new_password.length() > 200) {
            errors.put("password", "TOO_LONG");
        }
        if (key == null) {
            if (name.length() < 4) {
                errors.put("name", "TOO_SHORT");
            } else if (name.length() > 50) {
                errors.put("name", "TOO_LONG");
            } else {
                name = UsersUtils.uniqueName(name);
            }
        }

        if (!errors.isEmpty()) {

            res.put("errors", errors);
            return res;
        }

        Json user;
        if (key != null && !key.isEmpty()) {

            user = Db.findOneAndUpdate("Users", Filters.eq("key", key),
                    new Json()
                            .put("$unset", new Json("key", ""))
                            .put("$set", new Json().put("email", email).put("password", Fx.crypt(new_password)))
                    , new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));

        } else {

            user = UsersBase.getBase();
            user.put("name", name);
            user.put("email", email);
            user.put("settings", settings);
            user.put("password", Fx.crypt(new_password));
            Db.save("Users", user);
        }
        if (user == null) {
            return res.put("ok", false);
        }
        String session_id = sendSession(req, resp, user.getId());

        return res.put("ok", true).put("user", user.getId()).put("session", session_id);

    }

    public static Json login(BaseServletRequest req, BaseServletResponse resp, String email, String password) {
        Json res = new Json("ok", false);
        if (BruteLocker.isBan(ServletUtils.realIp(req))) {
            return res;
        }
        Json user = Db.find("Users",
                Filters.and(
                        Filters.eq("email", email),
                        Filters.eq("password", Fx.crypt(password))
                )
        ).first();
        if (user != null) {
            res = getUserData(user, false);
            sendSession(req, resp, user.getId());
            res.put("ok", true);
        } else {
            BruteLocker.add(req);
        }
        return res;
    }

    public static Users getUser(HttpServletRequest req, String email, String password) {
        if (BruteLocker.isBan(ServletUtils.realIp(req))) {
            return null;
        }
        Json user = Db.aggregate("Users",
                List.of(
                        Aggregates.match(Filters.and(
                                Filters.eq("email", email),
                                Filters.eq("password", Fx.crypt(password))
                        )),
                        Aggregates.lookup("Groups", "groups", "_id", "groups")
                )
        ).first();
        if (user != null) {
            userAccess(user.getId());
            return new Users(user);
        } else {
            BruteLocker.add(req);
        }
        return null;
    }

    public static Users getUser(HttpServletRequest req, String session) {
        if (session == null || BruteLocker.isBan(ServletUtils.realIp(req))) {
            return null;
        }
        Json user = userForSession(session);
        if (user != null) {
            updateSession(session);
            userAccess(user.getId());
            return new Users(user);
        } else {
            BruteLocker.add(req);
        }
        return null;
    }

    public static Users getUser(String ip, String session) {
        if (session == null || BruteLocker.isBan(ip)) {
            return null;
        }
        Json user = userForSession(session);
        if (user != null) {
            updateSession(session);
            userAccess(user.getId());
            return new Users(user);
        } else {
            BruteLocker.add(ip);
        }
        return null;
    }

    public static Json recover(ApiServletRequest req, String email) {
        Json res = new Json("ok", false);
        if (BruteLocker.isBan(ServletUtils.realIp(req))) {
            return res;
        }
        Json user = Db.findOneAndUpdate("Users",
                Filters.eq("email", email),
                new Json("$set", new Json("activate", Fx.getSecureKey())),
                new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        );
        if (user != null) {
            return new Json("ok", Mailer.sendActivation(req.getLng(), user.getString("email"), user.getString("activate")));
        } else {
            BruteLocker.add(req);
            return new Json("error", "UNKNOWN_USER");
        }
    }

    public static Json activate(ApiServletRequest req, ApiServletResponse resp, String activate) {
        if (activate == null || activate.isEmpty()) {
            return new Json("ok", false);
        }
        Json user = Db.findOneAndUpdate("Users",
                Filters.eq("activate", activate),
                new Json("$set", new Json("activate", Fx.getSecureKey())),
                new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        );
        if (user == null) {
            return new Json("ok", false);
        }
        return new Json("session", sendSession(req, resp, user.getId())).put("activation", user.getString("activate"));
    }

    public static Json password(String password, String newPassword, Users user) {
        if (user == null) {
            return new Json("ok", false);
        }

        if (password.length() < 5) {
            return new Json("ok", false).put("password", "TOO_SHORT");
        }
        UpdateResult update = Db.updateOne("Users",
                Filters.and(
                        Filters.eq("_id", user.getId()),
                        Filters.or(Filters.eq("activate", password), Filters.eq("password", Fx.crypt(password)))
                ),
                new Json().put("$set", new Json("password", Fx.crypt(newPassword)))
        );
        return new Json("ok", update.getMatchedCount() > 0);
    }


    public static Json userForSession(String session) {

        Aggregator aggregator = new Aggregator("name", "coins", "groups", "locale", "join", "avatar", "lng", "posts", "cash", "providers", "last", "email", "settings", "children");
        Pipeline pipeline = new Pipeline();
        pipeline.add(Aggregates.match(Filters.eq("_id", session)));
        pipeline.add(Aggregates.limit(1));
        pipeline.add(Aggregates.lookup("Users", "user", "_id", "user"));
        pipeline.add(Aggregates.unwind("$user", new UnwindOptions()));
        pipeline.add(new Json("$addFields", new Json("user.original", "$original").put("user.provider", "$provider")));
        pipeline.add(Aggregates.replaceRoot("$user"));

        pipeline.add(Aggregates.lookup("Groups", "groups", "_id", "groups"));

        List<Bson> subUserPipeline = List.of(
                Aggregates.lookup("BlobFiles", "avatar", "_id", "avatar"),
                Aggregates.unwind("$avatar", new UnwindOptions().preserveNullAndEmptyArrays(true)),
                Aggregates.sort(Sorts.descending("join")),
                Aggregates.lookup("Groups", "groups", "_id", "groups"),
                Aggregates.project(new Json()
                        .put("_id", "$_id")
                        .put("name", "$name")
                        .put("avatar",
                                new Json("$concat", Arrays.asList(
                                        Settings.getCDNHttp(),
                                        new Json("$cond",
                                                Arrays.asList(new Json("$eq", Arrays.asList("$avatar._id", new BsonUndefined())),
                                                        Settings.UI_LOGO,
                                                        new Json("$concat", Arrays.asList("/files/", "$avatar._id"))))
                                ))
                        )));


        pipeline.add(new Json("$lookup",
                new Json("from", "Users")
                        .put("localField", "_id")
                        .put("foreignField", "parent")
                        .put("as", "children")
                        .put("pipeline", List.of(
                                Aggregates.lookup("BlobFiles", "avatar", "_id", "avatar"),
                                Aggregates.unwind("$avatar", new UnwindOptions().preserveNullAndEmptyArrays(true)),
                                Aggregates.sort(Sorts.descending("join")),
                                Aggregates.lookup("Groups", "groups", "_id", "groups"),
                                Aggregates.project(new Json()
                                        .put("_id", "$_id")
                                        .put("name", "$name")
                                        .put("avatar",
                                                new Json("$concat", Arrays.asList(
                                                        Settings.getCDNHttp(),
                                                        new Json("$cond",
                                                                Arrays.asList(new Json("$eq", Arrays.asList("$avatar._id", new BsonUndefined())),
                                                                        Settings.UI_LOGO,
                                                                        new Json("$concat", Arrays.asList("/files/", "$avatar._id"))))
                                                ))
                                        ))))
        ));


        pipeline.add(Aggregates.graphLookup("Users", "$original", "parent", "_id", "original"));

        pipeline.add(Aggregates.unwind("$original", new UnwindOptions().preserveNullAndEmptyArrays(true)));

        pipeline.add(Aggregates.lookup("Groups", "original.groups", "_id", "original.groups"));

        pipeline.add(Aggregates.lookup("BlobFiles", "original.avatar", "_id", "original.avatar"));
        pipeline.add(Aggregates.unwind("$original.avatar", new UnwindOptions().preserveNullAndEmptyArrays(true)));

        pipeline.add(Aggregates.group("$_id", aggregator.getGrouper(
                Accumulators.push("original", "$original")
        )));
        pipeline.add(Aggregates.lookup("BlobFiles", "avatar", "_id", "avatar"));
        pipeline.add(Aggregates.unwind("$avatar", new UnwindOptions().preserveNullAndEmptyArrays(true)));

        pipeline.add(Aggregates.addFields(
                new Field<>("url", new Json("$concat", Arrays.asList("/users/", "$_id"))),
                new Field<>("coins", "$coins"),
                new Field<>("avatar",
                        new Json("$concat", Arrays.asList(
                                Settings.getCDNHttp(),
                                new Json("$cond",
                                        Arrays.asList(new Json("$eq", Arrays.asList("$avatar._id", new BsonUndefined())),
                                                Settings.UI_LOGO,
                                                new Json("$concat", Arrays.asList("/files/", "$avatar._id"))))
                        ))
                ))

        );

        pipeline.add(Aggregates.addFields(new Field<>("original",
                                new Json("$filter", new Json("input",
                                        new Json("$map", new Json("input", "$original")
                                                .put("as", "original")
                                                .put("in",
                                                        new Json("$cond", new Json()
                                                                .put("if", new Json("$ne", Arrays.asList("$$original._id", new BsonUndefined())))
                                                                .put("then",
                                                                        new Json()
                                                                                .put("_id", "$$original._id")
                                                                                .put("name", "$$original.name")
                                                                                .put("avatar",
                                                                                        new Json("$concat", Arrays.asList(
                                                                                                Settings.getCDNHttp(),
                                                                                                new Json("$cond",
                                                                                                        Arrays.asList(new Json("$eq", Arrays.asList("$$original.avatar._id", new BsonUndefined())),
                                                                                                                Settings.UI_LOGO,
                                                                                                                new Json("$concat", Arrays.asList("/files/", "$$original.avatar._id"))))
                                                                                        ))
                                                                                )
                                                                                .put("groups", "$$original.groups")

                                                                )
                                                                .put("else", null)
                                                        )
                                                )
                                        )).put("as", "original").put("cond",
                                        new Json("$and", Arrays.asList(
                                                new Json("$ne", Arrays.asList("$$original._id", new BsonUndefined())),
                                                new Json("$ne", Arrays.asList("$$original._id", null))
                                        ))
                                ))
                        )

                )

        );
        pipeline.add(Aggregates.addFields(
                        new Field<>("original",
                                new Json("$cond", Arrays.asList(
                                        new Json("$eq", Arrays.asList(new Json("$size", "$original"), 0)), "$$REMOVE", "$original"))
                        )
                )

        );


        return Db.aggregate("Sessions", pipeline).first();
    }

    public static Json avatar(Users user, String avatar) {
        if (user == null || avatar == null || !Db.exists("BlobFiles", Filters.eq("_id", avatar))) {
            return new Json("ok", false);
        }
        return new Json("ok", Db.updateOne("Users",
                Filters.eq("_id", user.getId()),
                new Json("$set", new Json("avatar", avatar))
        ).getModifiedCount() > 0);
    }

    public static Json getUserData(Json user_, boolean email) {
        Users user = new Users(user_);
        Json data = new Json();

        data.put("id", user.getId());
        data.put("name", user.getString("name"));
        if (email) {
            data.put("email", user.getString("email"));
        }
        data.put("join", user.getDate("join"));

        data.put("notices", countNotices(user.getId()));

        data.put("cash", user.getJson("cash"));
        data.put("coins", user.getInteger("coins", 0));


        if (user.getListJson("children") != null && !user.getListJson("children").isEmpty()) {
            data.put("children", user.getListJson("children"));
        }

        if (user.getListJson("original") != null && !user.getListJson("original").isEmpty()) {
            data.put("original", user.getListJson("original"));
        }

        Json settings = user.getJson("settings");
        if (settings != null) {
            data.put("currency", settings.getString("currency"));
        }


        if (user.getString("avatar") != null) {
            data.put("avatar", (user.getString("avatar").startsWith("http") ? "" : Settings.getCDNHttp() + "/files/") + user.getString("avatar"));
        } else {
            data.put("avatar", Settings.getLogo());
        }

        data.put("admin", user.getAdmin());

        return data;
    }

    public static void clearSession(HttpServletRequest req, HttpServletResponse resp) {
        BaseCookie cookie = BaseCookie.getAuth(req);
        if (cookie != null) {
            Db.deleteOne("Sessions", Filters.eq("_id", cookie.getValue()));
            cookie.setMaxAge(0);
            resp.setHeader("Set-Cookie", cookie.stringHeader());
        }
    }

    public static Json tos(Users user, boolean accept) {
        if (user == null) {
            return new Json("ok", false);
        }
        if (accept) {
            Db.updateOne("Users", Filters.eq("_id", user.getId()), new Json("$set", new Json("tos", new Date())));
        } else {
            Db.updateOne("Users", Filters.eq("_id", user.getId()), new Json("$unset", new Json("tos", "")));
        }
        return new Json("ok", true);
    }

    public static String countNotices(String user_id) {
        List<Json> notices = Db.aggregate("Notices", List.of(
                Aggregates.match(Filters.eq("user", user_id)),
                Aggregates.project(new Json().put("grouper", true)),
                Aggregates.group("$grouper"),
                Aggregates.limit(100)
        )).into(new ArrayList<>());
        int counts = notices.size();
        return counts >= 100 ? "99+" : counts + "";
    }


    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        Fx.shutdownService(service);
    }
}
