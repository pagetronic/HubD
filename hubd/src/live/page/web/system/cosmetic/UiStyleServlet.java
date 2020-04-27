/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.system.cosmetic;

import live.page.web.blobs.BlobsService;
import live.page.web.system.Language;
import live.page.web.system.Settings;
import live.page.web.system.cosmetic.svg.SVGServlet;
import live.page.web.system.cosmetic.svg.SVGTemplate;
import live.page.web.system.json.Json;
import live.page.web.system.servlet.BaseServlet;
import live.page.web.system.servlet.wrapper.BaseServletRequest;
import live.page.web.system.servlet.wrapper.BaseServletResponse;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.utils.Fx;
import live.page.web.utils.Hidder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cosmetic servlet, send CSS, JS an fonts. Aggregate, clean and compress files.
 * Convert fonts in Base64/CSS for better loading experience.
 */
@WebServlet(name = "UI Servlet", urlPatterns = {"/ui/*"}, loadOnStartup = 1)
public class UiStyleServlet extends BaseServlet {

	//control change of files for update
	private final ScheduledExecutorService control = Executors.newSingleThreadScheduledExecutor();

	//No compression
	private static String uiJs = null; //Normal JavaScript
	private static String uiCss = null; //Normal CSS
	private static String uiFonts = null; //Normal Fonts

	//Bzip compression
	private static byte[] uiJsGZip = null; //Bzip JavaScript
	private static byte[] uiCssGZip = null; //Bzip CSS
	private static byte[] uiFontsGZip = null; //Bzip Fonts


	//Brotli compression
	private static byte[] uiJsBRZip = null; //Brotli JavaScript
	private static byte[] uiCssBRZip = null; //Brotli CSS
	private static byte[] uiFontsBRZip = null; //Brotli Fonts

	//Uniques names
	private static String nameJs = null;
	private static String nameCss = null;
	private static String nameFonts = null;

	//Date of changes
	private static Date date_js = new Date(0);
	private static Date date_css = new Date(0);
	private static Date date_fonts = new Date(0);

	@Override
	public void init(ServletConfig config) {
		buildFonts();
		buildJs();
		buildCss();
		control.scheduleAtFixedRate(this::control, Settings.CTRL_PERIOD, Settings.CTRL_PERIOD, TimeUnit.SECONDS);
	}

	@Override
	public void destroy() {
		Fx.shutdownService(control);
		uiJs = uiCss = null;
		uiJsGZip = uiCssGZip = null;
		date_js = date_css = null;
	}

	/**
	 * Periodical verifications of change
	 */
	private void control() {
		List<File> csss = Fx.listFiles(Settings.REPO + "/html", "css");
		csss.addAll(Fx.listFiles(Settings.HUB_REPO + "/libs", "css"));
		if (!Settings.NOUI) {
			csss.addAll(Fx.listFiles(Settings.HUB_REPO + "/html", "css"));
		}
		for (File css : csss) {
			if (css.lastModified() > date_css.getTime()) {
				buildCss();
				break;
			}
		}

		List<File> jss = Fx.listFiles(Settings.REPO + "/html", "js");
		jss.addAll(Fx.listFiles(Settings.HUB_REPO + "/libs", "js"));
		if (!Settings.NOUI) {
			jss.addAll(Fx.listFiles(Settings.HUB_REPO + "/html", "js"));
		}
		for (File js : jss) {
			if (js.lastModified() > date_js.getTime()) {
				buildJs();
				break;
			}
		}

		SVGServlet.control();

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
			resp.sendError(404, "Not found");
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
			send(resp, accept, date_css, uiCssGZip, uiCssBRZip, uiCss);
			return;

		}

		if (req.getRequestURI().equals(nameFonts)) {
			resp.setContentType("text/css; charset=utf-8");
			WebServletResponse.setHeaderMaxCache(resp);
			send(resp, accept, date_fonts, uiFontsGZip, uiFontsBRZip, uiFonts);
			return;
		}


		if (Fx.IS_DEBUG && req.getRequestURI().endsWith(".js")) {
			try {
				resp.setContentType("application/javascript; charset=utf-8");
				WebServletResponse.setHeaderNoCache(resp);
				resp.getWriter().write(getJs());
				return;
			} catch (Exception e) {
			}
		}

		if (req.getRequestURI().equals(nameJs)) {
			resp.setContentType("application/javascript; charset=utf-8");
			WebServletResponse.setHeaderMaxCache(resp);
			send(resp, accept, date_js, uiJsGZip, uiJsBRZip, uiJs);

			return;
		}


		if (Fx.IS_DEBUG && req.getRequestURI().matches("^/ui/hub/(.*).css$")) {
			resp.setContentType("text/css; charset=utf-8");
			WebServletResponse.setHeaderNoCache(resp);


			Matcher mat = Pattern.compile("^/ui/hub/(.*)").matcher(req.getRequestURI());
			String str = null;
			if (mat.find()) {
				str = FileUtils.readFileToString(new File("/" + mat.group(1)));
			}
			if (str == null) {
				resp.sendError(404, "Not found");
			} else {
				resp.getWriter().write(str);
			}
			resp.flushBuffer();
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

		resp.sendError(404, "Not found");

	}


	/**
	 * Send corrects headers to client
	 */
	private void send(HttpServletResponse resp, List<String> accept, Date date, byte[] uiGZip, byte[] uiBRZip, String ui) throws IOException {
		resp.setDateHeader("Last-Modified", date.getTime());
		ServletOutputStream out = resp.getOutputStream();

		if (accept.contains("br") && uiBRZip != null) {
			resp.setContentLength(uiBRZip.length);
			resp.setHeader("Content-Encoding", "br");
			out.write(uiBRZip);

		} else if (accept.contains("gzip")) {
			resp.setContentLength(uiGZip.length);
			resp.setHeader("Content-Encoding", "gzip");
			out.write(uiGZip);
		} else {
			resp.setContentLength(ui.getBytes().length);
			out.write(ui.getBytes());
		}
	}

	/**
	 * Concat, date and compress CSS files
	 */
	private void buildCss() {
		if (!Fx.IS_DEBUG) {
			try {
				String _uiCss = getCss();
				uiCss = Compressors.compressCss(_uiCss);
				uiCss = uiCss.replace("url(/", "url(" + Settings.getCDNHttp() + "/");
				byte[] uiCss_copy = Compressors.compressCss(getCopyright() + _uiCss).getBytes();
				uiCssGZip = Compressors.gzipCompressor(uiCss_copy);
				uiCssBRZip = Compressors.brotliCompressor(uiCss_copy);
				Fx.log("UI CSS cached and compressed at " + Fx.UTCDate() + ", lastModification on " + date_css);
			} catch (Exception e) {
				Fx.log("Error CSS cacher " + Fx.UTCDate());
				e.printStackTrace();
			}
		}
		nameCss = "/ui/css-" + Hidder.encodeDate(date_css) + ".css";
	}

	/**
	 * Concat, date and compress Fonts files
	 */
	private void buildFonts() {

		try {
			List<File> ttfs = Fx.listFiles(Settings.REPO + "/html", "woff");
			if (ttfs.size() > 0) {
				StringWriter wrt = new StringWriter();
				String simplename = ttfs.get(0).getName().split("-")[0].split("\\.")[0];
				StringBuilder names = new StringBuilder();
				for (File fontfile : ttfs) {
					names.append(fontfile.getName());
					date_fonts = controleDate(date_fonts, fontfile);
					wrt.append("@font-face {\n");
					wrt.append("/*! Copyright Font ").append(simplename).append(" */\n");
					wrt.append("font-family:'").append(simplename).append("';\n");
					wrt.append("font-weight:").append(fontfile.getName().contains("Bold") ? "bold" : "normal").append(";\n");
					wrt.append("font-style:").append(fontfile.getName().contains("Italic") || fontfile.getName().contains("Light") ? "italic" : "normal").append(";\n");
					wrt.append("src:url(data:font/woff;charset=utf-8;base64,").append(Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(fontfile))).append(") format('woff');\n");
					wrt.append("}\n");
					wrt.append("\n");
				}

				uiFonts = "\n" + (Fx.IS_DEBUG ? wrt.toString() : Compressors.compressCss(wrt.toString()));
				uiFontsGZip = Compressors.gzipCompressor(uiFonts.getBytes());
				uiFontsBRZip = Compressors.brotliCompressor(uiFonts.getBytes());
				nameFonts = "/ui/fonts-" + Hidder.encodeString(names.toString().hashCode() + Hidder.encodeDate(date_fonts)).toLowerCase() + ".css";
				Fx.log("UI Fonts cached and compressed at " + Fx.UTCDate() + ", lastModification on " + date_fonts);
				wrt.close();
			}
		} catch (Exception e) {
			Fx.log("Error Fonts cacher " + Fx.UTCDate());
		}

	}

	/**
	 * Get all Css as String
	 */
	private String getCss() throws IOException {

		StringWriter wrt = new StringWriter();

		if (!Settings.NOUI) {

			for (File cssfile : Fx.listFiles(Settings.HUB_REPO + "/html", "css")) {
				date_css = controleDate(date_css, cssfile);
				wrt.append(FileUtils.readFileToString(cssfile)).append("\n");
			}
		}


		for (File cssfile : Fx.listFiles(Settings.HUB_REPO + "/libs", "css")) {
			date_css = controleDate(date_css, cssfile);
			wrt.append(FileUtils.readFileToString(cssfile)).append("\n");

		}
		for (File cssfile : Fx.listFiles(Settings.REPO + "/html", "css")) {
			date_css = controleDate(date_css, cssfile);
			wrt.append(FileUtils.readFileToString(cssfile)).append("\n");
		}

		String css_data = wrt.toString();
		wrt.close();

		return css_data;
	}

	/**
	 * Concat, date and compress JavaScript files
	 */
	public static void buildJs() {
		if (!Fx.IS_DEBUG) {
			try {
				String _uiJs = getJs();
				uiJs = Compressors.compressJs(_uiJs);
				uiJsGZip = Compressors.gzipCompressor(uiJs.getBytes());
				uiJsBRZip = Compressors.brotliCompressor(uiJs.getBytes());
				Fx.log("UI JS cached and compressed at " + Fx.UTCDate() + ", lastModification on " + date_js);
				nameJs = "/ui/js-" + Hidder.encodeDate(date_js) + ".js";
			} catch (Exception e) {
				Fx.log("Error JS cacher " + Fx.UTCDate());
				e.printStackTrace();
			}
		}

	}

	/**
	 * Get all JavaScript as String
	 */
	private static String getJs() throws IOException {

		try (StringWriter wrt = new StringWriter()) {
			wrt.append(getCopyright());

			wrt.append("\n\n/*!\n * Language\n */\n");
			wrt.append("\n").append(Fx.getResource("/res/langs.js").replace("var lang = {};", "var lang = " + Language.getLangsJs() + ";"));

			Json constants = new Json()
					.put("ajax", Settings.AJAX)
					.put("max_file_size", Settings.MAX_FILE_SIZE)
					.put("apiurl", Settings.getApiHTTP()).put("cdnurl", Settings.getCDNHttp())
					.put("domain", Settings.STANDARD_HOST).put("logo", Settings.getLogo())
					.put("files_type", Settings.FILES_TYPE).put("vapId", Settings.VAPID_PUB).put("debug", Fx.IS_DEBUG)
					.put("domains", Settings.getDomains());

			wrt.append("\n\n/*!\n * Constants\n */\n" + "\nvar constants=").append(constants.toString(true)).append(";");

			wrt.append("\n\n/*!\n * Base System\n */\n");
			for (File jsfile : Fx.listFiles(Settings.HUB_REPO + "/libs", "js")) {
				date_js = controleDate(date_js, jsfile);
				wrt.append(FileUtils.readFileToString(jsfile)).append("\n");
			}

			if (!Settings.NOUI) {
				wrt.append("\n\n/*!\n * System\n */\n");
				for (File jsfile : Fx.listFiles(Settings.HUB_REPO + "/html", "js")) {
					date_js = controleDate(date_js, jsfile);
					wrt.append(FileUtils.readFileToString(jsfile)).append("\n");
				}

			}
			for (File jsfile : Fx.listFiles(Settings.REPO + "/html", "js")) {
				date_js = controleDate(date_js, jsfile);
				wrt.append(FileUtils.readFileToString(jsfile)).append("\n");
			}


			wrt.append("\nsys.push=function(exe){if(typeof exe=='string'){eval(exe);}else{exe();}};$.each(sys.$,function(){sys.push(this);});delete sys.$;\n");

			String str = wrt.toString();
			Matcher matcher = Pattern.compile("\\$\\{?svg\\.([0-9a-z_\\-]+)}?", Pattern.CASE_INSENSITIVE).matcher(str);
			while (matcher.find()) {
				str = str.replaceFirst(Pattern.quote(matcher.group(0)), SVGTemplate.get(matcher.group(1)));
			}

			if (date_js.before(SVGServlet.getDate())) {
				date_js = SVGServlet.getDate();
			}
			return str;

		}
	}

	/**
	 * Get copyright for file header
	 */
	private static String getCopyright() {
		String copy = Fx.getResource("/res/copyright");
		return (copy == null) ? "" : copy + "\n";
	}

	/**
	 * Get all links for css (Regular CSS or all files if debug
	 */
	public static List<String> getCssLinks() {
		List<String> uiCssFiles = new ArrayList<>();
		if (Fx.IS_DEBUG) {

			if (!Settings.NOUI) {
				for (String lib : Fx.listFilesNames(Settings.HUB_REPO + "/html", "css")) {
					uiCssFiles.add(Settings.getCDNHttp() + "/ui/hub" + lib);
				}
			}

			for (String lib : Fx.listFilesNames(Settings.HUB_REPO + "/libs", "css")) {
				uiCssFiles.add(Settings.getCDNHttp() + "/ui/hub" + lib);
			}
			for (String lib : Fx.listFilesNames(Settings.REPO + "/html", "css")) {
				uiCssFiles.add(Settings.getCDNHttp() + "/ui/hub" + lib);
			}

		} else {
			uiCssFiles.add(Settings.getCDNHttp() + nameCss);
		}
		return uiCssFiles;
	}


	/**
	 * Get CSS Fonts file
	 */
	public static String getFontsLink() {
		return nameFonts == null ? null : Settings.getCDNHttp() + nameFonts;
	}

	/**
	 * Get JavaScript file
	 */
	public static String getJsLink() {
		return Settings.getCDNHttp() + (nameJs != null ? nameJs : "/ui/debug.js");
	}

	/**
	 * Get last Date
	 */
	private static Date controleDate(Date date, File file) {
		if (date == null || file.lastModified() > date.getTime()) {
			return new Date(file.lastModified());
		}
		return date;
	}

	/**
	 * Get preload header
	 */
	public static String getPreloadHeader() {
		List<String> csss = getCssLinks();
		if (getFontsLink() != null) {
			csss.add(getFontsLink());
		}
		csss.add("");
		return "<" + StringUtils.join(csss, ">; rel=preload; as=style,<") + getJsLink() + ">; rel=preload; as=script";
	}


}
