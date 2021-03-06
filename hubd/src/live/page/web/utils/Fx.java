/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.utils;

import live.page.web.system.Settings;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class Fx {

	/**
	 * Test if system is in debug mode
	 */
	public static final boolean IS_DEBUG = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toString().contains("-agentlib:jdwp");

	/**
	 * Date format for JSON
	 */
	public static final String ISO_DATE = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"; // Warning.. Z necessary for next

	private static final SecureRandom random = new SecureRandom(Fx.getUnique().getBytes());

	/**
	 * Generate a Secure key
	 *
	 * @return a random secure key
	 */
	public static String getSecureKey() {
		String big = new BigInteger(280, random).toString(Character.MAX_RADIX);
		big = big.substring(0, 52);
		return big.toUpperCase();
	}

	/**
	 * Generate an unique key
	 *
	 * @return unique key
	 */
	public static String getUnique() {
		return RandomStringUtils.randomAlphabetic(8).toLowerCase();
	}

	/**
	 * List Files recursively by extension in a specific folder
	 *
	 * @param directory  where search recursively
	 * @param extensions to search
	 * @return a List of File
	 */
	public static List<File> listFiles(String directory, String... extensions) {
		List<String> extsl = Arrays.asList(extensions);
		List<File> files = new ArrayList<>();
		for (File file : new File(directory).listFiles()) {

			if (file.isDirectory()) {
				files.addAll(listFiles(file.getAbsolutePath(), extensions));
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

	/**
	 * List filenames recursively by extension in a specific folder
	 *
	 * @param directory  where search recursively
	 * @param extensions to search
	 * @return a List of String filenames
	 */
	public static List<String> listFilesNames(String directory, String... extensions) {

		List<String> files = new ArrayList<>();
		for (File file : listFiles(directory, extensions)) {
			files.add(file.getAbsolutePath());
		}
		return files;
	}

	/**
	 * Read a resource as String
	 *
	 * @param location of the resource
	 * @return String representing the content of the resource
	 */
	public static String getResource(String location) {
		try {
			InputStream sr = Thread.currentThread().getContextClassLoader().getResourceAsStream(location);
			String str = Streams.asString(sr, StandardCharsets.UTF_8.displayName());
			sr.close();
			return str;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Truncate a String
	 *
	 * @param str    string to truncate
	 * @param length of the output
	 * @return the string truncated
	 */
	public static String truncate(String str, int length) {
		if (str == null) {
			return null;
		}
		if (str.length() <= length) {
			return str;
		}

		int end = str.lastIndexOf(' ', length - 3);

		if (end == -1) {
			return str.substring(0, length - 3) + "&#8230;";
		}
		int newEnd = end;
		do {
			end = newEnd;
			newEnd = str.indexOf(' ', end + 1);

			if (newEnd == -1) {
				newEnd = str.length();
			}

		} while ((str.substring(0, newEnd) + "&#8230;").length() < length);

		return str.substring(0, end) + "&#8230;";
	}

	/**
	 * Strip tag of a html string
	 *
	 * @param html to process
	 * @return a string without html tags
	 */
	public static String textbrut(String html) {
		if (html == null) {
			return null;
		}
		return Jsoup.parse(html).text().replace("\"", "&#34;");
	}

	/**
	 * Capitalize string
	 *
	 * @param str to capitalize
	 * @return a capitalized string
	 */
	public static String ucfirst(String str) {
		if (str == null || str.equals("")) {
			return str;
		}
		return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
	}

	/**
	 * Normalize a sequence of text.
	 *
	 * @param text to normalize
	 * @return the text normalized
	 */
	public static String normalize(String text) {
		if (text == null) {
			return null;
		}
		return Normalizer.normalize(text, Normalizer.Form.NFKC);
	}

	/**
	 * Normalize a sequence of char values and strip strange tags
	 *
	 * @param text to normalize
	 * @return the text normalized
	 */
	public static String normalizePost(String text) {
		if (text == null) {
			return null;
		}
		text = Normalizer.normalize(text, Normalizer.Form.NFKC);
		text = Jsoup.clean(text, "/", new Whitelist().addTags("em", "strong", "br", "a").addAttributes("a", "href"), new OutputSettings().prettyPrint(false).outline(true).charset(StandardCharsets.UTF_8));

		text = text.replaceFirst("([ \n]+)$", "");
		return text;
	}

	/**
	 * Clean a string to use as URL
	 *
	 * @param str to clean
	 * @return an url
	 */
	public static String cleanURL(String str) {
		if (str == null) {
			return null;
		}
		try {
			str = str.replace("?", "");
			str = Normalizer.normalize(str, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
			str = str.replaceAll("([\\p{javaWhitespace}]+)", "-");
			str = str.replaceAll("([-]+)", "-");
			str = str.replaceAll("([.]+)", "");

			str = str.replaceAll("([()]+)", "").replaceAll("^ ?(.*) ?$", "$1").replaceAll("[ ':,;]+", "-");
			str = str.replaceAll("^[\\-]+", "").replaceAll("[\\-]+$", "");
			str = URLEncoder.encode(str, StandardCharsets.UTF_8);

			return str.toLowerCase();
		} catch (Exception e) {
			return "~~~error~~~";
		}
	}

	/**
	 * Get Date at UTC time zone
	 *
	 * @return date at UTC time zone
	 */
	public static Date UTCDate() {
		return Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime();
	}

	/**
	 * Shutdown correctly an executorService
	 *
	 * @param service to shutdown
	 */
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

	/**
	 * Crypt a string/password
	 *
	 * @param password to crypt
	 * @return password encrypted
	 */
	public static String crypt(String password) {
		return DigestUtils.sha256Hex(Settings.SALT + password + Settings.SALT);
	}

	/**
	 * Print string to console
	 *
	 * @param log object to display in console
	 */
	public static void log(Object log) {
		if (log == null) {
			log("null");
			return;
		}
		try {
			System.out.println(log.toString());
		} catch (Exception ignore) {
		}
	}

}
