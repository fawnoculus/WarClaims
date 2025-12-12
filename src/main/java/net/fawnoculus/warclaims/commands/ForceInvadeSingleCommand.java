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

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ForceInvadeSingleCommand extends CommandBase {
    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getName() {
        return "force-invade-single";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "force-invade-single <chunkX> <chunkZ>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2) {
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
            throw new NumberInvalidException("%1$s is not a valid integer", args[0]);
        }

        int chunkZ;
        try {
            chunkZ = Integer.parseInt(args[1]);
        } catch (NumberFormatException ignored) {
            throw new NumberInvalidException("%1$s is not a valid integer", args[0]);
        }

        ClaimInstance claim = ClaimManager.getClaim(dimension, chunkX, chunkZ);
        FactionInstance claimingFaction = ClaimManager.getFaction(dimension, chunkX, chunkZ);
        if (claim == null || claimingFaction == null) {
            throw new CommandException("Chunk is not claimed by anyone");
        }

        UUID selectedFaction = FactionManager.getSelectedFaction(playerMP);
        if (selectedFaction == null) {
            throw new CommandException("You must select or create a faction with /faction");
        }

        if (selectedFaction.equals(claim.factionId)) {
            throw new CommandException("A faction can't invade its own territory");
        }

        FactionInstance faction = FactionManager.getFaction(selectedFaction);
        if (faction == null) {
            throw new CommandException("The Team you have selected does not exist");
        }

        if (!InvasionManager.takeRequiredItems(playerMP)) {
            throw new CommandException("You don't have the resources required to start invading this chunk");
        }

        InvasionManager.addInvasion(dimension, chunkX, chunkZ, claim, selectedFaction);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length < 3) {
            return getListOfStringsMatchingLastWord(args, ImmutableList.of("-2", "0", "100", "420", "666"));
        }

        return Collections.emptyList();
    }
}
