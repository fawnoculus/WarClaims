package net.fawnoculus.warclaims.commands;

import com.google.common.collect.ImmutableList;
import net.fawnoculus.warclaims.WarClaimsConfig;
import net.fawnoculus.warclaims.claims.ClaimInstance;
import net.fawnoculus.warclaims.claims.ClaimManager;
import net.fawnoculus.warclaims.claims.faction.FactionInstance;
import net.fawnoculus.warclaims.claims.faction.FactionManager;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.NumberInvalidException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class ClaimSelectionCommand extends CommandBase {
    @Override
    public String getName() {
        return "claim-selection";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "claim-selection <start-chunkX> <start-chunkZ> <end-chunkX> <end-chunkZ> <level>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 5) {
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

        int level;
        try {
            level = Integer.parseInt(args[4]);
        } catch (NumberFormatException ignored) {
            throw new NumberInvalidException("%1$s is not a valid integer (level)", args[4]);
        }

        if (level < 0 || level > 4) {
            throw new NumberInvalidException("level %1$s invalid, must be between 0 and 4", args[2]);
        }

        UUID selectedFaction = FactionManager.getSelectedFaction(playerMP);
        if (selectedFaction == null) {
            throw new CommandException("You must select or create a faction with /faction");
        }

        FactionInstance faction = FactionManager.getFaction(selectedFaction);
        if (faction == null) {
            throw new CommandException("The Team you have selected does not exist");
        }

        if (!faction.isOfficer(playerMP)) {
            throw new CommandException(
                    "You do not have permission to claim chunks for \"%1$s\" you must be an officer or the owner",
                    faction.name
            );
        }

        final int minX = Math.min(startChunkX, endChunkX);
        final int maxX = Math.max(startChunkX, endChunkX);
        final int minZ = Math.min(startChunkZ, endChunkZ);
        final int maxZ = Math.max(startChunkZ, endChunkZ);

        int outOfRange = 0;
        int claimedByOtherFaction = 0;

        int totalChunks = 0;
        HashSet<ChunkPos> toTryClaim = new HashSet<>();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                totalChunks++;

                if (WarClaimsConfig.isOutOfClaimRange(playerPos, x, z)) {
                    outOfRange++;
                    continue;
                }

                ClaimInstance claim = ClaimManager.getClaim(dimension, x, z);
                FactionInstance claimingFaction = ClaimManager.getFaction(dimension, x, z);
                if (claim != null && claimingFaction != null && claim.level != 5 && !selectedFaction.equals(claim.factionId)) {
                    claimedByOtherFaction++;
                    continue;
                }

                toTryClaim.add(new ChunkPos(x, z));
            }
        }

        if (outOfRange > 0) {
            sender.sendMessage(new TextComponentString(String.format("%1$d chunks where out of range", outOfRange)));
        }

        if (claimedByOtherFaction > 0) {
            sender.sendMessage(new TextComponentString(String.format("%1$d chunks where claimed by other factions", claimedByOtherFaction)));
        }


        int notEnoughResources = 0;

        int successfullyClaimedChunks = 0;

        int previousSize = Integer.MAX_VALUE;
        while (toTryClaim.size() != previousSize) {
            HashSet<ChunkPos> newTryInvade = new HashSet<>();
            for (ChunkPos tryInvade : toTryClaim) {
                if (level != 4 && !isValidPos(selectedFaction, dimension, tryInvade.x, tryInvade.z)) {
                    // We need to keep checking as it may become valid latet
                    newTryInvade.add(tryInvade);
                    continue;
                }

                if (!ClaimManager.takeRequiredItems(playerMP, level)) {
                    notEnoughResources++;
                    continue;
                }

                ClaimManager.claim(dimension, tryInvade.x, tryInvade.z, selectedFaction, level);
                successfullyClaimedChunks++;
            }

            previousSize = toTryClaim.size();
            toTryClaim = newTryInvade;
        }

        if (!toTryClaim.isEmpty()) {
            sender.sendMessage(new TextComponentString(String.format(
                    "%1$d chunks could not be claimed, because to claim a chunk one of their neighbours must be claimed by you or you must be claiming with level 4",
                    toTryClaim.size()
            )));
        }

        if (notEnoughResources > 0) {
            sender.sendMessage(new TextComponentString(String.format("You didn't have enough resources for %1$d chunks", notEnoughResources)));
        }

        if (successfullyClaimedChunks > 0) {
            sender.sendMessage(new TextComponentString(String.format("Claimed %1$d/%2$d chunks", successfullyClaimedChunks, totalChunks)));
        }
    }

    private boolean isValidPos(UUID selectedFaction, int dimension, int chunkX, int chunkZ) {
        ClaimInstance northClaim = ClaimManager.getClaim(dimension, chunkX, chunkZ - 1);
        if (northClaim != null && selectedFaction.equals(northClaim.factionId)) {
            return true;
        }
        ClaimInstance eastClaim = ClaimManager.getClaim(dimension, chunkX + 1, chunkZ);
        if (eastClaim != null && selectedFaction.equals(eastClaim.factionId)) {
            return true;
        }
        ClaimInstance southClaim = ClaimManager.getClaim(dimension, chunkX, chunkZ + 1);
        if (southClaim != null && selectedFaction.equals(southClaim.factionId)) {
            return true;
        }
        ClaimInstance westClaim = ClaimManager.getClaim(dimension, chunkX - 1, chunkZ);
        if (westClaim != null && selectedFaction.equals(westClaim.factionId)) {
            return true;
        }

        return false;
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
