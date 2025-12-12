package net.fawnoculus.warclaims;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class WarClaimsConfig {
    public static Configuration configuration;
    public static String claimSingleCommandFormat = "/claim-single %1$d %2$d LEVEL_HERE";
    public static String claimSelectionCommandFormat = "/claim-selection %1$d %2$d %3$d %4$d LEVEL_HERE";
    public static String unclaimSingleCommandFormat = "/unclaim-single %1$d %2$d";
    public static String unclaimSelectionCommandFormat = "/unclaim-selection %1$d %2$d %3$d %4$d";
    public static String invadeSingleCommandFormat = "/invade-single %1$d %2$d ";
    public static String invadeSelectionCommandFormat = "/invade-selection %1$d %2$d %3$d %4$d ";
    public static int claimDistance = 16;
    public static int invadeDistance = 16;
    public static boolean showTicksInInvasions = false;

    public static void synchronizeConfiguration(File configFile) {
        configuration = new Configuration(configFile);

        claimSingleCommandFormat = configuration.getString(
                "claimSingleCommandFormat",
                Configuration.CATEGORY_CLIENT,
                claimSingleCommandFormat,
                "Format for the command for claiming a single chunk"
        );

        claimSelectionCommandFormat = configuration.getString(
                "claimSelectionCommandFormat",
                Configuration.CATEGORY_CLIENT,
                claimSelectionCommandFormat,
                "Format for the command for claiming a selection of chunks"
        );

        unclaimSingleCommandFormat = configuration.getString(
                "unclaimSingleCommandFormat",
                Configuration.CATEGORY_CLIENT,
                unclaimSingleCommandFormat,
                "Format for the command for unclaiming a single chunk"
        );

        unclaimSelectionCommandFormat = configuration.getString(
                "unclaimSelectionCommandFormat",
                Configuration.CATEGORY_CLIENT,
                unclaimSelectionCommandFormat,
                "Format for the command for unclaiming a selection of chunks"
        );

        invadeSingleCommandFormat = configuration.getString(
                "invadeSingleCommandFormat",
                Configuration.CATEGORY_CLIENT,
                invadeSingleCommandFormat,
                "Format for the command for invading a single chunk"
        );

        invadeSelectionCommandFormat = configuration.getString(
                "invadeSelectionCommandFormat",
                Configuration.CATEGORY_CLIENT,
                invadeSelectionCommandFormat,
                "Format for the command for invading a selection of chunks"
        );

        claimDistance = configuration.getInt(
                "claimDistance",
                Configuration.CATEGORY_GENERAL,
                claimDistance,
                0,
                Integer.MAX_VALUE,
                "Maximum Claim distance in Chunks"
        );

        invadeDistance = configuration.getInt(
                "invadeDistance",
                Configuration.CATEGORY_GENERAL,
                invadeDistance,
                0,
                Integer.MAX_VALUE,
                "Maximum Invade distance in Chunks"
        );

        showTicksInInvasions = configuration.getBoolean(
                "showTicksInInvasions",
                Configuration.CATEGORY_GENERAL,
                showTicksInInvasions,
                "If tick progress and total tick amounts should be displayed in invasion tooltips"
        );

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    public static boolean isOutOfClaimRange(BlockPos playerPos, int chunkX, int chunkZ) {
        ChunkPos playerChunk = new ChunkPos(playerPos);
        int xDifference = Math.abs(playerChunk.x - chunkX);
        int zDifference = Math.abs(playerChunk.z - chunkZ);
        return xDifference >= claimDistance || zDifference >= claimDistance;
    }

    public static boolean isOutOfInvasionRange(BlockPos playerPos, int chunkX, int chunkZ) {
        ChunkPos playerChunk = new ChunkPos(playerPos);
        int xDifference = Math.abs(playerChunk.x - chunkX);
        int zDifference = Math.abs(playerChunk.z - chunkZ);
        return xDifference >= invadeDistance || zDifference >= invadeDistance;
    }
}
