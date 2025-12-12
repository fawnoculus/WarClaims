package net.fawnoculus.warclaims.commands;

import com.google.common.collect.ImmutableList;
import net.fawnoculus.warclaims.WarClaimsConfig;
import net.fawnoculus.warclaims.claims.ClaimInstance;
import net.fawnoculus.warclaims.claims.ClaimManager;
import net.fawnoculus.warclaims.claims.faction.FactionInstance;
import net.fawnoculus.warclaims.claims.faction.FactionManager;
import net.fawnoculus.warclaims.claims.invade.InvasionKey;
import net.fawnoculus.warclaims.claims.invade.InvasionManager;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.NumberInvalidException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
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

        int outOfRangeChunks = 0;
        int notClaimedChunks = 0;
        int claimedByOwnFaction = 0;

        int totalChunks = 0;
        HashSet<ChunkPos> toTryInvade = new HashSet<>();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                totalChunks++;

                if (WarClaimsConfig.isOutOfInvasionRange(playerPos, x, z)) {
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

                toTryInvade.add(new ChunkPos(x, z));
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


        int notEnoughResources = 0;
        int successfullyInvadedChunks = 0;

        int previousSize = Integer.MAX_VALUE;
        while (toTryInvade.size() != previousSize) {
            HashSet<ChunkPos> newTryInvade = new HashSet<>();
            for (ChunkPos tryInvade : toTryInvade) {
                if (!isValidPos(selectedFaction, dimension, tryInvade.x, tryInvade.z)) {
                    // We need to keep checking as it may become valid later
                    newTryInvade.add(tryInvade);
                    continue;
                }

                if (!InvasionManager.takeRequiredItems(playerMP)) {
                    notEnoughResources++;
                    continue;
                }

                ClaimInstance claim = ClaimManager.getClaim(dimension, tryInvade.x, tryInvade.z);
                if (claim == null) {
                    throw new CommandException("Something went really wrong, claim == null");
                }

                InvasionManager.addInvasion(dimension, tryInvade.x, tryInvade.z, claim, selectedFaction);
                successfullyInvadedChunks++;
            }

            previousSize = toTryInvade.size();
            toTryInvade = newTryInvade;
        }

        if (!toTryInvade.isEmpty()) {
            sender.sendMessage(new TextComponentString(String.format(
                    "%1$s chunks could not be invaded because, To invade a chunk it must be next to a chunk you have a claim on or two or more chunks you are invading",
                    toTryInvade.size()
            )));
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

    private boolean isValidPos(UUID attackingTeam, int dimension, int chunkX, int chunkZ) {
        ClaimInstance northClaim = ClaimManager.getClaim(dimension, chunkX, chunkZ - 1);
        if (northClaim != null && attackingTeam.equals(northClaim.factionId)) {
            return true;
        }
        ClaimInstance eastClaim = ClaimManager.getClaim(dimension, chunkX + 1, chunkZ);
        if (eastClaim != null && attackingTeam.equals(eastClaim.factionId)) {
            return true;
        }
        ClaimInstance southClaim = ClaimManager.getClaim(dimension, chunkX, chunkZ + 1);
        if (southClaim != null && attackingTeam.equals(southClaim.factionId)) {
            return true;
        }
        ClaimInstance westClaim = ClaimManager.getClaim(dimension, chunkX - 1, chunkZ);
        if (westClaim != null && attackingTeam.equals(westClaim.factionId)) {
            return true;
        }

        int invasionCount = 0;

        InvasionKey northInvasion = InvasionManager.fromPos(dimension, chunkX, chunkZ - 1);
        if (northInvasion != null && attackingTeam.equals(northInvasion.attackingFaction)) {
            invasionCount++;
        }
        InvasionKey eastInvasion = InvasionManager.fromPos(dimension, chunkX + 1, chunkZ);
        if (eastInvasion != null && attackingTeam.equals(eastInvasion.attackingFaction)) {
            invasionCount++;
        }
        InvasionKey southInvasion = InvasionManager.fromPos(dimension, chunkX, chunkZ + 1);
        if (southInvasion != null && attackingTeam.equals(southInvasion.attackingFaction)) {
            invasionCount++;
        }
        InvasionKey westInvasion = InvasionManager.fromPos(dimension, chunkX - 1, chunkZ);
        if (westInvasion != null && attackingTeam.equals(westInvasion.attackingFaction)) {
            invasionCount++;
        }

        return invasionCount >= 2;
    }
}
