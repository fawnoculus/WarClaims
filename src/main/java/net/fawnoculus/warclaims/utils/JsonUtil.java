package net.fawnoculus.warclaims.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import java.io.Reader;
import java.io.Writer;

public class JsonUtil {
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .serializeNulls()
            .create();

    public static void toWriter(Writer writer, JsonElement json) {
        GSON.toJson(json, writer);
    }

    public static <T extends JsonElement> T fromReader(Reader reader, Class<T> clazz) {
        return GSON.fromJson(reader, clazz);
    }
}
