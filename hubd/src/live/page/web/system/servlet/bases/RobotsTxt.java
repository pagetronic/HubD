/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.system.servlet.bases;

import live.page.web.system.Settings;
import live.page.web.system.servlet.BaseServlet;
import live.page.web.system.servlet.wrapper.BaseServletRequest;
import live.page.web.system.servlet.wrapper.BaseServletResponse;
import live.page.web.system.servlet.wrapper.WebServletResponse;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;

@WebServlet(urlPatterns = {"/robots.txt"})
public class RobotsTxt extends BaseServlet {


	@Override
	public void doService(BaseServletRequest req, BaseServletResponse resp) throws IOException, ServletException {

		resp.setContentType("text/plain");
		WebServletResponse.setHeaderMaxCache(resp);

		if (req.getServerName().equals(Settings.HOST_CDN) || req.getServerName().equals(Settings.HOST_API)) {

			resp.getWriter().write("User-agent: *\n" + "Allow: /\n\n" + "User-agent: ia_archiver\n" + "Disallow: /");

		} else if (Settings.LANGS_DOMAINS.containsValue(req.getServerName())) {

			req.getServletContext().getNamedDispatcher("default").forward(req, resp);

		} else if (req.getServerName().equals(Settings.STANDARD_HOST)) {

			resp.getWriter().write("User-agent: *\n" + "Allow: /\n\n" + "User-agent: ia_archiver\n" + "Disallow: /");

		} else {
			resp.getWriter().write("User-agent: *\n" + "Disallow: /\n\n");
		}

	}
}
