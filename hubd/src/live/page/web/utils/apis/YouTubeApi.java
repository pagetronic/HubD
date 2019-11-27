/*
 * Copyright (c) 2019. PAGE and Sons
 */
package live.page.web.utils.apis;

import live.page.web.system.Settings;
import live.page.web.system.json.Json;
import live.page.web.utils.Fx;
import live.page.web.utils.http.HttpClient;

import java.util.List;

public class YouTubeApi {

	/**
	 * Get the title of a channel
	 *
	 * @param channel to search
	 * @return title of the channel
	 */
	public static String getChannelTitle(String channel) {
		String url = "https://www.googleapis.com/youtube/v3/search?part=snippet&channelId=" + channel + "&maxResults=10&order=date&type=video&key=" + Settings.YOUTUBE_API_KEY;
		Json api = new Json(HttpClient.get(url));
		if (api.containsKey("error")) {
			Fx.log(api);
			return null;
		}
		for (Json item : api.getListJson("items")) {
			Json snippet = item.getJson("snippet");
			if (snippet.getString("channelId", "").equals(channel)) {
				return snippet.getString("channelTitle");
			}
		}

		return null;
	}

	/**
	 * Get last video of a channel
	 *
	 * @param channel to search
	 * @return last video as Json List
	 */
	public static List<Json> getChannelVideos(String channel) {
		String url = "https://www.googleapis.com/youtube/v3/search?part=snippet&channelId=" + channel + "&maxResults=50&order=date&type=video&key=" + Settings.YOUTUBE_API_KEY;
		Json api = new Json(HttpClient.get(url));
		if (api.containsKey("error")) {
			Fx.log(api);
			return null;
		}
		return api.getListJson("items");
	}

	/**
	 * Get a video information
	 *
	 * @param id of the video
	 * @return the information of the video
	 */
	public static Json getVideoSnippets(String id) {
		String url = "https://www.googleapis.com/youtube/v3/videos?part=snippet&id=" + id + "&key=" + Settings.YOUTUBE_API_KEY;
		Json api = new Json(HttpClient.get(url));
		if (api.containsKey("error")) {
			Fx.log(api);
			return null;
		}
		Json video = api.getListJson("items").get(0);
		return video.getJson("snippet");
	}

}
