/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.cosmetic.tmpl;

import live.page.hubd.system.json.Json;
import live.page.hubd.system.utils.Fx;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FxTemplate {

    public static String isoDate(Date date) {
        if (date == null) {
            return null;
        }
        return Fx.dateFormater.format(date);
    }

    public static int textWidth(String str) {
        return str.length() - (str.replaceAll("[^iIl1\\.,']", "").length() / 2);
    }

    public static List<?> reverse(List<?> ele) {
        Collections.reverse(ele);
        return ele;
    }

    public static String truncate(String chaine, int length) {
        if (chaine == null) {
            return null;
        }
        return Fx.truncate(chaine, length);
    }

    public static String escape(String chaine) {
        if (chaine == null) {
            return "";
        }
        return chaine.replace("\"", "&#34;");
    }

    public static String clean(String chaine, int length) {
        if (chaine == null) {
            return null;
        }
        String clean = chaine.replaceAll("\\[([^]]+)]", "").replaceAll("\n", " ").replaceAll("([\\s]+)", " ");
        return Fx.truncate(clean, length);
    }

    public static String getDomain(String url) {

        try {
            return new URL(url).getHost();
        } catch (final Exception e) {
            return null;
        }
    }

    public static String moneyNumber(double input, String lang) {

        try {
            return NumberFormat.getNumberInstance(new Locale(lang)).format(input);
        } catch (final Exception e) {
            return null;
        }
    }

    public static int middle(int num) {
        try {
            return Math.round(num / 2);
        } catch (final Exception e) {
            return -1;
        }
    }

    public static double round(double num) {
        try {
            return Math.round(num);
        } catch (final Exception e) {
            return -1;
        }
    }

    public static double mstokmh(double num) {
        try {
            final double kph = (num / 1000D) / (1D / 3600D);
            return Math.round(kph);
        } catch (final Exception e) {
            return -1;
        }
    }

    public static String ucfirst(String chaine) {
        String out = chaine;

        try {
            out = chaine.substring(0, 1).toUpperCase() + chaine.substring(1);
        } catch (final Exception e) {
        }
        return out;
    }

    public static String textbrut(String chaine) {

        return chaine.replaceAll("</?([^>]+)>", "");
    }

    public static List<Json> jumper(String text) {
        if (text == null) {
            return new ArrayList<>();
        }
        text = text.replace("’", "'");
        text = "\n" + text + "\n";
        Pattern pat = Pattern.compile("[\r\n]([ ]+)?([=]{1,4})([ ]+)?([^=]+)([ ]+)?([=]{1,4})([ ]+)?[\r\n]");
        Matcher matches = pat.matcher(text);
        List<Json> anchors = new ArrayList<>();
        while (matches.find()) {
            Json anchor = new Json();
            anchor.put("title", matches.group(4));
            anchor.put("link", "#" + Fx.cleanURL(matches.group(4)).replace("-", "_"));
            anchors.add(anchor);
        }
        return anchors;
    }

    public static String avatar(String src) {
        return avatar(src, false);
    }

    public static String avatar(String src, boolean itemprop) {
        String prod = "<picture>";
        prod += "<source width=\"100\" height=\"100\" srcset=\"" + src + "@100x100\" media=\"(min-width: 701px) and (max-width: 900px)\" />";
        prod += "<source width=\"38\" height=\"38\" srcset=\"" + src + "@38x38\" media=\"(max-width: 700px)\" />";
        prod += "<img alt=\"avatar\" ";
        if (itemprop) {
            prod += "itemprop=\"image\" ";
        }
        prod += "src=\"" + src + "@55x55\" width=\"55\" height=\"55\" />";
        prod += "</picture>";
        return prod;
    }

    public static String author(List<Json> users, String and) {
        StringBuilder output = new StringBuilder();
        List<Json> clean_users = new ArrayList<>();
        for (Json user : users) {
            if (user.getId() != null) {
                clean_users.add(user);
            }
        }

        String vir = "";
        for (int i = 0; i < clean_users.size(); i++) {
            Json user = clean_users.get(i);
            output.append((clean_users.size() > 1 && i == clean_users.size() - 1) ? and : vir);
            output.append("<span itemprop=\"author\" itemscope itemtype=\"https://schema.org/Person\"><a itemprop=\"url\" href=\"/users/").append(user.getId()).append("\"><span itemprop=\"name\">").append(user.getString("name")).append("</span></a></span>");
            vir = ", ";

        }
        return output.toString();

    }

    public static String urlencode(String str) {
        try {
            return URLEncoder.encode(str, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return str;
        }
    }

    public static String baseSite(String url) {
        try {
            URL url_ = new URL(url);

            return url_.getProtocol() + "://" + url_.getHost();
        } catch (Exception e) {
            return null;
        }
    }

    public static String domain(String url) {
        try {
            URL url_ = new URL(url);
            return url_.getHost().replace("www.", "");
        } catch (Exception e) {
            return null;
        }
    }

    public static String video(String url) {

        Map<String, Pattern> tubes = new HashMap<>();
        tubes.put("https://www.youtube.com/embed/",
                Pattern.compile("(?:https?://)?(?:youtu\\.be/|(?:www\\.)?youtube\\.com/(?:watch(?:\\.php)?\\?.*v=|v/|embed/))([a-zA-Z0-9\\-_]+)"));
        tubes.put("https://www.dailymotion.com/embed/video/",
                Pattern.compile("(?:https?://)(?:www.)?(?:dailymotion.com\\/(?:video|hub)|dai.ly)/([^_ <]+)"));
        tubes.put("https://player.vimeo.com/video/",
                Pattern.compile("(?:https?://)(?:www\\.|player\\.)?vimeo.com/([0-9]+)"));
        tubes.put("https://ok.ru/videoembed/",
                Pattern.compile("(?:https?://)(?:m\\.)?ok.ru/(?:video/)?([0-9]+)"));

        for (Map.Entry<String, Pattern> item : tubes.entrySet()) {
            if (url.matches(item.getValue().pattern())) {
                return url.replaceFirst(item.getValue().pattern(),
                        "<iframe class=\"video\" width=\"560\" height=\"315\" src=\"" + item.getKey() + "$1\" frameborder=\"0\"  allow=\"autoplay\" referrerpolicy=\"no-referrer\" allowfullscreen></iframe>"
                );
            }
        }
        return null;
    }

}
