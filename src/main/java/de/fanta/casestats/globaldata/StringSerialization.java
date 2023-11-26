package de.fanta.casestats.globaldata;

import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

public class StringSerialization {

    public static final int MAX_TYPE_NAME_LENGTH = 64;
    public static final String SERIALIZATION_TYPE_STRING = "STRING";
    public static final String SERIALIZATION_TYPE_BOOLEAN = "BOOLEAN";
    public static final String SERIALIZATION_TYPE_INTEGER = "INTEGER";
    public static final String SERIALIZATION_TYPE_LONG = "LONG";
    public static final String SERIALIZATION_TYPE_DOUBLE = "DOUBLE";

    private static final Pattern LEGAL_TYPE_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9\\_]{1," + MAX_TYPE_NAME_LENGTH + "}");
    private static Map<String, Function<String, ?>> serializationTypes;

    static {
        serializationTypes = new GeneralHashMap<>(StringUtilCore.CASE_IGNORING_HASHER, StringUtilCore.CASE_IGNORING_EQUALITY);

        registerInternal(NullWrapper.SERIALIZATION_TYPE, serialized -> null);
        registerInternal(SERIALIZATION_TYPE_STRING, serialized -> serialized);
        registerInternal(SERIALIZATION_TYPE_BOOLEAN, Boolean::parseBoolean);
        registerInternal(SERIALIZATION_TYPE_INTEGER, Integer::parseInt);
        registerInternal(SERIALIZATION_TYPE_LONG, Long::parseLong);
        registerInternal(SERIALIZATION_TYPE_DOUBLE, Double::parseDouble);
    }

    public static synchronized void register(String serializationType, Function<String, StringSerializable> deserializer) {
        registerInternal(serializationType, deserializer);
    }

    private static synchronized void registerInternal(String serializationType, Function<String, ?> deserializer) {
        if (!LEGAL_TYPE_NAME_PATTERN.matcher(serializationType).matches()) {
            throw new IllegalArgumentException("Name of serialization is illegal.");
        }
        Function<String, ?> old = serializationTypes.putIfAbsent(serializationType, deserializer);
        if (old != null) {
            throw new IllegalArgumentException("This serializationType is already registered!");
        }
    }

    public static Pair<String, String> serialize(Object arg) {
        if (arg == null) {
            return new Pair<>(NullWrapper.SERIALIZATION_TYPE, NullWrapper.INSTANCE.serializeToString());
        } else if (arg instanceof StringSerializable s) {
            return new Pair<>(s.getSerializationType(), s.serializeToString());
        } else if (arg instanceof String s) {
            return new Pair<>(SERIALIZATION_TYPE_STRING, s);
        } else if (arg instanceof Boolean b) {
            return new Pair<>(SERIALIZATION_TYPE_BOOLEAN, String.valueOf(b));
        } else if (arg instanceof Integer i) {
            return new Pair<>(SERIALIZATION_TYPE_INTEGER, String.valueOf(i));
        } else if (arg instanceof Long x) {
            return new Pair<>(SERIALIZATION_TYPE_LONG, String.valueOf(x));
        } else if (arg instanceof Double d) {
            return new Pair<>(SERIALIZATION_TYPE_DOUBLE, String.valueOf(d));
        } else {
            throw new ClassCastException(arg.getClass() + " neither implements StringSerializable nor is implicitely serializable to String.");
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T deserialize(String serializationType, String serialized) {
        Function<String, ?> deserializer;
        synchronized (StringSerializable.class) {
            deserializer = serializationTypes.get(serializationType);
        }
        if (deserializer == null) {
            throw new IllegalArgumentException("Unknown serializationType " + serializationType + ".");
        }
        try {
            return (T) deserializer.apply(serialized);
        } catch (Exception e) {
            throw new RuntimeException("Exception trying to deserialize type " + serializationType + " with serialized text: " + serialized, e);
        }
    }

    private StringSerialization() {
        throw new UnsupportedOperationException("No instance for you, Sir!");
        // prevent instances
    }

}
