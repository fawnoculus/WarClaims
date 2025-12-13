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
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ClaimSingleCommand extends CommandBase {
    @Override
    public String getName() {
        return "claim-single";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "claim-single <chunkX> <chunkZ> <level>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 3 || args[2].isEmpty()) {
            throw new CommandException("Not Enough Arguments: " + this.getUsage(sender));
        }

        if (!(sender.getCommandSenderEntity() instanceof EntityPlayerMP)) {
            throw new CommandException("Must be executed by a Player");
        }
        EntityPlayerMP playerMP = (EntityPlayerMP) sender.getCommandSenderEntity();
        int dimension = playerMP.dimension;

        int chunkX;
        try {
            chunkX = Integer.parseInt(args[0]);
        } catch (NumberFormatException ignored) {
            throw new NumberInvalidException("%1$s is not a valid integer (chunkX)", args[0]);
        }

        int chunkZ;
        try {
            chunkZ = Integer.parseInt(args[1]);
        } catch (NumberFormatException ignored) {
            throw new NumberInvalidException("%1$s is not a valid integer (chunkZ)", args[1]);
        }

        if (WarClaimsConfig.isOutOfClaimRange(playerMP.getPosition(), chunkX, chunkZ)) {
            throw new CommandException(String.format("Chunk is to far away, Max Claim Distance is: %1$d chunks", WarClaimsConfig.claimDistance));
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
                    "You do not have permission to claim chunks for \"%1$s\" you must be an officer or the owner",
                    faction.name
            ));
        }

        ClaimInstance claim = ClaimManager.getClaim(dimension, chunkX, chunkZ);
        FactionInstance claimingFaction = ClaimManager.getFaction(dimension, chunkX, chunkZ);
        if (claim != null && claimingFaction != null && claim.level != 5 && !selectedFaction.equals(claim.factionId)) {
            throw new CommandException(String.format("Chunk is already Claimed by %1$s", claimingFaction.name));
        }

        int level;
        try {
            level = Integer.parseInt(args[2]);
        } catch (NumberFormatException ignored) {
            throw new NumberInvalidException(String.format("%1$s is not a valid integer (level)", args[2]));
        }

        if (level < 0 || level > 4) {
            throw new NumberInvalidException(String.format("level %1$s invalid, must be between 0 and 4", args[2]));
        }

        if (level != 4 && !isValidPos(selectedFaction, dimension, chunkX, chunkZ)) {
            throw new CommandException("Chunks can only be claimed if one of their neighbours is claimed by you or if you are claiming with level 4");
        }

        if (!ClaimManager.takeRequiredItems(playerMP, level)) {
            throw new CommandException("You don't have the resources required to claim this chunk");
        }

        ClaimManager.claim(dimension, chunkX, chunkZ, selectedFaction, level);
        sender.sendMessage(new TextComponentString("Claimed the chunk at " + chunkX + ", " + chunkZ));
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
        if (args.length < 3) {
            return getListOfStringsMatchingLastWord(args, ImmutableList.of("-2", "0", "100", "420", "666"));
        }

        if (args.length == 3) {
            return getListOfStringsMatchingLastWord(args, ImmutableList.of("0", "1", "2", "3", "4"));
        }

        return Collections.emptyList();
    }
}
