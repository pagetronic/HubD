/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.system.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import live.page.web.system.db.ParentParser;
import live.page.web.utils.Fx;
import live.page.web.utils.Hidder;
import org.bson.BsonDocument;
import org.bson.BsonDocumentWrapper;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.bson.types.Binary;
import org.bson.types.Decimal128;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Json implements Map<String, Object>, Serializable, Bson {

	private static final long serialVersionUID = 10009990999099099L;

	private final LinkedHashMap<String, Object> datas;

	public Json() {
		datas = new LinkedHashMap<>();
	}

	public Json(String json_string) {

		datas = new LinkedHashMap<>();
		if (json_string == null || json_string.equals("null")) {
			return;
		}
		try {
			ObjectMapper objmapper = new ObjectMapper();

			DateFormat df = new SimpleDateFormat(Fx.ISO_DATE);
			df.setTimeZone(TimeZone.getTimeZone("UTC"));
			objmapper.setDateFormat(df);
			putAll(objmapper.readValue(json_string, Json.class));
		} catch (Exception e) {
			if (Fx.IS_DEBUG) {
				e.printStackTrace();
			}
		}
	}

	public Json(Object obj) {
		datas = new LinkedHashMap<>();
		if (obj == null) {
			return;
		}
		putAll((Map<String, Object>) obj);
	}

	public Json(String key, Object value) {

		datas = new LinkedHashMap<>();
		datas.put(key, value);
	}

	public Json(Map<String, Object> map) {

		datas = new LinkedHashMap<>();
		datas.putAll(map);
	}

	private <T> T get(String key, Class<T> clazz) {
		try {
			if (get(key) == null) {
				return null;
			}
			if (clazz.equals(Integer.class)) {
				return clazz.cast(((Number) datas.get(key)).intValue());
			}
			return clazz.cast(datas.get(key));
		} catch (Exception e) {

			return null;
		}
	}


	public Object get(String key, Object def) {

		if (get(key) == null) {
			return def;
		}
		return get(key);
	}

	public Object get(String key) {
		return datas.get(key);
	}

	public String getId() {
		if (containsKey("_id")) {
			return getString("_id");
		} else {
			return getString("id");
		}
	}

	/**
	 * @param key
	 * @return null or a truncated string
	 */
	public String getString(String key) {
		if (key == null) {
			return null;
		}
		String val = get(key, String.class);
		if (val == null) {
			return null;
		}
		return Fx.truncate(val, 1000);
	}

	public String getString(String key, String def) {
		if (containsKey(key) && get(key) != null) {
			return getString(key);
		}
		return def;
	}

	public String getChoice(String key, String... possibles) {
		for (String possible : possibles) {
			if (possible.equals(get(key))) {
				return getString(key);
			}
		}
		return null;
	}

	public String getText(String key) {
		return get(key, String.class);
	}

	public String getText(String key, String def) {
		if (containsKey(key)) {
			String text = getText(key);
			if (text == null) {
				return def;
			}
			return text;
		}
		return def;
	}

	public boolean getBoolean(String key, boolean def) {
		try {
			if (containsKey(key) && get(key) != null) {
				return get(key, Boolean.class);
			}
			return def;
		} catch (Exception e) {
			if (getString(key, "").equals("true")) {
				return true;
			}
			if (getString(key, "").equals("false")) {
				return false;
			}
			return def;
		}
	}

	public Date getDate(String key) {
		return get(key, Date.class);
	}

	public Date getDate(String key, Date def) {
		if (get(key) == null) {
			return def;
		}
		return get(key, Date.class);
	}

	public Date parseDate(String key) {
		if (getString(key) == null) {
			return null;
		}
		try {
			SimpleDateFormat fm = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
			fm.setTimeZone(TimeZone.getTimeZone("UTC"));
			return fm.parse(getString(key));
		} catch (Exception e) {
			return null;
		}
	}

	public int getInteger(String key) {
		if (get(key).getClass().equals(Long.class)) {
			return get(key, Long.class).intValue();
		}
		try {
			return get(key, Integer.class);
		} catch (Exception e) {
			return Integer.parseInt(getString(key));
		}
	}

	public int getInteger(String key, int def) {

		try {
			if (datas.get(key) == null) {
				return def;
			}
			return (int) get(key);
		} catch (Exception e) {
			try {
				return Integer.parseInt(getString(key));
			} catch (Exception ex) {
				return def;
			}
		}
	}

	public double getDouble(String key) {

		try {
			return get(key, Double.class);
		} catch (Exception e) {
			return Double.parseDouble(getString(key));
		}
	}

	public double getDouble(String key, double def) {
		if (datas.get(key) == null) {
			return def;
		}
		try {
			return getDouble(key);
		} catch (Exception e) {
			try {
				return getInteger(key);
			} catch (Exception ex) {
				return def;
			}
		}
	}

	public BigDecimal getBigDecimal(String key, double def) {

		if (get(key) instanceof Decimal128) {
			return get(key, Decimal128.class).bigDecimalValue();
		}
		if (get(key) instanceof BigDecimal) {
			return get(key, BigDecimal.class);
		}
		if (get(key) instanceof Double) {
			return BigDecimal.valueOf(get(key, Double.class));
		}
		if (get(key) instanceof Float) {
			return BigDecimal.valueOf(get(key, Float.class));
		}
		if (get(key) instanceof Long) {
			return BigDecimal.valueOf(get(key, Long.class));
		}
		if (get(key) instanceof Integer) {
			return BigDecimal.valueOf(get(key, Integer.class));
		}
		return BigDecimal.valueOf(def);
	}

	public List<String> getList(String key) {
		return getList(key, String.class);
	}

	public <T> List<T> getList(String key, Class<T> clazz) {
		return (List<T>) datas.get(key);
	}

	public List<Json> getListJson(String key) {

		if (key == null || get(key) == null) {
			return null;
		}
		Iterator list = get(key, List.class).iterator();
		List<Json> outlist = new ArrayList<>();
		while (list.hasNext()) {
			Object obj = list.next();
			if (Json.class.isAssignableFrom(obj.getClass())) {
				outlist.add((Json) obj);
			} else if (Map.class.isAssignableFrom(obj.getClass())) {
				outlist.add(new Json(obj));
			} else {
				return null;
			}
		}
		return outlist;
	}

	public Json getJson(String key) {
		if (get(key) == null) {
			return null;
		}
		try {
			if (get(key).getClass().equals(Json.class)) {
				return get(key, Json.class);
			} else if (get(key).getClass().equals(Document.class)) {
				return new Json(get(key, Document.class));
			} else {
				return new Json(get(key));
			}
		} catch (Exception e) {
			return null;
		}

	}

	public Binary getBinary(String key) {
		return get(key, Binary.class);
	}

	@Override
	public int size() {
		return datas.size();
	}

	@Override
	public boolean isEmpty() {
		return datas.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return datas.containsKey(key);
	}

	@Override
	public boolean containsValue(Object val) {
		return datas.containsValue(val);
	}

	@Override
	public Object get(Object key) {
		return datas.get(key);
	}

	public Json put(String key, Object value) {
		datas.remove(key);
		datas.put(key, value);
		return this;
	}

	public Json prepend(String key, Object value) {
		datas.remove(key);
		Json clone = new Json(this);
		datas.clear();
		datas.put(key, value);
		datas.putAll(clone);
		return this;
	}

	public Json set(String key, Object value) {
		datas.put(key, value);
		return this;
	}

	@Override
	public Object remove(Object key) {
		return datas.remove(key);
	}

	@Override
	public void putAll(Map<? extends String, ?> map) {
		datas.putAll(map);
	}

	public Json putAll(Json map) {
		datas.putAll(map);
		return this;
	}

	public boolean containsKey(String key) {
		return datas.containsKey(key);
	}

	public Json remove(String key) {
		datas.remove(key);
		return this;
	}

	public Json add(String key, Object value) {
		List<Object> values = getList(key, Object.class);
		if (values == null) {
			values = new ArrayList<>();
		}
		values.add(value);
		return put(key, values);
	}

	public Json add(String key, Object value, int position) {
		List<Object> values = getList(key, Object.class);
		if (values == null) {
			values = new ArrayList<>();
		}
		values.add(position, value);
		return put(key, values);
	}

	@Override
	public Set<java.util.Map.Entry<String, Object>> entrySet() {
		return datas.entrySet();
	}

	@Override
	public void clear() {
		datas.clear();
	}

	@Override
	public Set<String> keySet() {
		return datas.keySet();
	}

	@Override
	public Collection<Object> values() {
		return datas.values();
	}

	public List<String> keyList() {
		List<String> keys = new ArrayList<>();
		Collections.addAll(keys, datas.keySet().toArray(new String[0]));
		return keys;
	}

	@Override
	public String toString() {
		return XMLJsonParser.toJSON(this);
	}


	public String toString(boolean compressed) {
		return XMLJsonParser.toJSON(this, compressed);
	}

	@Override
	public <C> BsonDocument toBsonDocument(final Class<C> documentClass, final CodecRegistry codecRegistry) {
		return new BsonDocumentWrapper<Json>(this, codecRegistry.get(Json.class));
	}

	public String getHash() {
		return "i" + Hidder.encodeString(String.valueOf(toString().hashCode()));
	}

	@Override
	public Json clone() {
		try {
			return (Json) super.clone();
		} catch (CloneNotSupportedException e) {
			return new Json(this);
		}
	}

	public List<Json> toList() {
		List<Json> arr = new ArrayList<>();
		for (Entry<String, Object> entry : datas.entrySet()) {
			arr.add(new Json("key", entry.getKey()).put("value", entry.getValue()));
		}
		return arr;
	}

	public String findKey(Object value) {
		if (value == null) {
			return null;
		}
		for (Entry<String, Object> set : entrySet()) {
			if (set.getValue().equals(value)) {
				return set.getKey();
			}
		}
		return null;

	}

	public ParentParser getParent(String key) {
		try {
			return new ParentParser(getString(key));
		} catch (Exception e) {
			return null;
		}
	}

	public List<ParentParser> getParents(String key) {
		try {
			List<ParentParser> parents = new ArrayList<>();
			for (String parent : getList(key)) {
				parents.add(new ParentParser(parent));
			}
			return parents;
		} catch (Exception e) {
			return null;
		}
	}

	public Json sort() {
		List<String> keys = Arrays.asList(keySet().toArray(new String[0]));
		Collections.sort(keys);
		Collections.reverse(keys);
		keys.forEach((key) -> prepend(key, get(key)));
		return this;
	}
}
