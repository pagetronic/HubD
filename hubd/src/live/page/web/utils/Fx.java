/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.utils;

import live.page.web.db.Db;
import live.page.web.utils.json.Json;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.safety.Whitelist;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class Fx {

	public static final boolean IS_DEBUG = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toString().contains("-agentlib:jdwp");
	public static final String ISO_DATE = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"; // Warning.. Z necessary for next

	private static SecureRandom random = new SecureRandom(Settings.SALT.getBytes());

	public static String getSecureKey() {
		String big = new BigInteger(280, random).toString(Character.MAX_RADIX);
		big = big.substring(0, 52);
		return big.toUpperCase();
	}

	public static String getUnique() {
		return RandomStringUtils.randomAlphabetic(8).toLowerCase();
	}

	public static List<File> listFiles(String dir, String... exts) {
		List<String> extsl = Arrays.asList(exts);
		List<File> files = new ArrayList<>();
		for (File file : new File(dir).listFiles()) {

			if (file.isDirectory()) {
				files.addAll(listFiles(file.getAbsolutePath(), exts));
			} else if (extsl.contains(file.getName().replaceFirst(".*\\.(" + StringUtils.join(extsl, "|") + ")$", "$1"))) {
				files.add(file);
			}

		}
		files.sort(Comparator.comparing(str -> {
			try {
				return str.getCanonicalPath();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}));

		return files;
	}

	public static List<String> listFilesNames(String dir, String... ext) {

		List<String> files = new ArrayList<>();
		for (File file : listFiles(dir, ext)) {
			files.add(file.getAbsolutePath());
		}
		return files;
	}

	public static String getResource(String res) {
		try {
			InputStream sr = Thread.currentThread().getContextClassLoader().getResourceAsStream(res);
			String str = Streams.asString(sr, StandardCharsets.UTF_8.displayName());
			sr.close();
			return str;
		} catch (Exception e) {
			return null;
		}
	}


	public static String couper(String chaine, int length) {
		if (chaine == null) {
			return null;
		}
		if (textWidth(chaine) <= length) {
			return chaine;
		}

		int end = chaine.lastIndexOf(' ', length - 3);

		if (end == -1) {
			return chaine.substring(0, length - 3) + "&#8230;";
		}
		int newEnd = end;
		do {
			end = newEnd;
			newEnd = chaine.indexOf(' ', end + 1);

			if (newEnd == -1) {
				newEnd = chaine.length();
			}

		} while (textWidth(chaine.substring(0, newEnd) + "&#8230;") < length);

		return chaine.substring(0, end) + "&#8230;";
	}

	public static String textbrut(String chaine) {
		if (chaine == null) {
			return null;
		}
		return Jsoup.parse(chaine).text().replace("\"", "&#34;");
	}

	private static int textWidth(String chaine) {
		if (chaine == null) {
			return 0;
		} else {
			return chaine.length();
		}
	}

	public static String ucfirst(String chaine) {
		if (chaine == null || chaine.equals("")) {
			return chaine;
		}
		return chaine.substring(0, 1).toUpperCase() + chaine.substring(1).toLowerCase();
	}

	public static String normalize(String text) {
		if (text == null) {
			return null;
		}
		return Normalizer.normalize(text, Normalizer.Form.NFKC);
	}

	public static String normalizePost(String text) {
		if (text == null) {
			return null;
		}
		text = Normalizer.normalize(text, Normalizer.Form.NFKC);
		text = Jsoup.clean(text, "/", new Whitelist().addTags("em", "strong", "br", "a").addAttributes("a", "href"), new OutputSettings().prettyPrint(false).outline(true).charset(StandardCharsets.UTF_8));

		text = text.replaceFirst("([ \n]+)$", "");
		return text;
	}

	public static String cleanSpaces(String str) {
		return str.replaceAll("([ ]{2,})", " ").replaceAll("^ ", "").replaceAll(" $", "");
	}

	public static String cleanURL(String title) {
		if (title == null) {
			return null;
		}
		try {
			title = title.replace("?", "");
			title = Normalizer.normalize(title, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
			title = title.replaceAll("([\\p{javaWhitespace}]+)", "-");
			title = title.replaceAll("([-]+)", "-");
			title = title.replaceAll("([.]+)", "");

			title = title.replaceAll("([()]+)", "").replaceAll("^ ?(.*) ?$", "$1").replaceAll("[ ':,;]+", "-");
			title = title.replaceAll("^[\\-]+", "").replaceAll("[\\-]+$", "");
			title = URLEncoder.encode(title, StandardCharsets.UTF_8);

			return title.toLowerCase();
		} catch (Exception e) {
			return "~~~error~~~";
		}
	}

	public static String cleanDecode(String title) {
		try {
			String clean = URLDecoder.decode(title, StandardCharsets.UTF_8);
			clean = clean.replace("_", "-");
			clean = clean.toLowerCase();
			String[] find = "àáâãäåòóôõöøèéêëçìíîïùúûüÿñ’/".split("");
			String[] replace = "aaaaaaooooooeeeeciiiiuuuuyn--".split("");
			for (int i = 0; i < find.length; i++) {
				clean = clean.replace(find[i], replace[i]);
			}
			clean = clean.replace("œ", "oe");
			clean = clean.replaceAll("[\\-]+$", "-");
			return clean;
		} catch (Exception e) {
			return "~~~error~~~";
		}
	}

	public static Date UTCDate() {
		return Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime();
	}


	public static void shutdownService(ExecutorService service) {
		if (service != null && !service.isTerminated()) {
			service.shutdown();
			try {
				service.awaitTermination(5, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				return;
			}
			if (!service.isTerminated()) {
				service.shutdownNow();
			}
		}
	}

	public static String crypt(String password) {
		return DigestUtils.sha256Hex(Settings.SALT + password + Settings.SALT);
	}

	public static String md5(String str) {
		return DigestUtils.md5Hex(str).toUpperCase();
	}

	public static void log(Object log) {
		log(log, null);
	}

	public static void log(Object log, Class<?> cls) {
		try {
			if (IS_DEBUG) {
				System.out.println(ConsoleColors.YELLOW_BACKGROUND_BRIGHT + ConsoleColors.BLACK_BOLD + log.toString() + ConsoleColors.RESET);
			} else {
				Db.save("SysLog", new Json("date", new Date()).put("message", log).put("class", cls == null ? null : cls.getName()));
				System.out.println(log.toString());

			}
		} catch (Exception e) {
		}
	}

}
