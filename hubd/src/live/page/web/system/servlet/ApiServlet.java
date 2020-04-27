/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.system.servlet;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UnwindOptions;
import live.page.web.system.Settings;
import live.page.web.system.db.Db;
import live.page.web.system.json.Json;
import live.page.web.system.servlet.utils.Api;
import live.page.web.system.servlet.utils.BruteLocker;
import live.page.web.system.servlet.utils.ServletUtils;
import live.page.web.system.servlet.wrapper.ApiServletRequest;
import live.page.web.system.servlet.wrapper.ApiServletResponse;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.system.sessions.BaseCookie;
import live.page.web.system.sessions.BaseSession;
import live.page.web.system.sessions.Users;
import live.page.web.utils.Fx;
import org.apache.commons.io.IOUtils;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class ApiServlet extends FullServlet {


	/**
	 * Redirect user to correct function on API request
	 */
	@Override
	public void serviceApi(ApiServletRequest req, ApiServletResponse resp) {

		try {
			Api api = getClass().getAnnotation(Api.class);

			String origin = req.getHeader("Origin");
			if (origin != null) {
				boolean authorized = false;
				try {
					URI uriOrigin = new URI(origin);
					authorized = Settings.LANGS_DOMAINS.containsValue(uriOrigin.getHost()) && uriOrigin.getScheme().equals(Settings.HTTP_PROTO.replace("://", ""));
				} catch (Exception ignore) {
				}
				if (authorized) {
					resp.setHeader("Access-Control-Allow-Origin", origin);
					resp.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
					resp.setHeader("Access-Control-Allow-Credentials", "true");
					resp.setHeader("Access-Control-Allow-Headers", "X-Requested-With, Content-Type");
					resp.setIntHeader("Access-Control-Max-Age", Settings.COOKIE_DELAY);
					resp.setHeader("Vary", "Origin");
				}

				if (req.getMethod().equalsIgnoreCase("OPTIONS")) {
					resp.setHeaderMaxCache();
					resp.getWriter().write("");
					return;
				}
				if (!authorized) {
					resp.setStatus(401);
					resp.getWriter().write("hacked ?");
					return;
				}
			}


			Users user;

			//User use an OAuth procedure
			if (api != null && req.getHeader("Authorization") != null) {

				String access_token = req.getHeader("Authorization").replaceFirst(Pattern.compile("^Bearer ", Pattern.CASE_INSENSITIVE).pattern(), "");

				Json userdb = Db.aggregate("ApiAccess",
						Arrays.asList(
								Aggregates.match(Filters.eq("access_token", access_token)),
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
								Aggregates.lookup("Teams", "teams", "_id", "teams")
						)

				).first();

				if (userdb != null && userdb.get("app_id") != null &&
						(userdb.getDate("expire").after(new Date()) || userdb.getDate("expire").equals(new Date()))
				) {
					List<String> app_scopes = userdb.getList("app_scopes");
					List<String> scopes = userdb.getList("scopes");
					List<String> real_scopes = new ArrayList<>();
					if (scopes != null) {
						scopes.forEach(scope -> {
							if (app_scopes.contains(scope)) {
								real_scopes.add(scope);
							}
						});
					}
					userdb.remove("app_scopes").put("scopes", real_scopes);
					user = new Users(userdb);

					Db.updateOne("ApiAccess", Filters.eq("_id", userdb.getString("access")), new Json("$set", new Json("access", new Date())).put("$inc", new Json("count", 1)));

					if (!api.scope().equals("") && !user.scope(api.scope())) {
						resp.setStatus(401);
						resp.getWriter().write(new Json("error", "AUTHORIZATION_SCOPE_ERROR").toString());
						return;
					}

				} else if (userdb != null && userdb.getDate("expire").before(new Date())) {

					resp.setStatus(401);
					resp.getWriter().write(new Json("error", "EXPIRED_ACCESS_TOKEN").toString());
					return;

				} else if (userdb != null && userdb.get("app_id") == null) {

					resp.setStatus(401);
					resp.getWriter().write(new Json("error", "APP_NOT_FOUND").toString());
					return;

				} else {
					resp.setStatus(401);
					BruteLocker.add(ServletUtils.realIp(req));
					resp.getWriter().write(new Json("error", "INVALID_ACCESS_TOKEN").toString());
					return;
				}


			} else {
				//User use a cookie procedure
				user = BaseSession.getOrCreateUser(req, resp);
				if (user == null && BaseCookie.getAuth(req) != null && !BaseSession.sessionExists(req)) {
					BruteLocker.add(ServletUtils.realIp(req));
				}
			}

			if (req.getMethod().equalsIgnoreCase("POST")) {

				Json data = null;
				String contentType = req.getHeader("Content-Type");


				if (contentType == null) {
					resp.setStatus(500);
					resp.sendResponse(new Json("error", "NO_CONTENT_TYPE"));
					return;
				}

				if (contentType.matches("application/json;?.*")) {
					ServletInputStream payload = req.getInputStream();
					try {
						data = new Json(IOUtils.toString(payload));
					} catch (Exception ignore) {
					} finally {
						try {
							payload.close();
						} catch (Exception ignore) {
						}
					}
					if (data == null) {
						resp.setStatus(500);
						resp.sendResponse(new Json("error", "INVALID_PAYLOAD"));
						return;
					}
				} else if (contentType.equalsIgnoreCase("application/x-www-form-urlencoded")) {
					data = new Json(req.getParameter("data"));
				} else {
					resp.setStatus(500);
					resp.sendResponse(new Json("error", "INVALID_CONTENT_TYPE"));
					return;
				}


				if (user == null) {
					doPostApiPublic(req, resp, data);
				} else if (user.getAdmin()) {
					doPostApiEditor(req, resp, data, user);
				} else {
					doPostApiAuth(req, resp, data, user);
				}

			} else if (user == null) {
				doGetApiPublic(req, resp);
			} else if (user.getEditor()) {
				doGetApiEditor(req, resp, user);
			} else {
				doGetApiAuth(req, resp, user);
			}


		} catch (Exception ex) {
			ex.printStackTrace();
			Fx.log(ex.getMessage());
			try {
				resp.setStatus(500);
				resp.getWriter().write(new Json("error", "UNKNOWN").toString());
			} catch (Exception e) {
				Fx.log(e.getMessage());
			}

		}
	}

	/**
	 * Do Get request on API with no authentication
	 */
	public void doGetApiPublic(ApiServletRequest req, ApiServletResponse resp) throws IOException, ServletException {
		resp.setStatus(500);
		resp.sendResponse(new Json("error", "METHOD_NOT_FOUND"));
	}

	/**
	 * Do Get request on API with authenticated users
	 */
	public void doGetApiAuth(ApiServletRequest req, ApiServletResponse resp, Users user) throws IOException, ServletException {
		doGetApiPublic(req, resp);
	}

	/**
	 * Do Get request on API editor group privilege
	 */
	public void doGetApiEditor(ApiServletRequest req, ApiServletResponse resp, Users user) throws IOException, ServletException {
		doGetApiAuth(req, resp, user);
	}

	/**
	 * Do Post request on API with no authentication
	 */
	public void doPostApiPublic(ApiServletRequest req, ApiServletResponse resp, Json data) throws IOException, ServletException {
		resp.setStatus(500);
		resp.sendResponse(new Json("error", "METHOD_NOT_FOUND"));
	}

	/**
	 * Do Post request on API with authenticated users
	 */
	public void doPostApiAuth(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException, ServletException {
		doPostApiPublic(req, resp, data);
	}

	/**
	 * Do Post request on API editor group privilege
	 */
	public void doPostApiEditor(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException, ServletException {
		doPostApiAuth(req, resp, data, user);
	}

	/**
	 * Go to Web service when does not match API service
	 */
	@Override
	public void serviceWeb(WebServletRequest req, WebServletResponse resp) {
		try {
			resp.sendError(404, "not found");
		} catch (Exception e) {
		}
	}


	@Override
	public void destroy() {
	}
}

