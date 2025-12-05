package net.fawnoculus.warclaims;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class WarClaimsConfig {
    public static int ClaimDistance = 16;
    public static int InvadeDistance = 16;

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        ClaimDistance = configuration.getInt(
                "ClaimDistance",
                Configuration.CATEGORY_GENERAL,
                ClaimDistance,
                0,
                Integer.MAX_VALUE,
                "Maximum Claim distance in Chunks"
        );

        InvadeDistance = configuration.getInt(
                "InvadeDistance",
                Configuration.CATEGORY_GENERAL,
                InvadeDistance,
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
        return xDifference < ClaimDistance && zDifference < ClaimDistance;
    }

    public static boolean isInInvasionRange(BlockPos playerPos, int chunkX, int chunkZ) {
        ChunkPos playerChunk = new ChunkPos(playerPos);
        int xDifference = Math.abs(playerChunk.x - chunkX);
        int zDifference = Math.abs(playerChunk.z - chunkZ);
        return xDifference < InvadeDistance && zDifference < InvadeDistance;
    }
}
