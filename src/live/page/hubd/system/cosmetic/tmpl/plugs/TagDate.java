/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.cosmetic.tmpl.plugs;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TagDate extends Directive {

    @Override
    public String getName() {
        return "date";
    }

    @Override
    public int getType() {
        return LINE;
    }

    @Override
    public boolean render(InternalContextAdapter context, Writer writer, Node node) {
        try {

            TimeZone tz = TimeZone.getTimeZone("UTC");
            if (context.get("lng") != null) {
                try {
                    if (context.get("tz") != null) {
                        String tzreq = context.get("tz").toString();
                        tz = TimeZone.getTimeZone(TimeZone.getAvailableIDs(-(Integer.parseInt(tzreq) * 60 * 1000))[0]);
                    }
                } catch (Exception e) {
                }
            }

            Calendar cal = Calendar.getInstance(tz);
            if (node.jjtGetNumChildren() > 0) {
                cal.setTime((Date) node.jjtGetChild(0).value(context));
            }
            String lng = context.get("lng").toString();
            String date_string;

            if (node.jjtGetNumChildren() > 1) {
                String format = (String) node.jjtGetChild(1).value(context);

                if (format.equalsIgnoreCase("SHORT")) {

                    DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, new Locale(lng));
                    df.setTimeZone(tz);
                    date_string = df.format(cal.getTime());

                } else if (format.equalsIgnoreCase("FULL")) {

                    DateFormat df = DateFormat.getDateInstance(DateFormat.LONG, new Locale(lng));
                    df.setTimeZone(tz);
                    date_string = df.format(cal.getTime());
                    df = DateFormat.getTimeInstance(DateFormat.SHORT, new Locale(lng));
                    df.setTimeZone(tz);
                    date_string += " " + df.format(cal.getTime());

                } else if (format.equalsIgnoreCase("TIME")) {

                    DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, new Locale(lng));
                    df.setTimeZone(tz);
                    date_string = df.format(cal.getTime());
                    df = DateFormat.getTimeInstance(DateFormat.SHORT, new Locale(lng));
                    df.setTimeZone(tz);
                    date_string += " " + df.format(cal.getTime());

                } else {

                    SimpleDateFormat sdf = new SimpleDateFormat(format);
                    sdf.setTimeZone(tz);
                    date_string = sdf.format(cal.getTime());
                }
            } else {
                date_string = DateFormat.getDateInstance(DateFormat.LONG, new Locale(lng)).format(cal.getTime());
            }
            writer.write(date_string);
            return true;
        } catch (Exception e) {
            return false;
        }

    }
}
