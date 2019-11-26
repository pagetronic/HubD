/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.system.cosmetic.svg;

import live.page.web.system.servlet.HttpServlet;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.utils.Fx;

import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;

/**
 * Servlet for choose a SVG in the possibles
 */
@WebServlet(name = "SVGIcons preview", urlPatterns = {"/icons"})
public class SVGListServlet extends HttpServlet {

	@Override
	public void doGetPublic(WebServletRequest req, WebServletResponse resp) throws IOException {
		if (!Fx.IS_DEBUG) {
			resp.sendError(404, "Not found");
			return;
		}

		req.setTitle("Icons");
		req.setAttribute("admin_active", "icons");
		PrintWriter wrt = resp.getWriter();
		Iterator<Map.Entry<String, String>> svgsmap = SVGParser.getSvgs().entrySet().iterator();
		resp.setContentType("text/html; charset=utf-8");
		wrt.write("<!DOCTYPE html><html><head><title>SVG</title>");
		wrt.write("<style type=\"text/css\">body{zoom:5;color:#FFF;text-align:center;background:#000}textarea{position:fixed;top:0px;right:0px}" +
				"svg{fill:#FFFFFF;height:30px;width:40px}" +
				"span{display:inline-block;cursor:pointer;font-size:3px}" +
				"span:active{opacity:0.5}</style>");
		wrt.write("</head><body>");
		while (svgsmap.hasNext()) {
			Map.Entry<String, String> svgmap = svgsmap.next();
			String size = SVGParser.getSizes().get(svgmap.getKey());
			wrt.write("<span name=\"" + svgmap.getKey() + "\"><svg style=\"width:" + size.split(" ")[2] + "px;height:" + size.split(" ")[3] + "px\" version=\"1.1\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" viewBow=\"" + size + "\">" + svgmap.getValue().replace("<path", "<path fill=\"#FFF\" ") + "</svg><br/>" + svgmap.getKey() + "</span>\n");
		}
		wrt.write("<script type=\"text/javascript\">var spans=document.getElementsByTagName('span');for(var i=0;i<spans.length;i++){spans[i].onclick=function(){" +
				"var target=document.createElement('textarea');target.style.height=1;target.style.width=1;target.value=this.getAttribute('name');document.body.append(target);" +
				"target.focus();target.setSelectionRange(0,target.value.length);document.execCommand('copy');target.remove();}}</script>");
		wrt.write("</body></html>");
		wrt.close();
	}

}
