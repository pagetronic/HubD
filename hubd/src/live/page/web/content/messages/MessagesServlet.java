/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.content.messages;

import live.page.web.system.Language;
import live.page.web.system.json.Json;
import live.page.web.system.servlet.HttpServlet;
import live.page.web.system.servlet.utils.Api;
import live.page.web.system.servlet.wrapper.ApiServletRequest;
import live.page.web.system.servlet.wrapper.ApiServletResponse;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.system.sessions.Users;
import live.page.web.utils.Fx;

import javax.servlet.annotation.WebServlet;
import java.io.IOException;

@Api(scope = "pm")
@WebServlet(name = "Messages", urlPatterns = {"/messages", "/messages/*"})
public class MessagesServlet extends HttpServlet {

	@Override
	public void doGetAuth(WebServletRequest req, WebServletResponse resp, Users user) throws IOException {

		req.setCanonical("/messages", "sort", "paging", "archive");

		req.setAttribute("active", "messages");
		req.setTitle(Fx.ucfirst(Language.get("MESSAGES", req.getLng())));
		req.setAttribute("messages", MessagesUtils.getMessages(user, req.getString("paging", null), req.getString("sort", null), 40, req.getParameter("archive") != null));
		resp.sendTemplate(req, "/messages/messages.html");
	}

	@Override
	public void doGetApiAuth(ApiServletRequest req, ApiServletResponse resp, Users user) throws IOException {


		if (req.getId() != null) {
			resp.sendResponse(MessagesUtils.getMessage(user, req.getId(), req.getString("paging", null)));
			return;
		}
		resp.sendResponse(MessagesUtils.getMessages(user, req.getString("paging", null), req.getString("sort", null), 20, req.getParameter("archive") != null));
	}

	@Override
	public void doPostApiAuth(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException {


		Json rez = new Json("error", "INVALID_DATA");
		switch (data.getString("action", "")) {
			case "send":
				rez = MessagesUtils.sendMessage(data.getString("id", null), user, data.getText("message", null), data.getString("file", null), data.getString("subject", null), data.getList("recipients"));
				break;
		}

		resp.sendResponse(rez);


	}

}
