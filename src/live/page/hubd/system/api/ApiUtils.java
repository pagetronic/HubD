/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.api;

import com.mongodb.client.model.*;
import com.mongodb.client.result.UpdateResult;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.db.utils.Aggregator;
import live.page.hubd.system.db.utils.Pipeline;
import live.page.hubd.system.db.utils.paginer.Paginer;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.sessions.Users;
import live.page.hubd.system.utils.Fx;
import org.bson.conversions.Bson;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class ApiUtils {

    /**
     * remove access from OAuth service
     *
     * @param id   access ID
     * @param user granted removal
     * @return Api response ok=true|false
     */
    public static Json removeAccess(String id, Users user) {
        return new Json("ok", Db.deleteOne("ApiAccess",
                Filters.and(
                        Filters.eq("_id", id),
                        Filters.eq("user", user.getId())
                )
        ));

    }

    /**
     * refresh accessToken from OAuth service
     *
     * @param id   access ID to refresh
     * @param user granted refreshment
     * @return Api response ok=true|false
     */
    public static Json refreshAccess(String id, Users user) {

        return new Json("ok", Db.updateOne("ApiAccess",
                Filters.and(
                        Filters.eq("_id", id),
                        Filters.eq("user", user.getId())
                ),
                new Json("$set", new Json("access_token", Fx.getSecureKey())
                        .put("refresh_token", Fx.getSecureKey())
                        .put("expire", new Date(System.currentTimeMillis() + 3600000))
                )
        ));

    }

    /**
     * Create OAuth application
     *
     * @param user         granted creation
     * @param name         of the app
     * @param redirect_uri where redirection is authorized
     * @param scope        identification for authorization of management
     * @param auto         is come from an automatic request ?
     * @return the new Application informations or the same if an apps has been already created on automatic request
     */
    public static Json createApps(Users user, String name, String redirect_uri, String scope, boolean auto) {
        List<String> scopes = parseScope(scope);
        Json app = null;
        if (auto) {
            List<Bson> filters = new ArrayList<>();
            filters.add(Filters.eq("user", user.getId()));

            filters.add(Filters.exists("client_id", true));
            filters.add(Filters.exists("client_secret", true));

            if (redirect_uri != null) {
                filters.add(Filters.eq("redirect_uri", redirect_uri));
            }
            if (scopes != null) {
                filters.add(Filters.eq("scopes", scopes));
            }
            app = Db.find("ApiApps", Filters.and(filters)).first();
        }

        if (app == null) {
            app = new Json();
            app.put("name", name == null ? user.getString("name") + " App" : Fx.truncate(name, 255));
            String client_id = Fx.getSecureKey();
            while (Db.exists("ApiApps", Filters.eq("client_id", client_id))) {
                client_id = Fx.getSecureKey();
            }
            app.put("client_id", client_id);
            app.put("client_secret", Fx.getSecureKey());
            app.put("date", new Date());
            app.put("user", user.getId());
            app.put("scopes", scopes == null ? Scopes.scopes : scopes);
            if (redirect_uri != null) {
                app.add("redirect_uri", redirect_uri);
            }
            Db.save("ApiApps", app);
        }

        return new Json("ok", true).put("client_id", app.getString("client_id")).put("client_secret", app.getString("client_secret"));
    }

    /**
     * Generate an OAuth access for an user and an application
     *
     * @param id   of the application to connect the access
     * @param user granted access
     * @return
     */
    public static Json getAccess(String id, Users user) {

        Json app = null;
        if (id != null) {
            app = Db.find("ApiApps", Filters.and(Filters.eq("_id", id), Filters.eq("user", user.getId()))).first();
        }
        if (app == null) {
            return new Json("error", "NO_APPS");

        }


        Json access = new Json();
        Date date = new Date();
        Date expire = new Date(date.getTime() + 3600 * 1000);
        access.put("access_token", Fx.getSecureKey());
        access.put("refresh_token", Fx.getSecureKey());
        access.put("expire", expire);
        access.put("date", date);
        access.put("user", user.getId());
        access.put("app_id", app.getId());
        access.put("force", true);
        access.put("scopes", app.getList("scopes"));
        Db.save("ApiAccess", access);

        return new Json("ok", true).put("id", access.getId());
    }

    /**
     * Rename an OAuth application
     *
     * @param id      of the application
     * @param newname of the application
     * @param user    granted the rename
     * @return Api response rename true|false and cleaned name
     */
    public static Json renameApps(String id, String newname, Users user) {

        newname = Jsoup.clean(newname, new Safelist());

        UpdateResult rez = Db.updateOne("ApiApps",
                Filters.and(
                        Filters.eq("_id", id), Filters.eq("user", user.getId())
                )
                , new Json("$set", new Json("name", newname))
        );

        if (rez.getMatchedCount() > 0) {
            return new Json("ok", true).put("name", newname);
        }
        return new Json("ok", false);
    }

    /**
     * Delete an OAuth application
     *
     * @param id   of the application
     * @param user granted the deletion
     * @return Api response deleted true or error
     */
    public static Json deleteApps(String id, Users user) {

        Json app = Db.find("ApiApps", Filters.and(Filters.eq("_id", id), Filters.eq("user", user.getId()))).first();
        if (app == null) {
            return new Json("error", "NO_APPS");
        }

        //Backup
        app.put("client_id_before", app.getString("client_id")).remove("client_id");
        app.put("client_secret_before", app.getString("client_secret")).remove("client_secret");
        app.put("removed", true);

        Db.deleteMany("ApiAccess", Filters.eq("client_id", app.getString("client_id")));
        Db.save("ApiApps", app);

        return new Json("ok", true);
    }

    /**
     * Change OAuth application client secret
     *
     * @param id   of the application
     * @param user granted the change
     * @return Api response changing true and new client secret or error
     */
    public static Json changeSecret(String id, Users user) {
        Json app = Db.find("ApiApps", Filters.and(Filters.eq("_id", id), Filters.eq("user", user.getId()))).first();
        if (app == null) {
            return new Json("error", "NO_APPS");
        }
        app.put("client_secret_before", app.getString("client_secret"));
        app.put("client_secret", Fx.getSecureKey().substring(0, 12) + Fx.getSecureKey());
        Db.save("ApiApps", app);
        return new Json("ok", true).put("client_secret", app.get("client_secret"));
    }

    /**
     * Add or remove a redirection uri to an OAuth Application
     *
     * @param id           of the application
     * @param type         add or remove?
     * @param redirect_uri to add or remove
     * @param user         granted the change
     * @return Api response changing true|false
     */
    public static Json redirectUri(String id, String type, String redirect_uri, Users user) {

        if (type != null && redirect_uri != null) {
            Json accs = Db.findOneAndUpdate("ApiApps", Filters.and(Filters.eq("_id", id), Filters.eq("user", user.getId())),
                    new Json(type.equals("add") ? "$push" : "$pull", new Json("redirect_uri", redirect_uri)),
                    new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));

            if (accs != null) {
                return new Json("ok", true);
            }
        }

        return new Json("ok", false);

    }

    /**
     * Update Scopes from an OAuth application
     *
     * @param id     of the application
     * @param scopes to set
     * @param user   granted the change
     * @return Api response changing true|false
     */
    public static Json setScopes(String id, List<String> scopes, Users user) {
        if (scopes != null && Scopes.scopes.containsAll(scopes)) {
            scopes = Scopes.sort(scopes);
            Json accs = Db.findOneAndUpdate("ApiApps", Filters.and(Filters.eq("_id", id), Filters.eq("user", user.getId())),
                    new Json("$set", new Json("scopes", scopes)),
                    new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));

            if (accs != null) {
                return new Json("ok", true);
            }
        } else if (!Scopes.scopes.containsAll(scopes)) {
            return new Json("ok", false).put("error", "UNKNOWN_SCOP");
        }

        return new Json("ok", false);

    }

    /**
     * Get OAuth apps from user
     *
     * @param user       user need his accesses
     * @param paging_str pagination string
     * @return Iterable DB
     */
    public static Json getApps(Users user, String paging_str) {

        Aggregator aggregator = new Aggregator("_id", "date", "name", "client_id", "client_secret", "redirect_uri", "scopes");

        Paginer paginer = new Paginer(paging_str, "-date", 20);

        Pipeline pipeline = new Pipeline();
        List<Bson> filters = new ArrayList<>();


        filters.add(Filters.eq("user", user.getId()));
        filters.add(Filters.ne("client_id", null));
        if (paginer.getFilters() != null) {
            filters.add(paginer.getFilters());
        }

        pipeline.add(Aggregates.match(Filters.and(filters)));
        pipeline.add(paginer.getFirstSort());
        pipeline.add(paginer.getLimit());
        pipeline.add(paginer.getLastSort());

        pipeline.add(Aggregates.project(aggregator.getProjectionOrder()));

        return paginer.getResult("ApiApps", pipeline);
    }

    /**
     * Get OAuth access authorized by user
     *
     * @param user       user need his accesses
     * @param paging_str pagination string
     * @return Iterable DB
     */
    public static Json getAccesses(Users user, String paging_str) {

        Aggregator aggregator = new Aggregator("_id", "date", "count", "scopes", "access_token", "refresh_token", "access", "expire", "app_name", "app_id");

        Paginer paginer = new Paginer(paging_str, "-date", 20);

        Pipeline pipeline = new Pipeline();
        List<Bson> filters = new ArrayList<>();

        filters.add(Filters.eq("user", user.getId()));
        if (paginer.getFilters() != null) {
            filters.add(paginer.getFilters());
        }
        pipeline.add(Aggregates.match(Filters.and(filters)));

        pipeline.add(paginer.getFirstSort());
        pipeline.add(paginer.getLimit());

        pipeline.add(Aggregates.lookup("ApiApps", "app_id", "_id", "app"));
        pipeline.add(Aggregates.unwind("$app"));

        pipeline.add(Aggregates.project(aggregator.getProjection().put("app_name", "$app.name").put("app_id", "$app._id")));

        pipeline.add(Aggregates.project(aggregator.getProjectionOrder()));

        pipeline.add(paginer.getLastSort());

        return paginer.getResult("ApiAccess", pipeline);


    }

    /**
     * URL scopes parser
     *
     * @param scope_str dot separate Scopes
     * @return clean scope list
     */
    public static List<String> parseScope(String scope_str) {
        if (scope_str == null || scope_str.isEmpty()) {
            return null;
        }
        List<String> scopes = new ArrayList<>();
        for (String scope : scope_str.split("( +)?([,+\\- ])( +)?", 60)) {
            if (Scopes.scopes.contains(scope)) {
                scopes.add(scope);
            }
        }
        scopes = Scopes.sort(scopes);
        return scopes;
    }

    /**
     * Verify if an OAuth application is known and all is correct
     *
     * @param client_id     of the app
     * @param client_secret of the app
     * @param scope         string dot separated of the app
     * @return infos about the app or error if the verification fail
     */
    public static Json verifyApp(String client_id, String client_secret, String scope) {
        Json rez = new Json();
        Json app = Db.find("ApiApps", Filters.and(
                Filters.eq("client_id", client_id),
                Filters.eq("client_secret", client_secret),
                Filters.eq("scopes", ApiUtils.parseScope(scope)))).first();
        if (app != null) {
            rez.put("date", app.getDate("date"));
            rez.put("name", app.getString("name"));
            rez.put("logo", app.getString("logo"));
        } else {
            rez.put("error", "UNKNOWN_APP");
        }
        return rez;
    }

    public static Json getUser(String accessToken) {
        return Db.aggregate("ApiAccess",
                Arrays.asList(
                        Aggregates.match(Filters.eq("access_token", accessToken)),
                        Aggregates.limit(1),
                        Aggregates.lookup("ApiApps", "app_id", "_id", "app"),
                        Aggregates.unwind("$app", new UnwindOptions().preserveNullAndEmptyArrays(true)),
                        Aggregates.lookup("Users", "user", "_id", "user"),
                        Aggregates.unwind("$user"),
                        Aggregates.addFields(
                                new Field<>("user.expire", "$expire"),
                                new Field<>("user.scopes", "$scopes"),
                                new Field<>("user.app_scopes", "$app.scopes"),
                                new Field<>("user.app_id", "$app._id"),
                                new Field<>("user.access", "$_id")
                        ),
                        Aggregates.replaceRoot("$user"),
                        Aggregates.lookup("Groups", "groups", "_id", "groups")
                )

        ).first();
    }
}

