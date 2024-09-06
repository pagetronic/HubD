/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mongodb.client.MongoIterable;
import live.page.hubd.system.utils.Fx;
import org.apache.commons.text.StringEscapeUtils;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class XMLJsonParser {

    public static String toJSON(Object object) {
        return toJSON(object, false);
    }

    @SuppressWarnings("unchecked")
    public static String toJSON(Object object, boolean compressed) {
        if (object == null) {
            return null;
        }
        if (object instanceof MongoIterable) {
            object = ((MongoIterable<?>) object).into(new ArrayList<>());
        }

        if (object instanceof ObjectId) {
            object = object.toString();
        }
        class GsonAdapter implements TypeAdapterFactory {
            @Override
            public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {

                if (Date.class.isAssignableFrom(type.getRawType())) {
                    return (TypeAdapter<T>) new DateAdapter();
                }
                if (ObjectId.class.isAssignableFrom(type.getRawType())) {
                    return (TypeAdapter<T>) new ObjectIdAdapter();
                }
                if (Decimal128.class.isAssignableFrom(type.getRawType())) {
                    return (TypeAdapter<T>) new Decimal128Adapter();
                }
                /*
                    if (Double.class.isAssignableFrom(type.getRawType())) {
                        return (TypeAdapter<T>) new DoubleAdapter();
                    }
                */
                return null;
            }

            static class Decimal128Adapter extends TypeAdapter<Decimal128> {
                @Override
                public void write(JsonWriter out, Decimal128 value) throws IOException {
                    out.jsonValue(value.bigDecimalValue().toPlainString());
                }

                @Override
                public Decimal128 read(JsonReader in) {
                    return null;
                }
            }
            /*
                static class DoubleAdapter extends TypeAdapter<Double> {
                    static final DecimalFormat df = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

                    static {
                        df.setMaximumFractionDigits(15);

                    }

                    @Override
                    public void write(JsonWriter out, Double value) throws IOException {
                        out.jsonValue(df.format(value));
                    }

                    @Override
                    public Double read(JsonReader in) {
                        return null;
                    }
                }
            */

            static class DateAdapter extends TypeAdapter<Date> {
                @Override
                public void write(JsonWriter out, Date date) throws IOException {
                    out.jsonValue("\"" + Fx.ISO_DATE.format(date) + "\"");
                }

                @Override
                public Date read(JsonReader in) {
                    return null;
                }
            }

            static class ObjectIdAdapter extends TypeAdapter<ObjectId> {
                @Override
                public void write(JsonWriter out, ObjectId obj) throws IOException {
                    out.jsonValue("\"" + obj.toString() + "\"");
                }

                @Override
                public ObjectId read(JsonReader in) {
                    return null;
                }
            }
        }
        GsonBuilder gson = new GsonBuilder();
        gson.registerTypeAdapterFactory(new GsonAdapter());
        if (!compressed) {
            gson.setPrettyPrinting();
        }
        String json = gson.create().toJson(object)
                .replace("\"_id\"", "\"id\"").replace("\"_location\"", "\"location\"");
        if (!compressed) {
            json = json.replaceAll("\\[[ \\n]+?([0-9\\-.]+),[ \\n]+?[ \\n]+?([0-9\\-.]+)[ \\n]+?]", "[$1, $2]");
        }
        return json;

    }

    public static String toXML(Object object) {
        StringBuilder sb = new StringBuilder();
        if (object instanceof MongoIterable) {
            object = ((MongoIterable<?>) object).into(new ArrayList<>());
        }

        if (object instanceof ObjectId) {
            object = ((ObjectId) object).toString();
        }
        if (object instanceof Decimal128) {
            object = ((Decimal128) object).bigDecimalValue().toPlainString();
        }
        if (object instanceof Json json) {

            for (String key : json.keySet()) {
                Object value = json.get(key);
                if (value != null) {
                    sb.append("<");
                    if (key.equals("_id")) {
                        sb.append("id");
                    } else {
                        sb.append(key);
                    }
                    sb.append(">");
                    sb.append(toXML(value));
                    sb.append("</");
                    if (key.equals("_id")) {
                        sb.append("id");
                    } else {
                        sb.append(key);
                    }
                    sb.append('>');
                }
            }
        } else if (object instanceof List<?> list) {
            for (Object o : list) {
                sb.append("<value>");
                sb.append(toXML(o));
                sb.append("</value>");
            }

        } else if (object instanceof Object[] array) {
            for (Object o : array) {
                sb.append("<value>");
                sb.append(toXML(o));
                sb.append("</value>");
            }

        } else if (object instanceof Date date) {
            sb.append(Fx.ISO_DATE.format(date));

        } else {
            sb.append(StringEscapeUtils.escapeXml11("" + object));
        }
        return sb.toString();
    }
}
