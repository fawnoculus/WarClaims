package net.fawnoculus.warclaims;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class WarClaimsConfig {
    public static Configuration configuration;
    public static int claimDistance = 16;
    public static int invadeDistance = 16;

    public static void synchronizeConfiguration(File configFile) {
        configuration = new Configuration(configFile);

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

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    public static boolean isInClaimRange(BlockPos playerPos, int chunkX, int chunkZ) {
        ChunkPos playerChunk = new ChunkPos(playerPos);
        int xDifference = Math.abs(playerChunk.x - chunkX);
        int zDifference = Math.abs(playerChunk.z - chunkZ);
        return xDifference < claimDistance && zDifference < claimDistance;
    }

    public static boolean isInInvasionRange(BlockPos playerPos, int chunkX, int chunkZ) {
        ChunkPos playerChunk = new ChunkPos(playerPos);
        int xDifference = Math.abs(playerChunk.x - chunkX);
        int zDifference = Math.abs(playerChunk.z - chunkZ);
        return xDifference < invadeDistance && zDifference < invadeDistance;
    }
}
