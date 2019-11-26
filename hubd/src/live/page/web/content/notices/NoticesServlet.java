/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.content.notices;

import live.page.web.content.notices.push.PushSubscriptions;
import live.page.web.system.Language;
import live.page.web.system.json.Json;
import live.page.web.system.servlet.HttpServlet;
import live.page.web.system.servlet.utils.Api;
import live.page.web.system.servlet.wrapper.ApiServletRequest;
import live.page.web.system.servlet.wrapper.ApiServletResponse;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.system.sessions.Users;

import javax.servlet.annotation.WebServlet;
import java.io.IOException;

@Api(scope = "user")
@WebServlet(name = "Notices", urlPatterns = {"/notices", "/notices/*"})
public class NoticesServlet extends HttpServlet {

	@Override
	public void doGetPublic(WebServletRequest req, WebServletResponse resp) throws IOException {
		doGetAuth(req, resp, null);
	}

	@Override
	public void doGetAuth(WebServletRequest req, WebServletResponse resp, Users user) throws IOException {

		Json notice = NoticesUtils.readClick(req.getId());
		if (notice == null) {
			resp.sendError(404, Language.get("UNBKNOWN", req.getLng()));
			return;
		}
		resp.sendRedirect(notice.getString("url"), 301);


	}

	@Override
	public void doGetApiAuth(ApiServletRequest req, ApiServletResponse resp, Users user) throws IOException {

		resp.sendResponse(NoticesUtils.getNotices(user, req.getString("paging", null)));
	}

	@Override
	public void doPostApiPublic(ApiServletRequest req, ApiServletResponse resp, Json data) throws IOException {

		Json rez = new Json("error", "INVALID_DATA");

		switch (data.getString("action", "")) {
			case "get":
				rez = PushSubscriptions.listConfigFollows(data.getJson("config"), data.getString("paging", null));
				break;
			case "subscribe":
				rez = PushSubscriptions.subscribe(null, data.getString("lng"), data.getJson("device"), data.getJson("config"), data.getString("obj"));
				break;
			case "unsubscribe":
				rez = PushSubscriptions.unsubscribe(data.getJson("config"), data.getString("obj"));
				break;
			case "control":
				rez = PushSubscriptions.control(data.getJson("config"), data.getString("obj"));
				break;
			case "unpush":
				rez = PushSubscriptions.remove(data.getId(), data.getJson("config"), null);
				break;
			case "test":
				rez = PushSubscriptions.test(data.getJson("config"), data.getString("lng"));
				break;
		}
		resp.sendResponse(rez);
	}

	@Override
	public void doPostApiAuth(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException {

		Json rez;
		switch (data.getString("action", "")) {
			case "read":
				rez = NoticesUtils.read(user.getId(), data);
				break;
			case "remove":
				rez = NoticesUtils.remove(user.getId(), data);
				break;
			case "subscribe":
				rez = PushSubscriptions.subscribe(user.getId(), data.getString("lng"), data.getJson("device"), data.getJson("config"), data.getString("obj"));
				break;
			case "unpush":
				rez = PushSubscriptions.remove(data.getId(), data.getJson("config"), user);
				break;
			default:
				doPostApiPublic(req, resp, data);
				return;
		}

		resp.sendResponse(rez);

	}
}
