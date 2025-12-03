package net.fawnoculus.warclaims.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.util.math.ChunkPos;

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

    public static String fromChunkPos(ChunkPos pos) {
        return pos.x + ";" + pos.z;
    }

    public static ChunkPos toChunkPos(String string) {
        String[] split = string.split(";");

        int x = Integer.parseInt(split[0]);
        int z = Integer.parseInt(split[1]);

        return new ChunkPos(x, z);
    }
}
