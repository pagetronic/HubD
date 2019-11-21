/*
 * Copyright (c) 2019. PAGE and Sons
 */
package live.page.web.utils.google;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import live.page.web.utils.Fx;

public class Translater {
	public static String translate(String str, String source_lng, String destination_lng) {
		if (str == null || str.equals("") || str.length() <= 1) {
			return str;
		}
		try {
			Translation translation = TranslateOptions.newBuilder().setCredentials(
					GoogleCredentials.fromStream(Fx.class.getResourceAsStream("/res/translate.json")))
					.setTargetLanguage(destination_lng).build().getService().translate(str, Translate.TranslateOption.sourceLanguage(source_lng));
			return translation.getTranslatedText();
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
}
