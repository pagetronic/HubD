/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.system.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mongodb.client.MongoIterable;
import live.page.web.utils.Fx;
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

	public static String toJSON(Object object, boolean compressed) {
		if (object == null) {
			return null;
		}
		if (object instanceof MongoIterable) {
			object = ((MongoIterable) object).into(new ArrayList());
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

				return null;
			}

			class Decimal128Adapter extends TypeAdapter<Decimal128> {
				@Override
				public void write(JsonWriter out, Decimal128 value) throws IOException {
					out.jsonValue(value.bigDecimalValue().toPlainString());
				}

				@Override
				public Decimal128 read(JsonReader in) {
					return null;
				}
			}

			class DateAdapter extends TypeAdapter<Date> {
				@Override
				public void write(JsonWriter out, Date date) throws IOException {
					DateFormat df = new SimpleDateFormat(Fx.ISO_DATE);
					df.setTimeZone(TimeZone.getTimeZone("UTC"));
					out.jsonValue("\"" + df.format(date) + "\"");
				}

				@Override
				public Date read(JsonReader in) {
					return null;
				}
			}

			class ObjectIdAdapter extends TypeAdapter<ObjectId> {
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
		return gson.create().toJson(object).replace("\"_id\"", "\"id\"");
	}

	public static String toXML(Object object) {
		StringBuilder sb = new StringBuilder();
		if (object instanceof MongoIterable) {
			object = ((MongoIterable) object).into(new ArrayList());
		}

		if (object instanceof ObjectId) {
			object = ((ObjectId) object).toString();
		}
		if (object instanceof Decimal128) {
			object = ((Decimal128) object).bigDecimalValue().toPlainString();
		}
		if (object instanceof Json) {

			Json json = (Json) object;
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
		} else if (object instanceof List) {
			List list = (List) object;
			for (Object o : list) {
				sb.append("<value>");
				sb.append(toXML(o));
				sb.append("</value>");
			}

		} else if (object instanceof Object[]) {
			Object[] array = (Object[]) object;
			for (Object o : array) {
				sb.append("<value>");
				sb.append(toXML(o));
				sb.append("</value>");
			}

		} else if (object instanceof Date) {
			Date date = (Date) object;
			DateFormat dateFormat = new SimpleDateFormat(Fx.ISO_DATE);
			dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			sb.append(dateFormat.format(date));

		} else {
			sb.append(StringEscapeUtils.escapeXml11("" + object));
		}
		return sb.toString();
	}
}
