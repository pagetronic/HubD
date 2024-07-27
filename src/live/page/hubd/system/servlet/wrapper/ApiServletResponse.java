/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.servlet.wrapper;

import jakarta.servlet.ServletResponse;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.json.XMLJsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ApiServletResponse extends BaseServletResponse {

    private final long start = System.currentTimeMillis();
    private final boolean xml;

    public ApiServletResponse(ServletResponse response, boolean xml) throws IOException {

        super(response);
        this.xml = xml;
        setCharacterEncoding(StandardCharsets.UTF_8.displayName());

        setContentType(xml ? "application/xml; charset=utf-8" : "application/json; charset=utf-8");

        setHeader("Vary", "Authorization,Cookie,Origin");
        setHeaderNoCache();
        setHeader("X-Robots-Tag", "noindex");

    }

    public void sendResponse(Object obj) throws IOException {

        setHeader("X-Speed", (System.currentTimeMillis() - start) + "ms");
        if (obj == null) {
            setStatus(404);
            obj = new Json("error", "NOT_FOUND");
        }
        if (xml) {
            getWriter().write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>" + "<api>" + XMLJsonParser.toXML(obj) + "</api>");
        } else {
            getWriter().write(XMLJsonParser.toJSON(obj));
        }

    }

    public void sendError(String msg) throws IOException {
        sendError(500, msg);
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        setStatus(sc);
        sendResponse(new Json("error", msg));
    }

    @Override
    public void sendError(int sc) throws IOException {
        setStatus(sc);
        sendResponse(new Json("error", "ERROR_UNKNOWN"));
    }
}
