/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.blobs;

import jakarta.servlet.annotation.WebServlet;
import live.page.hubd.system.servlet.LightServlet;
import live.page.hubd.system.servlet.wrapper.BaseServletRequest;
import live.page.hubd.system.servlet.wrapper.BaseServletResponse;

import java.io.IOException;


/**
 * CDN/Image/File servlet
 */
@WebServlet(asyncSupported = true, urlPatterns = {"/files/*"})
public class FilesServlet extends LightServlet {

    @Override
    public void doService(BaseServletRequest req, BaseServletResponse resp) throws IOException {

        try {
            resp.setHeaderMaxCache();
            if (!req.getRequestURI().matches("^/files/(.*)@?([0-9]{1,4})?(x[0-9]{1,4})?(\\.jpg|\\.png|\\.webp)?$")) {
                resp.sendTextError(404, "Not found");
                return;
            }

            if (req.getRequestURI().matches(".*@([0-9]{1,4})(x[0-9]{1,4})?(\\.jpg|\\.png|\\.webp)?$")) {
                BlobsService.doGetThumb(req, resp);
            }

            if (req.getRequestURI().matches("^/files/([0-9A-Z]+).(\\.jpg|\\.png|\\.webp)$")) {
                BlobsService.doGetConvert(req, resp);
                return;
            }
            BlobsService.doGetBlob(req, resp);

        } catch (Exception e) {
            resp.sendTextError(500, "error");
        }
    }

}

