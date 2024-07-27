/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.cosmetic.tmpl.parsers;

import live.page.hubd.system.Settings;
import live.page.hubd.system.json.Json;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.io.Writer;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostParser extends Directive {

    public static String parse(String text, List<Json> docs, List<Json> links) {

        text = text.replaceAll("\n\n", "</p><p>");
        text = text.replaceAll("\n", "<br/>");
        text = "<p>" + text + "</p>";
        text = text.replaceAll("\\[bold](.+?)\\[/bold]", "<strong>$1</strong>");
        text = text.replaceAll("\\[italic](.+?)\\[/italic]", "<em>$1</em>");
        text = text.replaceAll("\\[quote](.+?)\\[/quote]", "<blockquote>$1</blockquote>");

        Pattern pattern = Pattern.compile("\\[url=?([^]]+)?](.+?)\\[/url]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String text_ = matcher.group(2).replace("#", "@~X@X~@");
            String url_ = (matcher.group(1) == null) ? text_ : matcher.group(1).replace("#", "@~X@X~@");
            text = text.replace(matcher.group(0), "<a href=\"" + url_.replace("#", "@~X@X~@") + "\">" + text_ + "</a>");
        }

        text = Jsoup.clean(text, new Safelist().addTags("strong", "a", "p", "br", "em", "blockquote").addAttributes("a", "href"));
        text = text.replaceAll("<[a-z]></[a-z]>", "");

        text = text.replaceAll("(\n+)", "\n").replace("  ", "\t").replace("\t ", "\t");
        text = text.replaceAll("[\t ]+<p></p>\n", "");

        text = video(text);

        text = hasher(text);

        text = docs(text, docs != null ? docs : new ArrayList<>());

        text = text.replace("@~X@X~@", "#");

        return text;
    }

    private static String docs(String text, List<Json> docs) {

        if (docs == null) {
            return text;
        }
        Pattern pattern = Pattern.compile("\\[Photos\\(([a-z0-9]+)\\)\\|?([^]]+)?]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        List<Json> new_docs = new ArrayList<>(docs);

        while (matcher.find()) {
            String key = matcher.group(1);
            StringBuilder info = new StringBuilder();

            for (Json doc : docs) {
                if (doc.getId().equals(key)) {
                    new_docs.remove(doc);
                    if (!doc.getString("text", "").isEmpty()) {
                        info.append("<span>").append(doc.getString("text", "")).append("</span>");
                    }
                }
            }

            if (matcher.group(2) != null && !matcher.group(2).replaceAll("( +)", "").isEmpty()) {
                info.append("<figcaption>").append(matcher.group(2)).append("</figcaption>");
            }
            text = text.replace(matcher.group(),
                    "<figure class=\"img\">" +
                            "<a href=\"" + Settings.getCDNHttp() + "/files/" + key + "\">" +
                            "<picture>" +
                            "<source srcset=\"" + Settings.getCDNHttp() + "/files/" + key + "@325\" media=\"(min-width: 0px) and (max-width: 361px)\">" +
                            "<img src=\"" + Settings.getCDNHttp() + "/files/" + key + "@500\" />" +
                            info +
                            "</picture>" +
                            "</a>" +
                            "</figure>");
        }

        docs.clear();
        docs.addAll(new_docs);
        return text;

    }

    public static String video(String text) {
        Map<String, Pattern> tubes = new HashMap<>();
        tubes.put("https://www.youtube.com/embed/",
                Pattern.compile("(?:https?://)?(?:youtu\\.be/|(?:www\\.)?youtube\\.com/(?:watch(?:\\.php)?\\?.*v=|v/|embed/))([a-zA-Z0-9\\-_]+)"));
        tubes.put("https://www.dailymotion.com/embed/video/",
                Pattern.compile("https?://(?:www.)?(?:dailymotion.com/(?:video|hub)|dai.ly)/([^_ <]+)"));
        tubes.put("https://player.vimeo.com/video/",
                Pattern.compile("https?://(?:www\\.|player\\.)?vimeo.com/([0-9]+)"));
        tubes.put("https://ok.ru/videoembed/",
                Pattern.compile("https?://(?:m\\.)?ok.ru/(?:video/)?([0-9]+)"));

        for (Map.Entry<String, Pattern> item : tubes.entrySet()) {
            text = text.replaceAll(item.getValue().pattern(),
                    "<iframe class=\"video\" width=\"560\" height=\"315\" src=\"" + item.getKey() + "$1\" frameborder=\"0\"  allow=\"autoplay\" referrerpolicy=\"no-referrer\" allowfullscreen></iframe>"
            );
        }
        return text;
    }

    private static String hasher(String text) {
        String punct = "?!¿.,;:";
        Pattern pattern = Pattern.compile("#([^\\p{javaWhitespace}" + punct + "]+)([\\p{javaWhitespace}" + punct + "])?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            try {
                text = text.replace(matcher.group(), "<a href=\"/hashtag/" + URLEncoder.encode(matcher.group(1).toLowerCase(), StandardCharsets.UTF_8) + "\">#" + matcher.group(1).replace("_", " ") + "</a>" + (matcher.group(2) != null ? matcher.group(2) : ""));
            } catch (Exception ignored) {

            }
        }
        return text;
    }

    @Override
    public String getName() {
        return "parsepost";
    }

    @Override
    public int getType() {
        return LINE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean render(InternalContextAdapter context, Writer writer, Node node) {
        try {
            Object textobj = node.jjtGetChild(0).value(context);
            if (textobj == null) {
                return true;
            }
            List<Json> docs = new ArrayList<>();
            List<Json> links = new ArrayList<>();
            if (node.jjtGetNumChildren() > 1) {
                docs = (List<Json>) node.jjtGetChild(1).value(context);
            }
            if (node.jjtGetNumChildren() > 2) {
                links = (List<Json>) node.jjtGetChild(2).value(context);
            }
            String text = String.valueOf(textobj);
            text = parse(text, docs, links);
            writer.write(text);

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
