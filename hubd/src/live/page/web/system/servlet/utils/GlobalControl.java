/*
 * Copyright (c) 2019. PAGE and Sons
 */
package live.page.web.system.servlet.utils;

import live.page.web.system.servlet.HttpServlet;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.system.sessions.Users;

import javax.servlet.ServletException;
import java.io.IOException;

public class GlobalControl extends HttpServlet {
	@Override
	public boolean controlWeb(Users user, WebServletRequest req, WebServletResponse resp) throws ServletException, IOException {
		req.setAttribute("tos", TosTester.isNotSeen(user, req.getLng()));
		return true;
	}

}
