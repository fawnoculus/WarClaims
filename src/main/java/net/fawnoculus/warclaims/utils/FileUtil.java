package net.fawnoculus.warclaims.utils;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.UUID;

public class FileUtil {
    // TODO: make this write the uuids bytes to make it more file size efficient
    public static void writeUUID(Writer writer, UUID uuid) throws IOException {
        String string = uuid.toString();
        writer.write(string.length());
        writer.write(string);
    }

    public static UUID readUUID(Reader reader) throws IOException {
        int stringSize = reader.read();
        char[] chars = new char[stringSize];
        int ignored = reader.read(chars);
        return UUID.fromString(new String(chars));
    }
}
