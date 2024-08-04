/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.cosmetic.tmpl.plugs;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.utils.Fx;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

import java.io.Writer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ForEach extends Directive {

    @Override
    public String getName() {
        return "foreach";
    }

    @Override
    public int getType() {
        return BLOCK;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean render(InternalContextAdapter context, Writer writer, Node node) {

        try {

            String varKey = node.jjtGetChild(0).literal().substring(1);
            Object iterable = node.jjtGetChild(2).value(context);
            if (iterable == null) {
                return false;
            }
            Node renderer = node.jjtGetChild(3);
            int velocityCount = 0;

            Object initial = context.get(varKey);
            Object intial_velocityCount = context.get("velocityCount");
            Object intial_velocityLast = context.get("velocityLast");

            if (Json.class.isAssignableFrom(iterable.getClass())) {

                Iterator<Map.Entry<String, Object>> entries = ((Json) iterable).entrySet().iterator();
                while (entries.hasNext()) {
                    Map.Entry<String, Object> item = entries.next();
                    context.put(varKey, item);
                    context.put("velocityCount", velocityCount);
                    context.put("velocityLast", !entries.hasNext());
                    renderer.render(context, writer);
                    writer.flush();
                    context.remove("velocityCount");
                    context.remove("velocityLast");
                    context.remove(varKey);
                    velocityCount++;

                }

            } else if (MongoIterable.class.isAssignableFrom(iterable.getClass())) {

                MongoCursor<Json> it = ((MongoIterable<Json>) iterable).iterator();
                while (it.hasNext()) {
                    Json item = it.next();
                    context.put(varKey, item);
                    context.put("velocityCount", velocityCount);
                    context.put("velocityLast", !it.hasNext());
                    renderer.render(context, writer);
                    writer.flush();
                    item.clear();
                    context.remove("velocityCount");
                    context.remove("velocityLast");
                    context.remove(varKey);
                    velocityCount++;

                }
                it.close();

            } else if (List.class.isAssignableFrom(iterable.getClass()) || Object[].class.isAssignableFrom(iterable.getClass())) {
                List<?> eles = (Object[].class.isAssignableFrom(iterable.getClass())) ? Arrays.asList((Object[]) iterable) : (List) iterable;
                for (Object ele : eles) {
                    context.put(varKey, ele);
                    context.put("velocityCount", velocityCount);
                    context.put("velocityLast", velocityCount + 1 == eles.size());
                    renderer.render(context, writer);
                    writer.flush();
                    context.remove("velocityCount");
                    context.remove("velocityLast");
                    context.remove(varKey);
                    velocityCount++;
                }

            }
            context.put(varKey, initial);
            context.put("velocityCount", intial_velocityCount);
            context.put("velocityLast", intial_velocityLast);

            return true;

        } catch (Exception e) {
            if (Fx.IS_DEBUG) {
                e.printStackTrace();
            }
            return false;
        }
    }

}
