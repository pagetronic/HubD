/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.content.notices.push;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import live.page.hubd.system.Language;
import live.page.hubd.system.Settings;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.servlet.HttpServlet;
import live.page.hubd.system.servlet.wrapper.WebServletRequest;
import live.page.hubd.system.servlet.wrapper.WebServletResponse;
import live.page.hubd.system.sessions.Users;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * the manifest json used for Webpush and future progressive web app from Google
 */
@WebServlet(asyncSupported = true, urlPatterns = {"/manifest.json"})
public class ManifestServlet extends HttpServlet {

    @Override
    public void doGetHttp(WebServletRequest req, WebServletResponse resp, Users user) throws IOException, ServletException {

        resp.setHeaderMaxCache();
        List<Json> icons = new ArrayList<>();

        for (int size : new int[]{16, 22, 24, 32, 33, 36, 48, 64, 72, 96, 144, 160, 192, 240, 320, 480, 640}) {
            icons.add(new Json().put("src", Settings.getLogo() + "@" + size + "x" + size).put("sizes", size + "x" + size).put("type", "image/png"));
        }
        resp.sendJson(new Json()
                .put("manifest_version", 2)
                .put("name", Settings.SITE_TITLE)
                .put("short_name", Settings.SITE_TITLE)
                .put("description", Language.exist("SITE_DESCRIPTION", req.getLng()) ? Language.get("SITE_DESCRIPTION", req.getLng()) : null)
                .put("homepage_url", Settings.getFullHttp(req.getLng()))
                .put("start_url", Settings.getFullHttp(req.getLng()) + "/")
                .put("author", "Tronic Page")
                .put("version", "1.1")
                .put("display", "standalone")
                .put("background_color", "#FFFFFF")
                .put("theme_color", "#6E5C40")
                .put("gcm_sender_id", Settings.GCM_SENDER_ID)
                .put("permissions", new String[]{"pushMessaging", "Notices"})
                .put("icons", icons));

    }
}
