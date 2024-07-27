/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.json;

import live.page.hubd.system.db.Db;
import org.bson.*;
import org.bson.codecs.*;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.bson.types.Decimal128;

import java.math.BigDecimal;
import java.util.*;

import static java.util.Arrays.asList;
import static org.bson.assertions.Assertions.notNull;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;

/**
 * Provide support for Json in MongoDB
 */
public class JsonProvider implements CodecProvider {

    private final BsonTypeClassMap bsonTypeClassMap = new BsonTypeClassMap();

    public JsonProvider() {
    }

    @Override
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {

        if (clazz == Json.class || clazz == Document.class) {
            return (Codec<T>) new BaseCodec(registry, bsonTypeClassMap);
        }

        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        JsonProvider that = (JsonProvider) o;

        return bsonTypeClassMap.equals(that.bsonTypeClassMap);
    }

    @Override
    public int hashCode() {
        return 31 * bsonTypeClassMap.hashCode();
    }

    public static class BaseCodec implements CollectibleCodec<Json> {

        private static final String ID_FIELD_NAME = "_id";

        private static final CodecRegistry DEFAULT_REGISTRY = fromProviders(asList(new ValueCodecProvider(), new BsonValueCodecProvider(), new DocumentCodecProvider()));

        private static final BsonTypeClassMap DEFAULT_BSON_TYPE_CLASS_MAP = new BsonTypeClassMap();

        private final BsonTypeCodecMap bsonTypeCodecMap;

        private final CodecRegistry registry;

        private final IdGenerator idGenerator;

        private final Transformer valueTransformer;

        public BaseCodec() {
            this(DEFAULT_REGISTRY, DEFAULT_BSON_TYPE_CLASS_MAP);
        }

        public BaseCodec(CodecRegistry registry, BsonTypeClassMap bsonTypeClassMap) {
            this(registry, bsonTypeClassMap, null);
        }

        public BaseCodec(CodecRegistry registry, BsonTypeClassMap bsonTypeClassMap, Transformer valueTransformer) {
            this.registry = notNull("registry", registry);
            this.bsonTypeCodecMap = new BsonTypeCodecMap(notNull("bsonTypeClassMap", bsonTypeClassMap), registry);
            this.idGenerator = new ObjectIdGenerator();
            this.valueTransformer = valueTransformer != null ? valueTransformer : new Transformer() {
                @Override
                public Object transform(Object value) {
                    return value;
                }
            };
        }

        @Override
        public boolean documentHasId(Json document) {
            return document.containsKey(ID_FIELD_NAME);
        }

        @Override
        public BsonValue getDocumentId(Json document) {
            if (!documentHasId(document)) {
                throw new IllegalStateException("The document does not contain an _id");
            }

            Object id = document.get(ID_FIELD_NAME);
            if (id instanceof BsonValue) {
                return (BsonValue) id;
            }

            BsonDocument idHoldingDocument = new BsonDocument();
            BsonWriter writer = new BsonDocumentWriter(idHoldingDocument);
            writer.writeStartDocument();
            writer.writeName(ID_FIELD_NAME);
            writeValue(writer, EncoderContext.builder().build(), id);
            writer.writeEndDocument();
            return idHoldingDocument.get(ID_FIELD_NAME);
        }

        @Override
        public Json generateIdIfAbsentFromDocument(Json document) {
            if (!documentHasId(document)) {
                document.put(ID_FIELD_NAME, idGenerator.generate());
            }
            return document;
        }

        @Override
        public void encode(BsonWriter writer, Json document, EncoderContext encoderContext) {
            writeMap(writer, document, encoderContext);
        }

        @Override
        public Json decode(BsonReader reader, DecoderContext decoderContext) {
            Json document = new Json();
            reader.readStartDocument();
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                String fieldName = reader.readName();
                document.put(fieldName, readValue(reader, decoderContext));
            }
            reader.readEndDocument();
            return document;
        }

        @Override
        public Class<Json> getEncoderClass() {
            return Json.class;
        }

        private void beforeFields(BsonWriter bsonWriter, EncoderContext encoderContext, Map<String, Object> document) {
            if (encoderContext.isEncodingCollectibleDocument() && document.containsKey(ID_FIELD_NAME)) {
                bsonWriter.writeName(ID_FIELD_NAME);
                writeValue(bsonWriter, encoderContext, document.get(ID_FIELD_NAME));
            }
        }

        private boolean skipField(EncoderContext encoderContext, String key) {
            return encoderContext.isEncodingCollectibleDocument() && key.equals(ID_FIELD_NAME);
        }

        private void writeValue(BsonWriter writer, EncoderContext encoderContext, Object value) {
            if (value == null) {
                writer.writeNull();
            } else if (value instanceof Iterable) {
                writeIterable(writer, (Iterable<Object>) value, encoderContext.getChildContext());
            } else if (value instanceof Map) {
                writeMap(writer, (Map<String, Object>) value, encoderContext.getChildContext());
            } else if (value instanceof Bson) {
                Map bsd = ((Bson) value).toBsonDocument(Map.class, Db.codecRegistry);
                writeMap(writer, bsd, encoderContext.getChildContext());
            } else if (value instanceof String[]) {
                writeIterable(writer, Arrays.asList((Object[]) value), encoderContext.getChildContext());
            } else if (value instanceof BigDecimal) {
                writer.writeDecimal128(new Decimal128((BigDecimal) value));
            } else {
                Codec codec = registry.get(value.getClass());
                encoderContext.encodeWithChildContext(codec, writer, value);
            }
        }


        private void writeMap(BsonWriter writer, Map<String, Object> map, EncoderContext encoderContext) {
            writer.writeStartDocument();

            beforeFields(writer, encoderContext, map);

            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (skipField(encoderContext, entry.getKey())) {
                    continue;
                }
                writer.writeName(entry.getKey());
                writeValue(writer, encoderContext, entry.getValue());
            }
            writer.writeEndDocument();
        }

        private void writeIterable(BsonWriter writer, Iterable<Object> list, EncoderContext encoderContext) {
            writer.writeStartArray();
            for (Object value : list) {
                writeValue(writer, encoderContext, value);
            }
            writer.writeEndArray();
        }

        private Object readValue(BsonReader reader, DecoderContext decoderContext) {
            BsonType bsonType = reader.getCurrentBsonType();
            if (bsonType == BsonType.NULL) {
                reader.readNull();
                return null;
            } else if (bsonType == BsonType.ARRAY) {
                return readList(reader, decoderContext);
            } else if (bsonType == BsonType.BINARY) {
                byte bsonSubType = reader.peekBinarySubType();
                if ((bsonSubType == BsonBinarySubType.UUID_STANDARD.getValue()) || (bsonSubType == BsonBinarySubType.UUID_LEGACY.getValue())) {
                    return registry.get(UUID.class).decode(reader, decoderContext);
                }
            }
            return valueTransformer.transform(bsonTypeCodecMap.get(bsonType).decode(reader, decoderContext));
        }

        private List<Object> readList(BsonReader reader, DecoderContext decoderContext) {
            reader.readStartArray();
            List<Object> list = new ArrayList<>();
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                list.add(readValue(reader, decoderContext));
            }
            reader.readEndArray();
            return list;
        }
    }

}
