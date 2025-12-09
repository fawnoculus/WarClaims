package net.fawnoculus.warclaims.commands;

import com.google.common.collect.ImmutableList;
import net.fawnoculus.warclaims.WarClaimsConfig;
import net.fawnoculus.warclaims.claims.ClaimInstance;
import net.fawnoculus.warclaims.claims.ClaimManager;
import net.fawnoculus.warclaims.claims.faction.FactionInstance;
import net.fawnoculus.warclaims.claims.faction.FactionManager;
import net.fawnoculus.warclaims.claims.invade.InvasionManager;
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
import java.util.UUID;

public class InvadeSelectionCommand extends CommandBase {
    @Override
    public String getName() {
        return "invade-selection";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "invade-selection <start-chunkX> <start-chunkZ> <end-chunkX> <end-chunkZ>";
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

        UUID selectedFaction = FactionManager.getSelectedFaction(playerMP);
        if (selectedFaction == null) {
            throw new CommandException("You must select or create a faction with /faction");
        }

        FactionInstance faction = FactionManager.getFaction(selectedFaction);
        if (faction == null) {
            throw new CommandException("The Team you have selected does not exist");
        }

        if (!faction.isOfficer(playerMP)) {
            throw new CommandException(String.format(
                    "You do not have permission to invade chunks for \"%1$s\" you must be an officer or the owner",
                    faction.name
            ));
        }

        final int minX = Math.min(startChunkX, endChunkX);
        final int maxX = Math.max(startChunkX, endChunkX);
        final int minZ = Math.min(startChunkZ, endChunkZ);
        final int maxZ = Math.max(startChunkZ, endChunkZ);

        if (arePositionsInvalid(dimension, minX, maxX, minZ, maxZ)) {
            throw new CommandException(
                    "You cannot invade chunks that don't have either one neighbouring chunk not claimed by the attacked faction or two or more neighbouring chunks being invaded"
            );
        }

        int outOfRangeChunks = 0;
        int notClaimedChunks = 0;
        int claimedByOwnFaction = 0;
        int notEnoughResources = 0;

        int successfullyInvadedChunks = 0;
        int totalChunks = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                totalChunks++;

                if (!WarClaimsConfig.isInInvasionRange(playerPos, x, z)) {
                    outOfRangeChunks++;
                    continue;
                }

                ClaimInstance claim = ClaimManager.getClaim(dimension, x, z);
                FactionInstance owningFaction = ClaimManager.getFaction(dimension, x, z);
                if (claim == null || owningFaction == null) {
                    notClaimedChunks++;
                    continue;
                }

                if (selectedFaction.equals(claim.factionId)) {
                    claimedByOwnFaction++;
                    continue;
                }

                if (!InvasionManager.takeRequiredItems(playerMP)) {
                    notEnoughResources++;
                    continue;
                }

                InvasionManager.addInvasion(dimension, x, z, claim, selectedFaction);
                successfullyInvadedChunks++;
            }
        }

        if (outOfRangeChunks > 0) {
            sender.sendMessage(new TextComponentString(String.format("%1$s chunks where out of range", outOfRangeChunks)));
        }

        if (notClaimedChunks > 0) {
            sender.sendMessage(new TextComponentString(String.format("%1$s chunks where not Claimed", notClaimedChunks)));
        }

        if (claimedByOwnFaction > 0) {
            sender.sendMessage(new TextComponentString(String.format("%1$s chunks where claimed by your selected faction", claimedByOwnFaction)));
        }

        if (notEnoughResources > 0) {
            sender.sendMessage(new TextComponentString(String.format("You didn't have enough resources for %1$s chunks", notEnoughResources)));
        }

        if (successfullyInvadedChunks > 0) {
            sender.sendMessage(new TextComponentString(String.format("Started Invading %1$s/%2$s chunks", successfullyInvadedChunks, totalChunks)));
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length < 5) {
            return getListOfStringsMatchingLastWord(args, ImmutableList.of("-2", "0", "100", "420", "666"));
        }

        return Collections.emptyList();
    }

    private boolean arePositionsInvalid(int dimension, int minX, int maxX, int minZ, int maxZ) {
        int invalidPositions = 0;

        if (isInvalidPos(dimension, minX, minZ)) {
            invalidPositions++;
        }

        if (isInvalidPos(dimension, minX, maxZ)) {
            invalidPositions++;
        }

        if (isInvalidPos(dimension, maxX, minZ)) {
            invalidPositions++;
        }

        if (isInvalidPos(dimension, maxX, maxZ)) {
            invalidPositions++;
        }

        return invalidPositions > 1;
    }

    private boolean isInvalidPos(int dimension, int chunkX, int chunkZ) {
        ClaimInstance claim = ClaimManager.getClaim(dimension, chunkX, chunkZ);
        if (claim == null) {
            return false;
        }

        int claimedNeighbours = 0;
        int invadedNeighbours = 0;

        ClaimInstance northClaim = ClaimManager.getClaim(dimension, chunkX, chunkZ - 1);
        if (northClaim != null && claim.factionId.equals(northClaim.factionId)) {
            claimedNeighbours++;
        }
        ClaimInstance eastClaim = ClaimManager.getClaim(dimension, chunkX + 1, chunkZ);
        if (eastClaim != null && claim.factionId.equals(eastClaim.factionId)) {
            claimedNeighbours++;
        }
        ClaimInstance southClaim = ClaimManager.getClaim(dimension, chunkX, chunkZ + 1);
        if (southClaim != null && claim.factionId.equals(southClaim.factionId)) {
            claimedNeighbours++;
        }
        ClaimInstance westClaim = ClaimManager.getClaim(dimension, chunkX - 1, chunkZ);
        if (westClaim != null && claim.factionId.equals(westClaim.factionId)) {
            claimedNeighbours++;
        }

        if (InvasionManager.fromPos(dimension, chunkX, chunkZ - 1) != null) {
            invadedNeighbours++;
        }
        if (InvasionManager.fromPos(dimension, chunkX + 1, chunkZ) != null) {
            invadedNeighbours++;
        }
        if (InvasionManager.fromPos(dimension, chunkX, chunkZ + 1) != null) {
            invadedNeighbours++;
        }
        if (InvasionManager.fromPos(dimension, chunkX - 1, chunkZ) != null) {
            invadedNeighbours++;
        }

        return claimedNeighbours == 4 && invadedNeighbours < 2;
    }
}
