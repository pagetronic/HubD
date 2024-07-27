/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.cosmetic.tmpl.plugs;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

import java.io.Writer;
import java.text.NumberFormat;
import java.util.Locale;

public class SizeTag extends Directive {

    @Override
    public String getName() {
        return "size";
    }

    @Override
    public int getType() {
        return LINE;
    }

    @Override
    public boolean render(InternalContextAdapter context, Writer writer, Node node) {
        try {
            NumberFormat formatter = NumberFormat.getNumberInstance(new Locale(context.get("lng").toString()));

            double size = (double) node.jjtGetChild(0).value(context);
            if (size < 1) {
                writer.write(formatter.format(size * 100) + "cm");
            } else {
                writer.write(formatter.format(size) + "m");
            }

            return true;
        } catch (Exception e) {
            return false;
        }

    }


}
