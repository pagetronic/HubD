package live.page.hubd.system.cosmetic.svg;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import live.page.hubd.system.Settings;
import live.page.hubd.system.cosmetic.compress.Compressors;
import live.page.hubd.system.servlet.LightServlet;
import live.page.hubd.system.servlet.wrapper.BaseServletRequest;
import live.page.hubd.system.servlet.wrapper.BaseServletResponse;
import live.page.hubd.system.servlet.wrapper.WebServletResponse;
import live.page.hubd.system.utils.FilesRepos;
import live.page.hubd.system.utils.Fx;
import live.page.hubd.system.utils.Hidder;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The type Svg servlet.
 */
@WebServlet(name = "SVG Source Servlet", urlPatterns = {"/svg/*"}, loadOnStartup = 1)
public class SVGServlet extends LightServlet {

    /**
     * The constant sizes.
     */
    static final Map<String, SVGParser.SVGData> svgs = new HashMap<>();
    /**
     * The Svg src.
     */
    private static byte[] svgSrc = null;
    /**
     * The Svg g zip.
     */
    private static byte[] svgGZip = null;
    /**
     * The Svg g zip.
     */
    private static byte[] svgBr = null;
    /**
     * The constant svgSrcDate.
     */
    private static Date svgSrcDate = new Date(0);

    /**
     * Gets name.
     *
     * @return the name
     */
    public static String getName() {
        return "/svg/" + Hidder.encodeDate(svgSrcDate) + ".svg";
    }

    /**
     * Build or rebuilt SVG file
     */
    public static void build() {
        svgSrc = buildSvg();
        svgGZip = Compressors.gzipCompressor(svgSrc);
        svgBr = Compressors.brCompressor(svgSrc);
    }

    /**
     * Build or rebuilt SVG and get Byte for compression
     *
     * @return the byte [ ]
     */
    private static byte[] buildSvg() {
        try {
            svgs.clear();

            Map<String, SVGParser.SVGData> allsSvgs = SVGParser.getAllSvgs();


            for (File file : getFilesToScan()) {
                String str = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                for (String pattern : Arrays.asList("\\$\\{?svg\\.([0-9a-z_\\-]+)}?", SVGTemplate.class.getSimpleName() + ".get\\(\"([0-9a-z_\\-]+)\"\\)")) {
                    Matcher mfile = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(str);

                    while (mfile.find()) {
                        String id = mfile.group(1);
                        if (allsSvgs.containsKey(id) && !svgs.containsKey(id)) {
                            svgs.put(mfile.group(1), allsSvgs.get(mfile.group(1)));
                        }
                        Date lastmode = new Date(file.lastModified());
                        if (svgSrcDate == null || lastmode.after(svgSrcDate)) {
                            svgSrcDate = lastmode;
                        }
                    }
                }
            }

            StringBuilder src_build = new StringBuilder();
            src_build.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
            src_build.append("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">\n");
            src_build.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100%\" height=\"100%\" viewBox=\"0 0 0 0\" style=\"background:#EEE url(")
                    .append(Settings.getLogo()).append("@512x512) 50% 40% no-repeat;background-size:auto 30%\">");

            src_build.append("\n\n<!--\n" +
                    " * Copyright 2016 Google Inc. All Rights Reserved.\n" +
                    " * Material Design Icons https://material.io\n" +
                    " * License: Apache License Version 2.0 http://www.apache.org/licenses/LICENSE-2.0\n" +
                    "-->\n");
            Collection<String> keys = svgs.keySet();
            for (String key : keys) {
                if (key.startsWith("mi_") && svgs.containsKey(key)) {
                    src_build.append(svgs.get(key).src);
                }
            }


            src_build.append("\n\n<!--\n" +
                    " * Copyright Mixed, please see sources\n" +
                    "-->\n");

            for (String key : keys) {
                if (!key.startsWith("mi_") && svgs.containsKey(key)) {
                    src_build.append(svgs.get(key).src);
                }
            }

            src_build.append("</svg>");

            return src_build.toString().getBytes(StandardCharsets.UTF_8);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    /**
     * Periodical verifications of change
     */
    public static void control() {
        Date current = new Date(0);
        for (File file : getFilesToScan()) {
            try {
                String str = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                Matcher mfile = Pattern.compile("\\$\\{?svg\\.([0-9a-z_\\-]+)}?", Pattern.CASE_INSENSITIVE).matcher(str);

                if (mfile.find(0)) {
                    Date lastmode = new Date(file.lastModified());
                    if (lastmode.after(current)) {
                        current = lastmode;
                    }
                }
            } catch (Exception ignore) {
            }
        }
        if (current.getTime() > svgSrcDate.getTime()) {
            long start = System.currentTimeMillis();
            build();
            Fx.log("SVG rebuild in " + (System.currentTimeMillis() - start) + "ms");
        }
    }

    /**
     * Get list of file to scan for SVG use
     *
     * @return the files to scan
     */
    private static List<File> getFilesToScan() {
        List<File> files = new ArrayList<>();
        files.addAll(FilesRepos.listFiles("libs", "js"));
        files.addAll(FilesRepos.listFiles("html", "js", "html"));
        files.addAll(FilesRepos.listFiles("src", "java"));
        return files;
    }

    @Override
    public void doService(BaseServletRequest req, BaseServletResponse resp) throws IOException, ServletException {

        if (!req.getRequestURI().equals(getName())) {
            resp.sendRedirect(getName(), 301);
            return;
        }

        ServletOutputStream out = resp.getOutputStream();

        WebServletResponse.setHeaderMaxCache(resp);
        resp.setHeader("Vary", "Accept-Encoding");
        resp.setContentType("image/svg+xml;charset=utf-8");

        if (Fx.IS_DEBUG) {
            out.write(buildSvg());
            return;
        }

        resp.setDateHeader("Last-Modified", svgSrcDate.getTime());


        String acceptEncoding = req.getHeader("accept-encoding");
        List<String> accept = new ArrayList<>();
        if (acceptEncoding != null) {
            accept = Arrays.asList(acceptEncoding.split("([ ,]+)"));
        }

        if (acceptEncoding.contains("br") && svgBr != null) {
            resp.setContentLength(svgBr.length);
            resp.setHeader("Content-Encoding", "br");
            out.write(svgBr);
        } else if (accept.contains("gzip") && svgGZip != null) {
            resp.setContentLength(svgGZip.length);
            resp.setHeader("Content-Encoding", "gzip");
            out.write(svgGZip);
        } else {
            resp.setContentLength(svgSrc.length);
            out.write(svgSrc);

        }
    }

    /**
     * Build SVG on init
     */
    @Override
    public void init(ServletConfig config) {
        build();
        super.init(config);
    }

}
