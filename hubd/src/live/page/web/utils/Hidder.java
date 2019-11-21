/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.utils;

import live.page.web.utils.json.Json;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class Hidder {


	public static String encodeString(String str) {
		try {
			BigInteger base = BigInteger.valueOf(62);
			byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
			BigInteger number = new BigInteger(bytes);
			if (BigInteger.ZERO.compareTo(number) == 0) {
				return "0";
			}
			BigInteger value = number.add(BigInteger.ZERO);
			StringBuilder sb = new StringBuilder();
			while (BigInteger.ZERO.compareTo(value) < 0) {
				BigInteger[] reminder = value.divideAndRemainder(base);
				int remainder = reminder[1].intValue();
				if (remainder < 10) {
					sb.insert(0, (char) (remainder + '0'));
				} else if (remainder < 10 + 26) {
					sb.insert(0, (char) (remainder + 'a' - 10));
				} else {
					sb.insert(0, (char) (remainder + 'A' - 10 - 26));
				}
				value = reminder[0];
			}
			return reverseString(sb.toString());
		} catch (Exception e) {
			return null;
		}
	}

	public static String decodeString(String enc) {
		try {
			String str = reverseString(enc);

			BigInteger base = BigInteger.valueOf(62);
			byte[] base62Bytes = str.getBytes(StandardCharsets.UTF_8);
			BigInteger rez = BigInteger.ZERO;
			BigInteger multiplier = BigInteger.ONE;
			for (int i = base62Bytes.length - 1; i >= 0; i--) {
				byte byt = base62Bytes[i];
				int alpha = byt - '0';
				if (Character.isLowerCase(byt)) {
					alpha = byt - ('a' - 10);
				} else if (Character.isUpperCase(byt)) {
					alpha = byt - ('A' - 10 - 26);
				}
				rez = rez.add(multiplier.multiply(BigInteger.valueOf(alpha)));
				multiplier = multiplier.multiply(base);
			}
			return new String(rez.toByteArray(), StandardCharsets.UTF_8);
		} catch (Exception e) {
			return null;
		}
	}

	public static String encodeDate(Date date) {
		if (date == null) {
			return null;
		}
		return encodeString(new SimpleDateFormat("HHmmssYYMMdd").format(date)).toLowerCase();
	}


	private static String reverseString(String str) {
		if (str.length() == 1) {
			return str;
		}
		return str.charAt(str.length() - 1) + reverseString(str.substring(0, str.length() - 1));
	}

	public static String encodeJson(Json paging) {
		if (paging == null || paging.isEmpty()) {
			return null;
		}
		try {
			String str = paging.toString(true);
			BigInteger base = BigInteger.valueOf(62);
			byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
			BigInteger number = new BigInteger(bytes);
			if (BigInteger.ZERO.compareTo(number) == 0) {
				return "0";
			}
			BigInteger value = number.add(BigInteger.ZERO);
			StringBuilder sb = new StringBuilder();
			while (BigInteger.ZERO.compareTo(value) < 0) {
				BigInteger[] reminder = value.divideAndRemainder(base);
				int remainder = reminder[1].intValue();
				if (remainder < 10) {
					sb.insert(0, (char) (remainder + '0'));
				} else if (remainder < 10 + 26) {
					sb.insert(0, (char) (remainder + 'a' - 10));
				} else {
					sb.insert(0, (char) (remainder + 'A' - 10 - 26));
				}
				value = reminder[0];
			}
			return reverseString(sb.toString());
		} catch (Exception e) {
			return null;
		}
	}

	public static Json decodeDecode(String paging) {
		if (paging == null || paging.equals("")) {
			return null;
		}

		try {
			String str = reverseString(paging);
			BigInteger base = BigInteger.valueOf(62);
			byte[] base62Bytes = str.getBytes(StandardCharsets.UTF_8);
			BigInteger rez = BigInteger.ZERO;
			BigInteger multiplier = BigInteger.ONE;
			for (int i = base62Bytes.length - 1; i >= 0; i--) {
				byte byt = base62Bytes[i];
				int alpha = byt - '0';
				if (Character.isLowerCase(byt)) {
					alpha = byt - ('a' - 10);
				} else if (Character.isUpperCase(byt)) {
					alpha = byt - ('A' - 10 - 26);
				}
				rez = rez.add(multiplier.multiply(BigInteger.valueOf(alpha)));
				multiplier = multiplier.multiply(base);
			}
			Json dec = new Json(new String(rez.toByteArray(), StandardCharsets.UTF_8));

			if (dec == null || dec.isEmpty()) {
				return null;
			}
			if (dec.containsKey("id")) {
				dec.put("_id", dec.getId());
				dec.remove("id");
			}
			for (String key : Arrays.asList("date", "update", "archived", "join", "last", "remove", "last.date")) {
				if (dec.containsKey(key)) {
					dec.set(key, dec.parseDate(key));
				}
			}

			return dec;
		} catch (Exception e) {
			return null;
		}
	}
}
