/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.system.cosmetic.svg;

import live.page.web.system.Settings;
import live.page.web.system.cosmetic.Compressors;
import live.page.web.system.cosmetic.UiStyleServlet;
import live.page.web.system.servlet.BaseServlet;
import live.page.web.system.servlet.wrapper.BaseServletRequest;
import live.page.web.system.servlet.wrapper.BaseServletResponse;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.utils.Fx;
import live.page.web.utils.Hidder;
import org.apache.commons.io.FileUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WebServlet(name = "SVG Source Servlet", urlPatterns = {"/svg/*"}, loadOnStartup = 1)
public class SVGServlet extends BaseServlet {

	private static byte[] svgSrc = null;
	private static byte[] svgGZip = null;
	private static byte[] svgBRZip = null;
	private static Date svgSrcDate = new Date(0);

	public static String getName() {
		return "/svg/" + Hidder.encodeDate(svgSrcDate) + ".svg";
	}


	/**
	 * Get compiled SVG file with only SVGs used
	 */
	@Override
	public void doService(BaseServletRequest req, BaseServletResponse resp) throws IOException {

		if (!req.getServerName().equals(Settings.STANDARD_HOST) && !req.getServerName().equals(Settings.HOST_CDN) && !Settings.LANGS_DOMAINS.containsValue(req.getServerName())) {
			resp.sendError(404, "Not found");
			return;
		}

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

		if (accept.contains("br") && svgBRZip != null) {

			resp.setContentLength(svgBRZip.length);
			resp.setHeader("Content-Encoding", "br");
			out.write(svgBRZip);

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
	 * Periodical verifications of change
	 */
	public static void control() {
		Date current = new Date(0);
		for (File file : getFilesToScan()) {
			try {
				String str = FileUtils.readFileToString(file);
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
			SVGParser.make();
			build();
			Fx.log("SVG rebuild in " + (System.currentTimeMillis() - start) + "ms");
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

	/**
	 * Build or rebuilt SVG file
	 */
	public static void build() {
		svgSrc = buildSvg();
		svgGZip = Compressors.gzipCompressor(svgSrc);
		svgBRZip = Compressors.brotliCompressor(svgSrc);
		UiStyleServlet.buildJs();
	}

	/**
	 * Get list of file to scan for SVG use
	 */
	private static List<File> getFilesToScan() {
		List<File> files = new ArrayList<>();
		files.addAll(Fx.listFiles(Settings.REPO + "/html", "js", "html"));
		files.addAll(Fx.listFiles(Settings.HUB_REPO + "/libs", "js"));
		if (!Settings.NOUI) {
			files.addAll(Fx.listFiles(Settings.HUB_REPO + "/html", "js", "html"));
		}
		files.addAll(Fx.listFiles(Settings.REPO + "/src", "java"));
		files.addAll(Fx.listFiles(Settings.HUB_REPO + "/src", "java"));
		return files;
	}


	/**
	 * Build or rebuilt SVG and get Byte for compression
	 */
	private static byte[] buildSvg() {
		try {
			Map<String, String> materialIcons = new HashMap<>();
			Map<String, String> fontAwesome = new HashMap<>();

			for (File file : getFilesToScan()) {
				String str = FileUtils.readFileToString(file);
				for (String pattern : Arrays.asList("\\$\\{?svg\\.([0-9a-z_\\-]+)}?", SVGTemplate.class.getSimpleName() + ".get\\(\"([0-9a-z_\\-]+)\"\\)")) {
					Matcher mfile = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(str);

					while (mfile.find()) {
						String id = mfile.group(1);
						if (id.startsWith("fa_") && !fontAwesome.containsKey(id)) {
							fontAwesome.put(id, SVGParser.get(id));
						} else if (id.startsWith("mi_") && !materialIcons.containsKey(id)) {
							materialIcons.put(id, SVGParser.get(id));
						}
					}
					if (mfile.find(0)) {
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
			Collection<String> mis = materialIcons.values();
			for (String mi : mis) {
				src_build.append(mi);
			}

			src_build.append("\n\n<!--\n" +
					" * Copyright 2016 by Dave Gandy @davegandy\n" +
					" * Font Awesome 3.0 http://fontawesome.io - @fontawesome\n" +
					" * License: http://fontawesome.io/license\n" +
					"-->\n");
			Collection<String> fas = fontAwesome.values();
			for (String fa : fas) {
				src_build.append(fa);
			}
			src_build.append("</svg>");

			return src_build.toString().getBytes(StandardCharsets.UTF_8);

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}


	/**
	 * Get last date of change
	 */
	public static Date getDate() {
		return svgSrcDate;
	}
}
