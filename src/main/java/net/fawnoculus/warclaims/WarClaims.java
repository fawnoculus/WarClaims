package net.fawnoculus.warclaims;

import com.google.gson.JsonElement;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Tags.MOD_ID, version = Tags.MOD_VERSION, name = Tags.MOD_NAME, useMetadata = true)
public class WarClaims {
    public static final String FILE_VERSION_NAME = "File-Version";
    public static final String FILE_VERSION = "1.0-alpha";
    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);
    @SidedProxy(clientSide = "net.fawnoculus.warclaims.WarClaimsClientProxy",
            serverSide = "net.fawnoculus.warclaims.WarClaimsCommonProxy",
            modId = Tags.MOD_ID
    )
    public static WarClaimsCommonProxy proxy;

    public static boolean isCorrectFileVersion(JsonElement versionJson) {
        return versionJson.isJsonPrimitive()
                && versionJson.getAsJsonPrimitive().isString()
                && isCorrectFileVersion(versionJson.getAsString());
    }

    public static boolean isCorrectFileVersion(String versionString) {
        return FILE_VERSION.equals(versionString);
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }
}
