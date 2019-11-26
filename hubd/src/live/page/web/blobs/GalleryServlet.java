/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.blobs;

import live.page.web.system.json.Json;
import live.page.web.system.servlet.HttpServlet;
import live.page.web.system.servlet.utils.Api;
import live.page.web.system.servlet.wrapper.ApiServletRequest;
import live.page.web.system.servlet.wrapper.ApiServletResponse;
import live.page.web.system.sessions.Users;

import javax.servlet.annotation.WebServlet;
import java.io.IOException;

@Api(scope = "files")
@WebServlet(name = "Gallery Servlet", urlPatterns = {"/gallery"})
public class GalleryServlet extends HttpServlet {

	@Override
	public void doGetApiAuth(ApiServletRequest req, ApiServletResponse resp, Users user) throws IOException {

		resp.sendResponse(BlobsDb.getFiles(user.getId(), req.getString("paging", null)));
	}

	@Override
	public void doPostApiAuth(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException {

		Json rez = new Json("error", "INVALID_DATA");
		switch (data.getString("action", "")) {

			case "text":
				rez = BlobsDb.updateText(data, user);
				break;

		}

		resp.sendResponse(rez);
	}

}
