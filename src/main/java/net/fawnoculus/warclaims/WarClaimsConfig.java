package net.fawnoculus.warclaims;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class WarClaimsConfig {
    public static String test = "Hello World";

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        test = configuration.getString("greeting", Configuration.CATEGORY_GENERAL, test, "How shall I greet?");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
