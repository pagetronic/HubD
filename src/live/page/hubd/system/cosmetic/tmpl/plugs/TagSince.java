/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.cosmetic.tmpl.plugs;

import live.page.hubd.system.Language;
import live.page.hubd.system.cosmetic.tmpl.FxTemplate;
import live.page.hubd.system.utils.Fx;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class TagSince extends Directive {

    private static final double DAYS_PER_YEAR = 365.24225D;
    private static final double M_PER_SECOND = 1000D;
    private static final double M_PER_MINUTE = 60D * M_PER_SECOND;
    private static final double M_PER_HOUR = 60D * M_PER_MINUTE;
    private static final double M_PER_DAY = 24D * M_PER_HOUR;
    private static final double M_PER_WEEKS = 7D * M_PER_DAY;
    private static final double M_PER_MONTH = Math.floor((DAYS_PER_YEAR / 12D) * M_PER_DAY);
    private static final double M_PER_YEAR = Math.floor(DAYS_PER_YEAR * M_PER_DAY);

    public static String formatSince(long durationInit, String lng, int level) {

        if (durationInit < 60000D && durationInit > -60000D) {
            return Language.get("JUST_NOW", lng);
        }
        boolean past = durationInit < 0;
        double durationMillis = Math.abs(durationInit);

        double yearsD = Math.floor(durationMillis / M_PER_YEAR);
        durationMillis = durationMillis - (yearsD * M_PER_YEAR);

        double monthsD = Math.floor(durationMillis / M_PER_MONTH);
        durationMillis = durationMillis - (monthsD * M_PER_MONTH);

        double weeksD = Math.floor(durationMillis / M_PER_WEEKS);
        durationMillis = durationMillis - (weeksD * M_PER_WEEKS);

        double daysD = Math.floor(durationMillis / M_PER_DAY);
        durationMillis = durationMillis - (daysD * M_PER_DAY);

        double hoursD = Math.floor(durationMillis / M_PER_HOUR);
        durationMillis = durationMillis - (hoursD * M_PER_HOUR);

        double minutesD = Math.floor(durationMillis / M_PER_MINUTE);
        durationMillis = durationMillis - (minutesD * M_PER_MINUTE);

        int years = (int) yearsD;
        int months = (int) monthsD;
        int weeks = (int) weeksD;
        int days = (int) daysD;
        int hours = (int) hoursD;
        int minutes = (int) minutesD;

        // years + "/" + months + "/" + weeks + "/" + days + "/" + hours + "/" +
        // minutes + "/" + seconds;

        StringWriter since = new StringWriter();

        String space_num = " ";
        String space = "";
        while (level > 0) {
            boolean effect = false;
            if (years > 0) {
                since.append(space + years + space_num + (years > 1 ? Language.get("YEARS", lng) : Language.get("YEAR", lng)));
                years = 0;
                effect = true;
            } else if (months > 0) {
                since.append(space + months + space_num + (months > 1 ? Language.get("MONTHS", lng) : Language.get("MONTH", lng)));
                months = 0;
                effect = true;
            } else if (weeks > 0) {
                since.append(space + weeks + space_num + (weeks > 1 ? Language.get("WEEKS", lng) : Language.get("WEEK", lng)));
                weeks = 0;
                effect = true;
            } else if (days > 0) {
                since.append(space + days + space_num + (days > 1 ? Language.get("DAYS", lng) : Language.get("DAY", lng)));
                days = 0;
                effect = true;
            } else if (hours > 0) {
                since.append(space + hours + space_num + (hours > 1 ? Language.get("HOURS", lng) : Language.get("HOUR", lng)));
                hours = 0;
                effect = true;
            } else if (minutes > 0) {
                since.append(space + minutes + space_num + (minutes > 1 ? Language.get("MINUTES", lng) : Language.get("MINUTE", lng)));
                minutes = 0;
                effect = true;
            }
            level--;
            if (effect) {
                if (level == 1) {
                    space = " " + Language.get("AND", lng) + " ";
                } else {
                    space = ", ";
                }
            }
        }
        if (!past) {
            return Language.get("SINCE_AGO", lng, since.toString());
        } else {
            return Language.get("SINCE_IN", lng, since.toString());
        }
    }

    @Override
    public String getName() {
        return "since";
    }

    @Override
    public int getType() {
        return LINE;
    }

    @Override
    public boolean render(InternalContextAdapter context, Writer writer, Node node) {
        try {
            Date date = (Date) node.jjtGetChild(0).value(context);
            int level = 1;
            boolean snipped = false;
            try {
                if ((node.jjtGetNumChildren() > 1) && (node.jjtGetChild(1).value(context) != null)) {
                    level = (Integer) node.jjtGetChild(1).value(context);
                }
                if ((node.jjtGetNumChildren() > 2) && (node.jjtGetChild(2).value(context) != null)) {
                    snipped = (Boolean) node.jjtGetChild(2).value(context);
                }
            } catch (Exception e) {
                if (Fx.IS_DEBUG) {
                    e.printStackTrace();
                }
                level = 1;
            }

            String lng = context.get("lng").toString();

            String since = formatSince(System.currentTimeMillis() - date.getTime(), lng, level);

            DateFormat df = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT, new Locale(lng));

            String tag = "<time datetime=\"" + FxTemplate.isoDate(date) + "\" title=\"" + (df.format(date).replace("\"", "&#34;")) + "\" level=\"" + level + "\"";
            if (snipped) {
                tag += " itemprop=\"dateCreated\"";
            }
            tag += ">" + since + "</time>";

            writer.write(tag);
            return true;
        } catch (Exception e) {
            return false;
        }

    }
}
