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

public class NumberTag extends Directive {

    @Override
    public String getName() {
        return "number";
    }

    @Override
    public int getType() {
        return LINE;
    }

    @Override
    public boolean render(InternalContextAdapter context, Writer writer, Node node) {
        try {
            NumberFormat formatter = NumberFormat.getNumberInstance(new Locale(context.get("lng").toString()));
            writer.write(formatter.format(node.jjtGetChild(0).value(context)));
            return true;
        } catch (Exception e) {
            return false;
        }

    }


}
