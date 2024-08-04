/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.cosmetic.tmpl.plugs;

import live.page.hubd.system.Language;
import live.page.hubd.system.utils.Fx;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

import java.io.Writer;

public class ModuloTag extends Directive {

    @Override
    public String getName() {
        return "modulo";
    }

    @Override
    public int getType() {
        return LINE;
    }

    @Override
    public boolean render(InternalContextAdapter context, Writer writer, Node node) throws MethodInvocationException, ResourceNotFoundException, ParseErrorException {

        try {

            String lng = context.get("lng").toString();
            String first = (node.jjtGetNumChildren() > 0) ? (String) node.jjtGetChild(0).value(context) : "";
            String middle = (node.jjtGetNumChildren() > 1) ? (String) node.jjtGetChild(1).value(context) : ", ";
            String last = (node.jjtGetNumChildren() > 2) ? (String) node.jjtGetChild(2).value(context) : " " + Language.get("AND", lng) + " ";
            int velocityCount = (int) context.get("velocityCount");
            boolean velocityLast = (boolean) context.get("velocityLast");
            writer.write(velocityCount == 0 ? first : velocityLast ? last : middle);

        } catch (Exception e) {
            if (Fx.IS_DEBUG) {
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }

}