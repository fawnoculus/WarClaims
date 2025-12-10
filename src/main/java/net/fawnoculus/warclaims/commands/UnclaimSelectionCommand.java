package net.fawnoculus.warclaims.commands;

import com.google.common.collect.ImmutableList;
import net.fawnoculus.warclaims.WarClaimsConfig;
import net.fawnoculus.warclaims.claims.ClaimManager;
import net.fawnoculus.warclaims.claims.faction.FactionInstance;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.NumberInvalidException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class UnclaimSelectionCommand extends CommandBase {
    @Override
    public String getName() {
        return "unclaim-selection";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "unclaim-selection <start-chunkX> <start-chunkZ> <end-chunkX> <end-chunkZ>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 4) {
            throw new CommandException("Not Enough Arguments: " + this.getUsage(sender));
        }

        if (!(sender.getCommandSenderEntity() instanceof EntityPlayerMP)) {
            throw new CommandException("Must be executed by a Player");
        }
        EntityPlayerMP playerMP = (EntityPlayerMP) sender.getCommandSenderEntity();
        int dimension = playerMP.dimension;
        BlockPos playerPos = playerMP.getPosition();

        int startChunkX;
        try {
            startChunkX = Integer.parseInt(args[0]);
        } catch (NumberFormatException ignored) {
            throw new NumberInvalidException("%1$s is not a valid integer (start-chunk-x)", args[0]);
        }

        int startChunkZ;
        try {
            startChunkZ = Integer.parseInt(args[1]);
        } catch (NumberFormatException ignored) {
            throw new NumberInvalidException("%1$s is not a valid integer (start-ChunkZ)", args[1]);
        }

        int endChunkX;
        try {
            endChunkX = Integer.parseInt(args[2]);
        } catch (NumberFormatException ignored) {
            throw new NumberInvalidException("%1$s is not a valid integer (end-ChunkX)", args[2]);
        }

        int endChunkZ;
        try {
            endChunkZ = Integer.parseInt(args[3]);
        } catch (NumberFormatException ignored) {
            throw new NumberInvalidException("%1$s is not a valid integer (end-ChunkZ)", args[3]);
        }

        final int minX = Math.min(startChunkX, endChunkX);
        final int maxX = Math.max(startChunkX, endChunkX);
        final int minZ = Math.min(startChunkZ, endChunkZ);
        final int maxZ = Math.max(startChunkZ, endChunkZ);

        int outOfRangeChunks = 0;
        int notClaimedChunks = 0;
        int missingPermissionChunks = 0;
        int successfullyUnclaimedChunks = 0;
        int totalChunks = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                totalChunks++;

                if (!WarClaimsConfig.isInClaimRange(playerPos, x, z)) {
                    outOfRangeChunks++;
                    continue;
                }

                FactionInstance claimingFaction = ClaimManager.getFaction(dimension, x, z);
                if (claimingFaction == null) {
                    notClaimedChunks++;
                    continue;
                }

                if (!claimingFaction.isOfficer(playerMP)) {
                    missingPermissionChunks++;
                    continue;
                }

                ClaimManager.unclaim(dimension, x, z);
                successfullyUnclaimedChunks++;
            }
        }

        if (outOfRangeChunks > 0) {
            sender.sendMessage(new TextComponentString(String.format("%1$d chunks where out of range", outOfRangeChunks)));
        }

        if (notClaimedChunks > 0) {
            sender.sendMessage(new TextComponentString(String.format("%1$d chunks where not Claimed", notClaimedChunks)));
        }

        if (missingPermissionChunks > 0) {
            sender.sendMessage(new TextComponentString(String.format("You didn't have the permission to unclaim %1$d chunks", missingPermissionChunks)));
        }

        if (successfullyUnclaimedChunks > 0) {
            sender.sendMessage(new TextComponentString(String.format("Unclaimed %1$d/%2$d chunks", successfullyUnclaimedChunks, totalChunks)));
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length < 5) {
            return getListOfStringsMatchingLastWord(args, ImmutableList.of("-2", "0", "100", "420", "666"));
        }

        if (args.length == 5) {
            return getListOfStringsMatchingLastWord(args, ImmutableList.of("0", "1", "2", "3", "4"));
        }

        return Collections.emptyList();
    }
}
