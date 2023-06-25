package net.xdow.aliyundrive.util;

import com.google.gson.*;

import java.lang.reflect.Type;

public class JsonUtils {

    private static final Gson sGson;
    private static final Gson sGsonPretty;
    static {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
        gsonBuilder.setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sGson = gsonBuilder.create();
        sGsonPretty = gsonBuilder.setPrettyPrinting().create();
    }

    public static <T> T fromJson(String json, Class<T> classOfT) throws JsonSyntaxException {
        return sGson.fromJson(json, classOfT);
    }

    public static <T> T fromJson(JsonElement json, Class<T> classOfT) throws JsonSyntaxException {
        return sGson.fromJson(json, classOfT);
    }

    public static <T> T fromJson(JsonElement json, Type typeOfT) throws JsonSyntaxException {
        return sGson.fromJson(json, typeOfT);
    }

    public static <T> T fromJson(String json, Type typeOfT) throws JsonSyntaxException {
        return sGson.fromJson(json, typeOfT);
    }

    public static String toJson(Object src) {
        return sGson.toJson(src);
    }

    public static String toJsonPretty(Object src) {
        return sGsonPretty.toJson(src);
    }

    public static JsonElement toJsonTree(Object src) {
        return sGson.toJsonTree(src);
    }
}
