package live.page.hubd.system.cosmetic.svg;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import live.page.hubd.system.servlet.LightServlet;
import live.page.hubd.system.servlet.wrapper.BaseServletRequest;
import live.page.hubd.system.servlet.wrapper.BaseServletResponse;
import live.page.hubd.system.utils.Fx;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Servlet for choose a SVG in the possibles
 */
@WebServlet(name = "SVGIcons preview", urlPatterns = {"/icons"})
public class SVGListServlet extends LightServlet {

    @Override
    public void doService(BaseServletRequest req, BaseServletResponse resp) throws IOException, ServletException {
        if (!Fx.IS_DEBUG) {
            resp.sendError(404, "Not found");
            return;
        }

        req.setAttribute("admin_active", "icons");
        PrintWriter wrt = resp.getWriter();
        Map<String, SVGParser.SVGData> svgs = SVGParser.getAllSvgs();
        List<String> list = new ArrayList<>(svgs.keySet());
        Collections.sort(list);
        Collections.reverse(list);
        resp.setContentType("text/html; charset=utf-8");
        wrt.write("<!DOCTYPE html><html><head><title>SVG</title>");
        wrt.write("<style type=\"text/css\">body{color:#000;text-align:center;background:#FFF}textarea{position:fixed;top:0px;right:0px}" +
                "svg{fill:#000;height:30px;width:40px}" +
                "span{display:inline-block;cursor:pointer;font-size:13px}" +
                "span:active{opacity:0.5}</style>");
        wrt.write("</head><body>");
        String type = null;
        for (String id : list) {
            if (type != null && !id.split("_", 2)[0].equals(type)) {
                wrt.write("<hr/>");
            }
            type = id.split("_", 2)[0];
            SVGParser.SVGData svgData = svgs.get(id);
            int big_height = 92;
            float width = Float.parseFloat(svgData.size.split(" ")[2]);
            float height = Float.parseFloat(svgData.size.split(" ")[3]);
            int big_width = big_height * Math.round(width / height);

            wrt.write("<span name=\"" + id + "\"><svg style=\"width:" + big_width + "px;height:" + big_height + "px\" version=\"1.1\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" viewBox=\"" + svgData.size + "\">" + svgData.src + "</svg><br/>" + id + "</span>\n");

        }
        wrt.write("<script type=\"text/javascript\">var spans=document.getElementsByTagName('span');for(var i=0;i<spans.length;i++){spans[i].onclick=function(){" +
                "let target=document.createElement('textarea');target.style.height=1;target.style.width=1;target.value=this.getAttribute('name');document.body.append(target);" +
                "target.focus();target.setSelectionRange(0,target.value.length);document.execCommand('copy');target.remove();}}</script>");
        wrt.write("</body></html>");
        wrt.close();
    }


}
