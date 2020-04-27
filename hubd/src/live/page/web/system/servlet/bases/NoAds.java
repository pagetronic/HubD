/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.system.servlet.bases;

import live.page.web.system.servlet.BaseServlet;
import live.page.web.system.servlet.wrapper.BaseServletRequest;
import live.page.web.system.servlet.wrapper.BaseServletResponse;
import live.page.web.system.servlet.wrapper.WebServletResponse;

import javax.servlet.annotation.WebServlet;
import java.io.IOException;

@WebServlet(urlPatterns = {"/noads"}, displayName = "noads")
public class NoAds extends BaseServlet {
	@Override
	public void doService(BaseServletRequest req, BaseServletResponse resp) throws IOException {
		WebServletResponse.setHeaderMaxCache(resp);
		resp.getWriter().write("<!DOCTYPE html>\n" +
				"<html>\n" +
				"<head>\n" +
				"\t<meta name=\"robots\" content=\"noindex, nofollow, noarchive\"/>\n" +
				"\t<meta charset=\"UTF-8\"/>\n" +
				"\t<title></title></head>\n" +
				"<body>\n" +
				"<script type=\"text/javascript\">try {\n" +
				"    var par = window;\n" +
				"    while (par.parent != par) {\n" +
				"        par = par.parent;\n" +
				"    }\n" +
				"    var ele = par.document.getElementById('" + (req.getQueryString() != null ? req.getQueryString().replace("?", "") : "ggads") + "');\n" +
				"    ele.parentNode.removeChild(ele);\n" +
				"} catch (e) {\n" +
				"\n" +
				"}\n" +
				"</script>\n" +
				"</body>\n" +
				"</html>");
	}
}
