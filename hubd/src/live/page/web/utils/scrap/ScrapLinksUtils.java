/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.utils.scrap;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import live.page.web.system.db.Db;
import live.page.web.system.json.Json;
import live.page.web.utils.Fx;
import live.page.web.utils.apis.YouTubeApi;
import live.page.web.utils.http.HttpClient;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

public class ScrapLinksUtils {

	/**
	 * Get page content
	 *
	 * @param url of the page
	 * @return string representing content
	 */
	public static String get(String url) {
		return HttpClient.getAsFacebook(url);
	}

	/**
	 * Get page content
	 *
	 * @param onResult   function to execute on result
	 * @param url        to get
	 * @param cleaner    regex for clean title
	 * @param lng        language needed
	 * @param scraps     elements to get
	 * @param aggregater is search external links ?
	 * @param link       where to get ?
	 * @param exclude    words from page
	 * @param preview    is preview mode?
	 * @throws InterruptedException on interruption
	 */
	public static void scrap(OnScrapResult onResult, String url, String cleaner, String lng, List<Json> scraps, boolean aggregater, String link, String exclude, boolean preview) throws InterruptedException {

		if (Fx.IS_DEBUG) {
			Fx.log("start scrap: " + url);
		}
		List<String> excludes = (exclude == null || exclude.matches("^([ ]+)?$")) ? null : Arrays.asList(exclude.toLowerCase().split(" ?[,] ?"));
		if (url.startsWith("https://www.youtube.com/channel/")) {
			getYoutubeChannelLinks(onResult, url.replace("https://www.youtube.com/channel/", ""), scraps.get(0).getList("forums"), cleaner, excludes, preview);
		} else {
			getPages(onResult, url, cleaner, lng, scraps, aggregater, link, excludes, preview);
		}
		if (Fx.IS_DEBUG) {
			Fx.log("end scrap: " + url);
		}
	}

	/**
	 * Get page content
	 *
	 * @param onResult   function to execute on result
	 * @param url        to get
	 * @param cleaner    regex for clean title
	 * @param lng        language needed
	 * @param scraps     elements to get
	 * @param aggregater is search external links ?
	 * @param link       where to get ?
	 * @param excludes   List of terms to exclude if in page
	 * @param preview    is preview mode?
	 * @throws InterruptedException on interruption
	 */
	public static void getPages(OnScrapResult onResult, String url, String cleaner, String lng, List<Json> scraps, boolean aggregater, String link, List<String> excludes, boolean preview) throws InterruptedException {
		String html = get(url);
		if (html == null) {
			Fx.log("error site scrap no content : " + url);
			onResult.run(new Json("error", "no content"));
			return;
		}
		Document site = Jsoup.parse(html, url);
		for (Json scrap : scraps) {
			if (Thread.interrupted()) {
				if (Fx.IS_DEBUG) {
					Fx.log("thread scrap stopped");
				}
				return;
			}

			List<String> includes = (scrap.getString("include", null) == null || scrap.getString("include").matches("^([ ]+)?$")) ? null : Arrays.asList(scrap.getString("include").split(" ?[,] ?"));
			Elements pages = site.select(scrap.getString("zone", "").replaceFirst("([ ]+)?$", ""));
			if (pages.size() == 0) {
				Fx.log("error site scrap no links in zone: " + url);
				onResult.run(new Json("error", "no links in zone"));
				return;
			}
			for (Element page : pages) {
				String sub_url = page.absUrl("href").replaceAll("#.*$", "");
				if (sub_url.equals(url) ||
						!isIncludeUrl(sub_url, includes) || isFixedExclude(sub_url) || isExclude(sub_url, excludes) || (!preview && isDejaVu(sub_url))) {
					continue;
				}


				if (!aggregater) {

					Thread.sleep(1000);

					String base_url = "";
					try {
						URL tmp = new URL(url);
						base_url = tmp.getProtocol() + "://" + tmp.getHost();
					} catch (MalformedURLException ignore) {
					}
					if (sub_url == null || sub_url.equals("") || !sub_url.startsWith(base_url)) {
						continue;
					}

					if (Fx.IS_DEBUG) {
						Fx.log("get data scrap: " + sub_url);
					}
					Json data = getPageData(sub_url, cleaner);
					if (data != null) {
						data.put("forums", scrap.getList("forums"));
						if (tooShort(data)) {
							Fx.log("too short subpage scrap data: " + sub_url);

						} else if (!isExclude(data.getString("title", ""), excludes) && !isExclude(data.getText("description", ""), excludes)) {
							data = cleaner(cleaner, data);
							if (!onResult.run(data)) {
								return;
							}

						} else if (data != null && Fx.IS_DEBUG) {
							Fx.log("exclude: ");
							Fx.log(data);
						}
					}
					if (data == null) {
						if (Fx.IS_DEBUG) {
							Fx.log("null subpage scrap data: " + sub_url);
						}
						unDejaVu(sub_url);
					}


				} else {

					if (Fx.IS_DEBUG) {
						Fx.log("get page scrap: " + sub_url);
					}
					Thread.sleep(500);

					String subhtml = get(sub_url);

					if (subhtml != null) {
						Document pagein = Jsoup.parse(subhtml, sub_url);
						Elements subpages = pagein.select(link);
						if (subpages.size() == 0) {
							continue;
						}
						for (Element subpage : subpages) {
							String agg_link = subpage.absUrl("href");
							if (agg_link.startsWith(url)) {
								continue;
							}

							if (agg_link == null || agg_link.equals("") ||
									isFixedExclude(agg_link) || isExclude(agg_link, excludes) || (!preview && isDejaVu(agg_link))) {
								continue;
							}

							if (Fx.IS_DEBUG) {
								Fx.log("get subpage scrap: " + agg_link);
							}
							Json data = getPageData(agg_link, cleaner);
							if (data != null) {
								data.put("forums", scrap.getList("forums"));
								if (tooShort(data)) {
									Fx.log("too short subpage scrap data: " + agg_link);

								} else if (data.getString("lng", lng).equals(lng) &&
										!isExclude(data.getString("title", ""), excludes) &&
										!isExclude(data.getText("description", ""), excludes)) {


									data = cleaner(cleaner, data);
									if (!onResult.run(data)) {
										return;
									}


								} else if (data != null && Fx.IS_DEBUG) {
									if (data.getString("lng", lng).equals(lng)) {
										Fx.log("exclude lang: " + lng);
									} else {
										Fx.log("exclude data: ");
										Fx.log(data);
									}
								}
							} else {
								if (Fx.IS_DEBUG) {
									Fx.log("null subpage scrap data: " + agg_link);
								}
								unDejaVu(sub_url);
								unDejaVu(agg_link);
							}
						}
					}
				}
			}
		}
		onResult.run(new Json("finish", true));
	}

	/**
	 * Regex @ separated for cleaning result
	 *
	 * @param cleaners Regex @ separated
	 * @param data     to clean
	 * @return data cleaned
	 */
	private static Json cleaner(String cleaners, Json data) {
		if (cleaners != null && !cleaners.equals("")) {
			try {
				String title = data.getString("title", "");
				String description = data.getString("description", "");
				for (String cleaner : cleaners.split("@")) {
					Pattern pattern = Pattern.compile(cleaner);
					title = title.replaceAll(pattern.pattern(), "");
					description = description.replaceAll(pattern.pattern(), "");
				}
				data.put("title", title);
				data.put("description", description);
			} catch (Exception e) {
				data.put("error", e.getMessage());
			}

		}
		return data;
	}


	/**
	 * Get youtube last videos contents
	 *
	 * @param onResult function to execute on result
	 * @param channel  where to get videos
	 * @param forums   where post video
	 * @param cleaner  regex for clean title
	 * @param excludes List of terms to exclude if in video snippets
	 * @param preview  is preview mode?
	 */
	private static void getYoutubeChannelLinks(OnScrapResult onResult, String channel, List<String> forums, String cleaner, List<String> excludes, boolean preview) {

		List<Json> videos = YouTubeApi.getChannelVideos(channel);
		if (videos == null) {
			return;
		}
		Collections.reverse(videos);
		for (Json video : videos) {
			Json snippet_short = video.getJson("snippet");
			if (snippet_short != null && snippet_short.getString("liveBroadcastContent", "").equals("none")) {
				String id = video.getJson("id").getString("videoId");
				String url = "https://www.youtube.com/watch?v=" + id;
				if (preview || !isDejaVu(url)) {
					Json snippet = YouTubeApi.getVideoSnippets(id);
					if (snippet == null) {
						return;
					}
					if (snippet != null) {
						if (Fx.IS_DEBUG) {
							Fx.log("get youtube scrap:" + url);
						}
						String title = StringEscapeUtils.unescapeXml(snippet.getString("title"));
						String description = StringEscapeUtils.unescapeXml(snippet.getText("description"));
						if (snippet.getString("channelId", "").equals(channel) && !isExclude(title, excludes) && !isExclude(description, excludes)) {
							try {
								Json thumbnails = snippet.getJson("thumbnails");
								Iterator<String> th_it = thumbnails.keySet().iterator();
								String logo = null;
								int width = 0;
								while (th_it.hasNext()) {
									Json thumb = thumbnails.getJson(th_it.next());
									if (thumb.getInteger("width", -1) > width) {
										width = thumb.getInteger("width");
										logo = thumb.getString("url");
									}
								}
								Json data = new Json()
										.put("url", url)
										.put("title", title)
										.put("description", description)
										.add("logos", logo)
										.put("forums", forums);

								data = cleaner(cleaner, data);
								if (!onResult.run(data)) {
									return;
								}

							} catch (Exception e) {
								if (Fx.IS_DEBUG) {
									e.printStackTrace();
								}
								if (!onResult.run(new Json("error", e.getMessage()))) {
									return;
								}
							}
						}
					}
				}
			}
		}

		onResult.run(new Json("finish", true));
	}

	/**
	 * Get snippets of a page
	 *
	 * @param url of the page
	 * @return Json contains snippets: title, abstract, url
	 */
	private static Json getPageData(String url, String cleaner) {
		String urlhtml = get(url);
		if (urlhtml == null) {
			Fx.log("error scrap get data: " + url);
			return null;
		}
		try {
			Document doc = Jsoup.parse(urlhtml, url);
			return ScrapDataUtils.parseData(doc, cleaner);
		} catch (Exception e) {
			return null;
		}


	}

	/**
	 * Is string contain exclude term ?
	 *
	 * @param str      to analyze
	 * @param excludes terms to exclude
	 * @return true if string contain a term to exclude
	 */
	private static boolean isExclude(String str, List<String> excludes) {
		if (excludes == null) {
			return false;
		}
		for (String exclude : excludes) {
			if (str.toLowerCase().contains(exclude)) {
				if (Fx.IS_DEBUG) {
					Fx.log("scrap exlude: " + exclude);
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * Is url accepted ?
	 *
	 * @param url      to test
	 * @param includes list of terms must be in the url
	 * @return true if url is include
	 */
	private static boolean isIncludeUrl(String url, List<String> includes) {
		if (includes == null) {
			return true;
		}
		for (String include : includes) {
			if (url.contains(include)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Exclude socials links
	 *
	 * @param url to analyze
	 * @return true if is excluded
	 */
	private static boolean isFixedExclude(String url) {
		return isExclude(url, Arrays.asList("twitter.com", "paypal.com", "//t.co", "wikipedia.org", "facebook.com", "linkedin.com", ".pdf", ".mp3"));
	}

	/**
	 * Is url already scanned ?
	 *
	 * @param url to search
	 * @return true if url is already scanned
	 */
	public static boolean isDejaVu(String url) {
		String url_id = url.substring(0, Math.min(url.length(), 1024));
		return Db.updateOne("DejaVu", Filters.eq("_id", url_id), new Json()
						.put("$inc", new Json("count", 1))
						.put("$setOnInsert", new Json("_id", url_id).put("date", new Date()))
				, new UpdateOptions().upsert(true)).getMatchedCount() > 0L;
	}

	/**
	 * Remove url from already scanned
	 *
	 * @param url to remove
	 */
	public static void unDejaVu(String url) {
		Db.deleteOne("DejaVu", Filters.eq("_id", url));
	}

	/**
	 * Is data snippet too short ?
	 *
	 * @param data to analyze
	 * @return true if data is too short
	 */
	private static boolean tooShort(Json data) {
		return data.getString("title", "").length() < 10 || data.getString("description", "").length() < 500;
	}

	/**
	 * Interface for execute on result
	 */
	public interface OnScrapResult {
		boolean run(Json data);
	}

}
