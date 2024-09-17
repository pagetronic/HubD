package live.page.hubd.system.cosmetic;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletResponse;
import live.page.hubd.blobs.BlobsService;
import live.page.hubd.system.Settings;
import live.page.hubd.system.cosmetic.compress.CSSMin;
import live.page.hubd.system.cosmetic.compress.Compressors;
import live.page.hubd.system.cosmetic.compress.JSMin;
import live.page.hubd.system.cosmetic.svg.SVGServlet;
import live.page.hubd.system.cosmetic.svg.SVGTemplate;
import live.page.hubd.system.json.Json;
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
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cosmetic servlet, send CSS, JS an App. Aggregate, clean and compress files.
 * Convert App in Base64/CSS for better loading experience.
 */
@WebServlet(name = "UI Servlet", urlPatterns = {"/ui/*"}, loadOnStartup = 1)
public class UiStyleServlet extends LightServlet {

    private static String uiJs = null; //Normal JavaScript
    private static String uiCss = null; //Normal CSS
    private static String uiApp = null; //Normal App
    private static byte[] uiJsGZip = null; //Bzip JavaScript
    private static byte[] uiCssGZip = null; //Bzip CSS
    private static byte[] uiAppGZip = null; //Bzip App
    private static byte[] uiJsBr = null; //Bzip JavaScript
    private static byte[] uiCssBr = null; //Bzip CSS
    private static byte[] uiAppBr = null; //Bzip App
    private static String nameJs = null;
    private static String nameCss = null;
    private static String nameApp = null;
    private static Date dateJs = new Date(0);
    private static Date dateCss = new Date(0);
    private static Date dateApp = new Date(0);
    /**
     * The Control.
     */
    private final ScheduledExecutorService control = Executors.newSingleThreadScheduledExecutor();


    /**
     * Get copyright for file header
     *
     * @return the copyright
     */
    private static String getCopyright() {
        String copy = Fx.getResource("/res/copyright");
        return (copy == null) ? "" : copy + "\n";
    }

    /**
     * Get all links for css (Regular CSS or all files if debug
     *
     * @return the css links
     */
    public static List<String> getCssLinks() {
        List<String> uiCssFiles = new ArrayList<>();
        if (Fx.IS_DEBUG) {

            for (File css : FilesRepos.listResourcesFiles("css")) {
                uiCssFiles.add(Settings.getCDNHttp() + "/ui/hub" + css.getAbsolutePath());
            }

        } else {
            uiCssFiles.add(Settings.getCDNHttp() + nameCss);
        }
        return uiCssFiles;
    }

    /**
     * Get JavaScript file
     *
     * @return the js link
     */
    public static String getJsLink() {
        return Settings.getCDNHttp() + (nameJs != null ? nameJs : "/ui/debug.js?" + System.currentTimeMillis());
    }

    /**
     * Get last Date
     *
     * @param date the date
     * @param file the file
     * @return the date
     */
    private static Date controleDate(Date date, File file) {
        if (date == null || file.lastModified() > date.getTime()) {
            return new Date(file.lastModified());
        }
        return date;
    }

    /**
     * Get all JavaScript as String
     *
     * @return the js
     * @throws IOException the io exception
     */
    private static String getJs(boolean minified) throws IOException {

        try (StringWriter wrt = new StringWriter()) {
            wrt.append(getCopyright());


            Json constants = new Json().put("max_file_size", Settings.MAX_FILE_SIZE).put("apiurl", Settings.getApiHTTP()).put("cdn", Settings.getCDNHttp()).put("domain", Settings.STANDARD_HOST).put("logo", Settings.getLogo()).put("files_type", Settings.FILES_TYPE).put("debug", Fx.IS_DEBUG);

            wrt.append("\n\n/*!\n * Constants\n */\n" + "\nconst constants=").append(constants.toString(true)).append(";");

            wrt.append("\n\n/*!\n * System\n */\n");
            for (File jsfile : FilesRepos.listResourcesFiles("js")) {
                if (jsfile.getName().startsWith("admin")) {
                    continue;
                }
                dateJs = controleDate(dateJs, jsfile);
                String fileString = FileUtils.readFileToString(jsfile, StandardCharsets.UTF_8);
                Matcher matcher = Pattern.compile("\\$\\{?svg\\.([0-9a-z_\\-]+)}?", Pattern.CASE_INSENSITIVE).matcher(fileString);
                SVGTemplate svgTemplate = new SVGTemplate();
                while (matcher.find()) {
                    fileString = fileString.replace(matcher.group(0), svgTemplate.get(matcher.group(1)));
                }

                if (!minified || jsfile.getName().contains(".min.")) {
                    wrt.append(fileString).append("\n");
                } else {
                    wrt.append(JSMin.minify(fileString)).append("\n");
                }
            }
            File app = FilesRepos.getFile("/app/main.dart.js");
            if (app.exists()) {
                dateJs = controleDate(dateJs, app);
            }
            return wrt.toString();

        }
    }

    /**
     * Concat, date and compress JavaScript files
     */
    public static void buildJs() {
        if (!Fx.IS_DEBUG) {
            try {
                uiJs = getJs(true);
                if (nameApp != null) {
                    uiJs = uiJs.replace("/ui/app.js", nameApp);
                }
                uiJsGZip = Compressors.gzipCompressor(uiJs.getBytes());
                uiJsBr = Compressors.brCompressor(uiJs.getBytes());
                Fx.log("UI JS cached and compressed at " + Fx.UTCDate() + ", lastModification on " + dateJs);
                nameJs = "/ui/js-" + Hidder.encodeDate(dateJs) + ".js";
            } catch (Exception e) {
                e.printStackTrace();
                Fx.log("Error JS cacher " + Fx.UTCDate());
            }
        }

    }

    /**
     * Send result who depends on client or Debug
     */
    @Override
    public void doService(BaseServletRequest req, BaseServletResponse resp) throws IOException {

        if (req.getRequestURI().contains("@")) {
            BlobsService.processLocal(req, resp);
            return;
        }

        resp.setCharacterEncoding(StandardCharsets.UTF_8.displayName());

        if (!req.getServerName().equals(Settings.HOST_CDN)) {
            resp.sendError(404, "NOT_FOUND");
            return;
        }

        if (!req.getMethod().equals("GET")) {
            resp.sendError(405);
            return;
        }
        String acceptEncoding = req.getHeader("accept-encoding");
        List<String> accept = new ArrayList<>();
        if (acceptEncoding != null) {
            accept = Arrays.asList(acceptEncoding.split("([ ,]+)"));
        }

        resp.setHeader("Vary", "Accept-Encoding");


        if (!Fx.IS_DEBUG && req.getRequestURI().equals(nameCss)) {
            resp.setContentType("text/css; charset=utf-8");
            WebServletResponse.setHeaderMaxCache(resp);
            send(resp, accept, dateCss, uiCssGZip, uiCssBr, uiCss);
            return;

        }
        if (Fx.IS_DEBUG && req.getRequestURI().equals("/ui/app.js")) {
            resp.setContentType("application/javascript; charset=utf-8");
            WebServletResponse.setHeaderNoCache(resp);
            resp.getWriter().write(getApp());
            return;
        }
        if (!Fx.IS_DEBUG && req.getRequestURI().equals(nameApp)) {
            resp.setContentType("application/javascript; charset=utf-8");
            WebServletResponse.setHeaderMaxCache(resp);
            send(resp, accept, dateApp, uiAppGZip, uiAppBr, uiApp);
            return;
        }

        if (Fx.IS_DEBUG && req.getRequestURI().endsWith(".js")) {
            try {
                resp.setContentType("application/javascript; charset=utf-8");
                WebServletResponse.setHeaderNoCache(resp);
                resp.getWriter().write(getJs(false));
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (req.getRequestURI().equals(nameJs)) {
            resp.setContentType("application/javascript; charset=utf-8");
            WebServletResponse.setHeaderMaxCache(resp);
            send(resp, accept, dateJs, uiJsGZip, uiJsBr, uiJs);

            return;
        }


        if (Fx.IS_DEBUG && req.getRequestURI().matches("^/ui/hub/(.*).css$")) {
            resp.setContentType("text/css; charset=utf-8");
            WebServletResponse.setHeaderNoCache(resp);


            Matcher mat = Pattern.compile("^/ui/hub/([^/]+)/(.*)").matcher(req.getRequestURI());
            String str = null;
            if (mat.find()) {
                str = FileUtils.readFileToString(new File("/" + mat.group(1) + "/" + mat.group(2)), StandardCharsets.UTF_8);
            }
            if (str == null) {
                resp.sendError(404, "NOT_FOUND");
            } else {
                resp.getWriter().write(str);
            }
            try {
                resp.flushBuffer();
            } catch (Exception ignore) {
            }
            return;
        }

        if (!Fx.IS_DEBUG && req.getRequestURI().matches("^/ui/(.*).js$") && nameJs != null) {
            resp.sendRedirect(nameJs, 301);
            return;
        }
        if (!Fx.IS_DEBUG && req.getRequestURI().matches("^/ui/(.*).css$") && nameCss != null) {
            resp.sendRedirect(nameCss, 301);
            return;
        }

        resp.sendError(404, "NOT_FOUND");

    }

    @Override
    public void init(ServletConfig config) {
        if (Fx.IS_DEBUG) {
            return;
        }
        buildApp();
        buildJs();
        buildCss();
        control.scheduleAtFixedRate(this::control, Settings.CTRL_PERIOD, Settings.CTRL_PERIOD, TimeUnit.SECONDS);
    }

    /**
     * Periodical verifications of change
     */
    private void control() {
        Date newDateCss = new Date(0);
        for (File css : FilesRepos.listFiles("html", "css")) {
            newDateCss = controleDate(newDateCss, css);
        }
        if (newDateCss.getTime() > dateCss.getTime()) {
            buildCss();
        }

        Date newDateJs = new Date(0);
        for (File js : FilesRepos.listFiles("html", "js")) {
            newDateJs = controleDate(newDateJs, js);
        }

        File app = FilesRepos.getFile("/app/main.dart.js");
        newDateJs = controleDate(newDateJs, app);

        if (newDateJs.getTime() > dateJs.getTime()) {
            buildApp();
            buildJs();
        }
        SVGServlet.control();

    }

    @Override
    public void destroy() {
        Fx.shutdownService(control);
        uiJs = uiCss = null;
        uiJsGZip = uiCssGZip = null;
        dateJs = dateCss = null;
    }

    /**
     * Send corrects headers to client
     *
     * @param resp   the resp
     * @param accept the accept
     * @param date   the date
     * @param uiGZip the ui g zip
     * @param uiBr   the ui g br
     * @param ui     the ui
     * @throws IOException the io exception
     */
    private void send(HttpServletResponse resp, List<String> accept, Date date, byte[] uiGZip, byte[] uiBr, String ui) throws IOException {
        resp.setDateHeader("Last-Modified", date.getTime());
        ServletOutputStream out = resp.getOutputStream();

        if (accept.contains("br") && uiBr != null && uiBr.length > 0) {
            resp.setContentLength(uiBr.length);
            resp.setHeader("Content-Encoding", "br");
            out.write(uiBr);
        } else if (accept.contains("gzip") && uiGZip != null && uiGZip.length > 0) {
            resp.setContentLength(uiGZip.length);
            resp.setHeader("Content-Encoding", "gzip");
            out.write(uiGZip);
        } else {
            resp.setContentLength(ui.getBytes().length);
            out.write(ui.getBytes());
        }
    }

    private String getApp() {

        try {
            File appFile = FilesRepos.getFile("/app/main.dart.js");
            if (!appFile.exists()) {
                return null;
            }
            dateApp = new Date(appFile.lastModified());
            String mainDart = FileUtils.readFileToString(appFile, StandardCharsets.UTF_8);
            return getCopyright() + "\n" + mainDart;

        } catch (Exception e) {
            Fx.log("Error App cacher " + Fx.UTCDate());
        }
        return null;
    }

    /**
     * Concat, date and compress App files
     */
    private void buildApp() {

        try {
            uiApp = getApp();
            if (uiApp == null) {
                return;
            }
            uiAppGZip = Compressors.gzipCompressor(uiApp.getBytes());
            uiAppBr = Compressors.brCompressor(uiApp.getBytes());
            nameApp = "/ui/app-" + Hidder.encodeString(Hidder.encodeDate(dateApp)).toLowerCase() + ".js";
            if (!Fx.IS_DEBUG) {
                Fx.log("UI App cached and compressed at " + Fx.UTCDate() + ", lastModification on " + dateApp);
            }

        } catch (Exception e) {
            Fx.log("Error App cacher " + Fx.UTCDate());
        }
    }

    /**
     * Get all Css as String
     *
     * @return the css
     * @throws IOException the io exception
     */
    private String getCss() throws IOException {

        StringWriter wrt = new StringWriter();

        for (File cssfile : FilesRepos.listResourcesFiles("css")) {
            dateCss = controleDate(dateCss, cssfile);
            wrt.append(FileUtils.readFileToString(cssfile, StandardCharsets.UTF_8)).append("\n");

        }

        String css_data = wrt.toString();
        wrt.close();

        return css_data;
    }

    /**
     * Concat, date and compress CSS files
     */
    private void buildCss() {
        if (!Fx.IS_DEBUG) {
            try {
                String _uiCss = getCss();
                uiCss = CSSMin.minify(_uiCss);
                uiCss = uiCss.replace("url(/", "url(" + Settings.getCDNHttp() + "/");
                byte[] uiCss_copy = CSSMin.minify(getCopyright() + _uiCss).getBytes();
                uiCssGZip = Compressors.gzipCompressor(uiCss_copy);
                uiCssBr = Compressors.brCompressor(uiCss.getBytes());
                Fx.log("UI CSS cached and compressed at " + Fx.UTCDate() + ", lastModification on " + dateCss);
            } catch (Exception e) {
                Fx.log("Error CSS cacher " + Fx.UTCDate());
            }
        }
        nameCss = "/ui/css-" + Hidder.encodeDate(dateCss) + ".css";
    }


}
