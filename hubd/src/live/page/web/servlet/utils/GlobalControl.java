/*
 * Copyright (c) 2019. PAGE and Sons
 */
package live.page.web.servlet.utils;

import live.page.web.servlet.HttpServlet;
import live.page.web.servlet.wrapper.WebServletRequest;
import live.page.web.servlet.wrapper.WebServletResponse;
import live.page.web.session.Users;

import javax.servlet.ServletException;
import java.io.IOException;

public class GlobalControl extends HttpServlet {
	@Override
	public boolean controlWeb(Users user, WebServletRequest req, WebServletResponse resp) throws ServletException, IOException {
		req.setAttribute("tos", TosTester.isNotSeen(user, req.getLng()));
		return true;
	}

}
