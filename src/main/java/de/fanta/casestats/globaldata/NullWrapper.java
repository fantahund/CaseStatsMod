package de.fanta.casestats.globaldata;

public class NullWrapper implements StringSerializable {

    public static final String SERIALIZATION_TYPE = "NULL";

    public static final NullWrapper INSTANCE = new NullWrapper();

    private NullWrapper() {

    }

    @Override
    public String getSerializationType() {
        return SERIALIZATION_TYPE;
    }

    @Override
    public String serializeToString() {
        return "";
    }

}
