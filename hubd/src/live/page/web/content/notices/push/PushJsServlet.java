/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.content.notices.push;

import live.page.web.system.cosmetic.Compressors;
import live.page.web.system.servlet.BaseServlet;
import live.page.web.system.servlet.wrapper.BaseServletRequest;
import live.page.web.system.servlet.wrapper.BaseServletResponse;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.utils.Fx;
import org.apache.commons.fileupload.util.Streams;

import javax.servlet.ServletConfig;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@WebServlet(name = "PushJsServlet", urlPatterns = {"/push.js"})
public class PushJsServlet extends BaseServlet {
	private static String push = null;
	private static byte[] pushGZip = null;
	private static byte[] pushBRZip = null;
	private static Date pushDate = new Date();

	@Override
	public void doService(BaseServletRequest req, BaseServletResponse resp) throws IOException {

		resp.setContentType("application/javascript; charset=utf-8");
		resp.setHeader("Access-Control-Allow-Origin", "*");
		resp.setHeader("X-Robots-Tag", "noindex");
		resp.setDateHeader("Last-Modified", pushDate.getTime());
		WebServletResponse.setHeaderMaxCache(resp);

		String accept = req.getHeader("accept-encoding");

		if (Fx.IS_DEBUG) {
			resp.setHeaderNoCache();
			InputStream sr = PushJsServlet.class.getResourceAsStream("/res/push.js");
			resp.getWriter().write(Streams.asString(sr, StandardCharsets.UTF_8.displayName()));
			sr.close();
			return;
		}

		resp.setHeaderMaxCache();

		if (pushBRZip != null && accept != null && accept.contains("br")) {
			resp.setContentLength(pushBRZip.length);
			resp.setHeader("Content-Encoding", "br");
			resp.getOutputStream().write(pushBRZip);

		} else if (pushGZip != null && accept != null && accept.contains("gzip")) {
			resp.setContentLength(pushGZip.length);
			resp.setHeader("Content-Encoding", "gzip");
			resp.getOutputStream().write(pushGZip);

		} else {
			resp.getWriter().write(push);
		}
	}

	@Override
	public void init(ServletConfig config) {
		try {
			InputStream sr = PushJsServlet.class.getResourceAsStream("/res/push.js");
			push = Streams.asString(sr, StandardCharsets.UTF_8.displayName());
			sr.close();
			pushDate = new Date(PushJsServlet.class.getResource("/res/push.js").openConnection().getLastModified());
			pushGZip = Compressors.gzipCompressor(push.getBytes());
			pushBRZip = Compressors.brotliCompressor(push.getBytes());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
