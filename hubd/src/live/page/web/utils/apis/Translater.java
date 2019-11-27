/*
 * Copyright (c) 2019. PAGE and Sons
 */
package live.page.web.utils.apis;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import live.page.web.system.Settings;
import live.page.web.utils.Fx;

import java.io.FileInputStream;
import java.io.InputStream;

public class Translater {
	/**
	 * Google Api Translation
	 * @param str to translate
	 * @param source_lng source language
	 * @param destination_lng destination language
	 * @return the translation
	 */
	public static String translate(String str, String source_lng, String destination_lng) {
		if (str == null || str.equals("") || str.length() <= 1) {
			return str;
		}
		InputStream props_stream;
		try {
			String file = "/res/.translate.json";
			props_stream = new FileInputStream(Settings.HUB_REPO + file);
			if (props_stream == null) {
				file = "/res/translate.json";
				props_stream = Translater.class.getResourceAsStream(file);
			}
			if (props_stream == null) {
				Fx.log("Translate need /res/translate.json from Google Translation service");
				return null;
			}
			Translation translation = TranslateOptions.newBuilder().setCredentials(
					GoogleCredentials.fromStream(props_stream))
					.setTargetLanguage(destination_lng).build().getService().translate(str, Translate.TranslateOption.sourceLanguage(source_lng));
			return translation.getTranslatedText();
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
}
