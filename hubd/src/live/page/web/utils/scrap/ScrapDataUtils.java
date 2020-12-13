/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.utils.scrap;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import live.page.web.blobs.BlobsUtils;
import live.page.web.system.db.Db;
import live.page.web.system.json.Json;
import live.page.web.utils.Fx;
import live.page.web.utils.apis.YouTubeApi;
import live.page.web.utils.http.HttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScrapDataUtils {

	private static final int max = 1000;

	/**
	 * Get snippet and abstract from an Url without cache
	 *
	 * @param url to get
	 * @return data of the page
	 */
	public static Json scrapNoCache(String url) {
		String html = HttpClient.getAsFacebook(url);
		if (html != null && html.length() > 10) {
			return parseData(Jsoup.parse(html, url));
		}
		return null;
	}


	/**
	 * Get snippet and abstract from an Url with cache
	 *
	 * @param url to get
	 * @return data of the page
	 */
	public static Json getData(String url) {
		Json preview;
		if (url.matches("//(www.)?(youtube.com|youtu.be)")) {
			preview = youtube(url);
		} else {
			preview = normal(url);
		}
		preview.put("url", url);
		try {
			preview.put("domain", new URL(url).getHost());
		} catch (MalformedURLException e) {
		}
		return preview;
	}


	/**
	 * Get snippet and abstract from a Jsoup document
	 *
	 * @param page Jsoup document
	 * @return data of the page
	 */
	public static Json parseData(Document page) {
		return parseData(page, null);
	}

	/**
	 * Get snippet and abstract from a Jsoup document
	 *
	 * @param page        Jsoup document
	 * @param cleaner_str clean the title
	 * @return data of the page
	 */
	public static Json parseData(Document page, String cleaner_str) {
		Pattern cleaner = Pattern.compile(cleaner_str);

		Json rez = new Json();

		Element title = page.select("title").first();

		if (title == null || title.text().replaceAll(cleaner.pattern(), "").length() < 20) {
			for (Element h1 : page.select("h1, h2")) {
				if (h1.text().replaceAll(cleaner.pattern(), "").length() > 20) {
					title = h1;
					break;
				}
			}
		}
		if (title != null) {
			rez.put("title", Jsoup.clean(title.text(), "/", new Whitelist()));
		}
		Element firstlang = page.select("[lang]").first();
		if (firstlang != null) {
			String lang = firstlang.attr("lang");
			if (lang != null && lang.length() >= 2) {
				rez.put("lng", firstlang.attr("lang").substring(0, 2));
			}
		}

		page.select("iframe, object, script, ol, ul, svg, h1, menu, .menu, #menu, .copyright, footer, .footer, .sidebar, .pagination, form, button, time, date, .date, .time, [datetime], .users, .breadcrumb, #breadcrumb, .partner, .partners").remove();
		List<String> logos = getLogos(page);

		for (Element video : page.select("iframe")) {
			String src = video.attr("src");
			if (src != null && (src.startsWith("https://www.dailymotion.com/") || src.startsWith("https://www.youtube.com/"))) {
				rez.put("video", src);
			}
		}

		page.select("aside, img, figure").remove();

		page.select("p, h2, h3, h4").forEach(p -> {
			Element pp = p.clone();
			pp.select("a").remove();
			if (pp.text().length() <= 35) {
				p.remove();
			}
		});

		page.select("p").forEach(p -> {
			if (p.text().length() <= 35) {
				p.remove();
			}
		});

		page.select("div").forEach(div -> {
			if (div.select("p").size() == 0 && div.text().length() < 100) {
				div.remove();
			}
		});

		StringBuilder description = new StringBuilder();

		Elements articles = page.select("article");
		for (String text : articles.eachText()) {
			text = text.replaceAll("\\s+", " ");
			if (text.length() > 20 && description.length() < max) {
				description.append(text);
				description.append(" ");
			}
		}

		if (description.length() < max) {
			Elements sections = page.select("section");
			for (String text : sections.eachText()) {
				text = text.replaceAll("\\s+", " ");
				if (text.length() > 20 && description.length() < max) {
					description.append(text);
					description.append(" ");
				}
			}
			if (description.length() < max) {
				Elements ps = page.select("p");
				for (String text : ps.eachText()) {
					text = text.replaceAll("\\s+", " ");
					if (text.length() > 20 && description.length() < max) {
						description.append(text);
						description.append(" ");
					}
				}
				if (description.length() < max) {
					Elements meta = page.select("meta[name=description]");
					if (meta.size() > 0) {
						description.append(meta.first().attr("content"));
						description.append(" ");
					}
				}
			}
		}

		rez.put("description", Fx.truncate(Jsoup.clean(description.toString(), "/", new Whitelist()), max));

		rez.put("logos", logos);
		rez.put("url", page.location());

		return rez;
	}

	/**
	 * Get logo of the page
	 *
	 * @param page Jsoup document where to find logos
	 * @return a List of logos ordered by size
	 */
	private static List<String> getLogos(Document page) {

		List<String> dejavu = new ArrayList<>();
		ImagesSortedStore logos = new ImagesSortedStore();
		int maxi = 20;
		for (Element image : page.select("[itemprop=image], [rel=schema:image], meta[property=og:image], meta[name=twitter:image], [style], img, [type=image/png]")) {

			String width_attr = image.attr("width");
			String height_attr = image.attr("height");
			int width = -1;
			int height = -1;
			String src = image.absUrl("src");
			if ((src == null || src.equals("")) && image.absUrl("content") != null) {
				src = image.absUrl("content");
			}
			if ((src == null || src.equals("")) && image.absUrl("resource") != null) {
				src = image.absUrl("resource");
			}
			if ((src == null || src.equals("")) && image.absUrl("href") != null) {
				src = image.absUrl("href");
			}
			if ((src == null || src.equals("")) && image.absUrl("style") != null) {
				String style = image.attr("style");
				if (style == null || !style.contains("url(")) {
					continue;
				}
				Matcher match = Pattern.compile("url\\([\"' ]?+([^)\"']+)").matcher(style);
				if (match.find()) {
					src = match.group(1);

				}
			}

			if ((src == null || src.equals("")) && image.attr("srcset") != null) {
				try {
					String srcset = image.attr("srcset");
					if (srcset != null && !srcset.equals("")) {
						int src_w = -1;
						for (String src_ : srcset.split("([ ]+)?([,+\\-])([ ]+)?", 60)) {
							String[] src_s = src_.split("([ ]+)");
							if (Integer.valueOf(src_s[1].replaceAll("^.*?([0-9]+).*?$", "$1")) > src_w && src_w < 300) {
								src = src_s[0];
							}
						}
					}
				} catch (Exception ignore) {

				}

			}

			if (src != null && !src.equals("") && !src.matches("^https?://.*$")) {

				try {
					URL url = new URL(page.location());
					src = url.getProtocol() + "://" + url.getHost() + (src.startsWith("/") ? "" : "/");
				} catch (Exception ignore) {
				}
			}

			if (src == null || dejavu.contains(src) || src.endsWith(".svg")) {
				continue;
			}

			dejavu.add(src);

			if (width_attr != null && width_attr.matches("^[0-9]+$")) {
				width = Integer.parseInt(width_attr);
			}
			if (height_attr != null && height_attr.matches("^[0-9]+$")) {
				height = Integer.parseInt(height_attr);
			}

			if ((width < 0 || height < 0) && src != null) {
				int[] size = BlobsUtils.getSize(src);
				if (size.length == 2) {
					width = size[0];
					height = size[1];
				}
			}

			if (width > 200 && src != null && !logos.contains(src) && (width / height) < 4) {
				logos.addImage(src, width, height);
				maxi--;
			}

			if (maxi <= 0) {
				break;
			}
		}
		return logos.getSorted();
	}


	/**
	 * Get snippet and abstract from an YouTube Url
	 *
	 * @param url to get
	 * @return data of the page
	 */
	private static Json youtube(String url) {
		String pattern = Pattern.compile("(?:https?://)?(?:youtu\\.be/|(?:www\\.)?youtube\\.com/(?:watch(?:\\.php)?\\?.*v=|v/|embed/))([a-zA-Z0-9\\-_]+)").pattern();
		String id = url.replaceAll(pattern, "$1");
		return YouTubeApi.getVideoSnippets(id);
	}

	/**
	 * Get snippet and abstract from an Url
	 *
	 * @param url to get
	 * @return data of the page
	 */
	private static Json normal(String url) {
		Json dejavu = Db.findOneAndUpdate("DejaVu",
				Filters.eq("_id", url),
				new Json("$inc", new Json("suggested", 1))
						.put("$setOnInsert", new Json()
								.put("_id", url)
						),
				new FindOneAndUpdateOptions().upsert(true).bypassDocumentValidation(true).returnDocument(ReturnDocument.AFTER)
		);

		if (dejavu.containsKey("data")) {
			return dejavu.getJson("data");
		} else {
			String html = HttpClient.getAsFacebook(url);
			if (html != null && html.length() > 10) {
				Json data = parseData(Jsoup.parse(html, url));
				Db.updateOne("DejaVu", Filters.eq("_id", url), new Json("$set", new Json("data", data)));
				return data;
			} else {
				return new Json("error", "NO_RESULT");
			}
		}
	}


	/**
	 * Get Title from an Url
	 *
	 * @param url to get
	 * @return title of the page
	 */
	public static String getTitle(String url) {
		try {
			if (url.contains("youtube.com/channel")) {
				return YouTubeApi.getChannelTitle(url.replace("https://www.youtube.com/channel/", ""));
			}
			return Jsoup.parse(HttpClient.get(url), url).select("title").first().text();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Class to order images by sizes
	 */
	private static class ImagesSortedStore {
		private final List<ImageElement> list = new ArrayList<>();

		/**
		 * List of images sorted by size
		 *
		 * @return List of images in order
		 */
		public List<String> getSorted() {
			list.sort((img1, img2) -> {
				try {
					return (img2.width * img2.height * (new URL(img2.src).getPath().matches("^.*\\.jpe?g$") ? 100 : 1)) -
							(img1.width * img1.height * (new URL(img1.src).getPath().matches("^.*\\.jpe?g$") ? 100 : 1));
				} catch (Exception e) {
					return Integer.MIN_VALUE;
				}
			});
			List<String> sorted = new ArrayList<>();
			for (ImageElement img : list) {
				sorted.add(img.src);
			}
			return sorted;
		}

		/**
		 * Add image to the store
		 *
		 * @param src    of the image
		 * @param width  of the image
		 * @param height of the image
		 */
		public void addImage(String src, int width, int height) {
			list.add(new ImageElement(src, width, height));
		}

		/**
		 * Is this Store contains the image?
		 *
		 * @param src of the image
		 * @return true if image is in this store
		 */
		public boolean contains(String src) {
			for (ImageElement img : list) {
				if (img.src.equals(src)) {
					return true;
				}
			}
			return false;
		}

		/**
		 * Image information stored in the store
		 */
		private class ImageElement {
			final String src;
			final int width;
			final int height;

			public ImageElement(String src, int width, int height) {
				this.src = src;
				this.width = width;
				this.height = height;

			}
		}
	}

}
