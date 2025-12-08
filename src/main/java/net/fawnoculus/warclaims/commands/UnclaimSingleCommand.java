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

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class UnclaimSingleCommand extends CommandBase {
    @Override
    public String getName() {
        return "unclaim-single";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "unclaim-single <chunkX> <chunkZ>";
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
        }catch (NumberFormatException ignored) {
            throw new NumberInvalidException("%1$s is not a valid integer (chunkX)", args[0]);
        }

        int chunkZ;
        try {
            chunkZ = Integer.parseInt(args[1]);
        }catch (NumberFormatException ignored) {
            throw new NumberInvalidException("%1$s is not a valid integer (chunkZ)", args[1]);
        }

        if (!WarClaimsConfig.isInClaimRange(playerMP.getPosition(), chunkX, chunkZ)) {
            throw new CommandException(String.format("Chunk is to far away, Max Claim Distance is: %1$d chunks", WarClaimsConfig.claimDistance));
        }

        FactionInstance claimingFaction = ClaimManager.getFaction(dimension, chunkX, chunkZ);
        if (claimingFaction == null) {
            throw new CommandException("Cannot unclaim Chunk at %1$s because it is not claimed", chunkX + "," + chunkZ);
        }

        if (!claimingFaction.isOfficer(playerMP)) {
            throw new CommandException(
                    "You do not have permission to unclaim chunks for \"%1$s\" you must be an officer or the owner",
                    claimingFaction.name
            );
        }

        ClaimManager.unclaim(dimension, chunkX, chunkZ);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length < 3) {
            return getListOfStringsMatchingLastWord(args, ImmutableList.of("-2", "0", "100", "420", "666"));
        }

        return Collections.emptyList();
    }
}
