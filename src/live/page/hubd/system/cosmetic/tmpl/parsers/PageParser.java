/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.cosmetic.tmpl.parsers;

import live.page.hubd.system.Settings;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.utils.Fx;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PageParser extends Directive {

    private static String parse(Json page, List<Json> links) {

        String text = page.getText("text");
        text = text.replace("’", "'");

        if (text == null) {
            return null;
        }
        text = parseWiki(text);
        text = sectionNizer(text);
        text = parsePhotos(text);
        text = clean(text);
        text = PostParser.video(text);
        return text;
    }

    private static String insertPub(String text, String pub) {
        if (text == null) {
            return "";
        }
        pub += "\n";
        Pattern p = Pattern.compile("(<section[^>]+>)", Pattern.MULTILINE);

        String[] spl = p.split(text);
        if (spl.length < 3) {
            return text;
        }
        double poz = Math.floor(spl.length / 2);
        Matcher m = p.matcher(text);
        while (m.find()) {
            if (poz-- == 0D) {
                text = text.substring(0, m.start()) + pub + text.substring(m.start());
            }
        }
        return text;
    }

    private static String clean(String text) {
        text = text.replaceAll("([\n]+)", "\n").replace("  ", "\t").replace("\t ", "\t");
        text = text.replaceAll("[\t ]+<p></p>\n", "");
        return text;
    }

    private static String parseWiki(String text) {
        text = text.replaceAll("[\r]", "").replaceAll("( ){2,}", " ");

        StringWriter parsed_text_oul = new StringWriter();
        String[] text_list = text.split("\n");
        boolean ul_opened = false;
        boolean ol_opened = false;
        for (String line : text_list) {
            if (line.startsWith("*")) {
                if (!ul_opened) {
                    parsed_text_oul.write("<ul>\n");
                }
                ul_opened = true;
                parsed_text_oul.write("<li>" + line.substring(1) + "</li>\n");
            } else if (line.startsWith("#")) {
                if (!ol_opened) {
                    parsed_text_oul.write("<ol>\n");
                }
                ol_opened = true;
                parsed_text_oul.write("<li>" + line.substring(1) + "</li>\n");

            } else {
                if (ol_opened) {
                    parsed_text_oul.write("</ol>\n\n");
                    ol_opened = false;
                }
                if (ul_opened) {
                    parsed_text_oul.write("</ul>\n\n");
                    ul_opened = false;
                }
                parsed_text_oul.write(line + "\n");
            }
        }
        text = parsed_text_oul.toString();

        String[] text_lines = text.split("([\\s\\u0085\\p{Z}]+){2,}");
        StringWriter parsed_text_hp = new StringWriter();
        for (String line : text_lines) {
            Matcher mh1 = Pattern.compile("^([\r\n]+)?([ ]+)?= ?([^=]+) ?=([ ]+)?([\r\n]+)?$").matcher(line);
            line = mh1.replaceAll("<h1>$3</h1>");
            if (!mh1.matches()) {
                Matcher mh2 = Pattern.compile("^([\r\n]+)?([ ]+)?== ?([^=]+) ?==([ ]+)?([\r\n]+)?$").matcher(line);
                line = mh2.replaceAll("<h2>$3</h2>");
                if (!mh2.matches()) {
                    Matcher mh3 = Pattern.compile("^([\n\n]+)?([ ]+)?=== ?([^=]+) ?===([ ]+)?([\r\n]+)?$").matcher(line);
                    line = mh3.replaceAll("<h3>$3</h3>");
                    if (!mh3.matches()) {
                        Matcher mh4 = Pattern.compile("^([\n\n]+)?([ ]+)?==== ?([^=]+) ?====([ ]+)?([\r\n]+)?$").matcher(line);
                        line = mh4.replaceAll("<h4>$3</h4>");
                        if (!mh4.matches()) {
                            if (!line.isEmpty()) {
                                line = "<p>" + line + "</p>";
                            }
                            line = line.replaceAll("([a-zA-Z0-9.;, ]+)\n", "$1<br/>");
                        }
                    }
                }
            }

            parsed_text_hp.write(line.replace(" </", "</") + "\n\n");
        }
        text = parsed_text_hp.toString();


        return text;

    }

    private static String sectionNizer(String text) {
        Document html = Jsoup.parse(text);
        int i = 1;
        for (int hn = 6; hn >= 1; hn--) {
            Iterator<Element> elements = html.select("body > h" + hn).iterator();
            while (elements.hasNext()) {
                Element hx = elements.next();
                hx.attr("itemprop", "alternateName");
                Element section = new Element(Tag.valueOf("section"), "");
                section.attr("itemprop", "articleSection");
                hx.before(section);
                Element next = hx.nextElementSibling();
                while ((next != null) && !next.tagName().matches("(h[1-6])")) {
                    section.appendChild(next);
                    next = hx.nextElementSibling();
                }
                section.prependChild(hx);
            }
        }

        Iterator<Element> sections_it = html.select("section").iterator();
        while (sections_it.hasNext()) {
            Element section = sections_it.next();
            Element child = section.children().first();
            if ((child != null) && child.tagName().matches("(h[1-6])")) {
                String id = Fx.cleanURL(child.text()).replace("-", "_");
                child.before(new Element(Tag.valueOf("a"), "").attr("name", id));
            }
        }

        Iterator<Element> elements = html.select("body > *").iterator();
        Element section = null;
        while (elements.hasNext()) {
            Element element = elements.next();
            if (element.tagName().equals("section")) {
                section = null;
            } else {
                if (section == null) {
                    section = new Element(Tag.valueOf("section"), "");
                    element.before(section);
                    section.appendChild(element);
                } else {
                    section.appendChild(element);
                }
            }

        }
        text = html.select("body").html();
        return text;
    }

    private static String parsePhotos(String text) {

        int width = 360;
        int height = 240;
        Pattern pattern = Pattern.compile("\\[Photos\\(([a-z0-9]+)\\)\\|?([^]]+)?]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {

            String key = matcher.group(1);
            String info = matcher.group(2) != null ? matcher.group(2) : "";
            text = text.replace(matcher.group(), "\n<div class=\"img\">\n" + "<a href=\"" + Settings.getCDNHttp() + "/files/" + key + "\">" + "<img width=\"" + width + "\" height=\"" + height + "\" alt=\"" + info + "\" src=\"" + Settings.getCDNHttp() + "/files/" + key + "@" + width + "x" + height + ".jpg\" />" + "</a>\n<legend>" + info + "</legend>\n</div>\n");
        }
        return text;
    }

    @Override
    public String getName() {
        return "parsepage";
    }

    @Override
    public int getType() {
        return LINE;
    }

    @Override
    public boolean render(InternalContextAdapter context, Writer writer, Node node) {
        try {
            Json page = (Json) node.jjtGetChild(0).value(context);
            if (page == null) {
                return true;
            }
            String text = parse(page, page.getListJson("links"));
            if (node.jjtGetNumChildren() > 1) {
                text = insertPub(text, node.jjtGetChild(1).value(context).toString().replace("'", "\""));
            }

            writer.write(text);

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
