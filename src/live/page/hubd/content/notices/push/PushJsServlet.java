/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.content.notices.push;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import live.page.hubd.system.cosmetic.compress.Compressors;
import live.page.hubd.system.servlet.LightServlet;
import live.page.hubd.system.servlet.wrapper.BaseServletRequest;
import live.page.hubd.system.servlet.wrapper.BaseServletResponse;
import live.page.hubd.system.servlet.wrapper.WebServletResponse;
import live.page.hubd.utils.Fx;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Objects;

@WebServlet(asyncSupported = true, name = "PushJsServlet", urlPatterns = {"/push.js"})
public class PushJsServlet extends LightServlet {
    private static String push = null;
    private static byte[] pushGZip = null;
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
            assert sr != null;
            resp.getWriter().write(IOUtils.toString(sr, StandardCharsets.UTF_8.displayName()));
            sr.close();
            return;
        }

        resp.setHeaderMaxCache();

        if (pushGZip != null && accept != null && accept.contains("gzip")) {
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
            if (sr != null) {
                push = IOUtils.toString(sr, StandardCharsets.UTF_8.displayName());
                sr.close();
                pushDate = new Date(Objects.requireNonNull(PushJsServlet.class.getResource("/res/push.js"))
                        .openConnection().getLastModified());
                pushGZip = Compressors.gzipCompressor(push.getBytes());
            }
        } catch (Exception ignore) {

        }
    }

}
