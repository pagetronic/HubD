/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.system.servlet.bases;

import live.page.web.system.Settings;
import live.page.web.system.servlet.HttpServlet;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;

@WebServlet(urlPatterns = {"/error"}, name = "error-page")
public class ErrorServlet extends HttpServlet {

	@Override
	public void doGetPublic(WebServletRequest req, WebServletResponse resp) throws IOException, ServletException {
		resp.setHeaderNoCache();
		req.setCanonical(null);


		if ((Settings.LANGS_DOMAINS.size() > 1 && req.getServerName().equals(Settings.STANDARD_HOST)) || !Settings.LANGS_DOMAINS.containsValue(req.getServerName())) {
			// redirect to standard Host
			req.getRequestDispatcher("/STANDARD_HOST").forward(req, resp);

		} else if (req.getServerName().equals(Settings.STANDARD_HOST) || Settings.LANGS_DOMAINS.containsValue(req.getServerName())) {

			req.setAttribute("error", "Error " + resp.getStatus());
			req.setAttribute("message", req.getAttribute("javax.servlet.error.message"));
			resp.sendTemplate(req, "/error.html");

		} else {
			resp.sendText("Error " + resp.getStatus());
		}

	}

}
