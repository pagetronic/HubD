/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.blobs;

import live.page.web.system.servlet.BaseServlet;
import live.page.web.system.servlet.wrapper.BaseServletRequest;
import live.page.web.system.servlet.wrapper.BaseServletResponse;

import javax.servlet.annotation.WebServlet;
import java.io.IOException;


/**
 * CDN/Image/File servlet
 */
@WebServlet(urlPatterns = {"/files/*"})
public class FilesServlet extends BaseServlet {

	@Override
	public void doService(BaseServletRequest req, BaseServletResponse resp) throws IOException {

		try {
			resp.setHeaderMaxCache();
			if (!req.getRequestURI().matches("^/files/(.*)@?([0-9]{1,4})?(x[0-9]{1,4})?(\\.jpg|\\.png)?$")) {
				resp.sendTextError(404, "Not found");
				return;
			}

			if (req.getRequestURI().matches(".*@([0-9]{1,4})(x[0-9]{1,4})?(\\.jpg|\\.png)?$")) {
				BlobsService.doGetThumb(req, resp);
			} else {
				BlobsService.doGetBlob(req, resp);
			}
		} catch (Exception e) {
			resp.sendTextError(500, "error");
		}
	}

}

