package net.fawnoculus.warclaims.commands;

import com.google.common.collect.ImmutableList;
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

public class ForceUnclaimSingleCommand extends CommandBase {
    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getName() {
        return "force-unclaim-single";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "force-unclaim-single <chunkX> <chunkZ>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2 || args[1].isEmpty()) {
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

        FactionInstance claimingFaction = ClaimManager.getFaction(dimension, chunkX, chunkZ);
        if (claimingFaction == null) {
            throw new CommandException("Cannot unclaim Chunk at %1$s because it is not claimed", chunkX + "," + chunkZ);
        }

        ClaimManager.unclaim(dimension, chunkX, chunkZ);
        sender.sendMessage(new TextComponentString("Force Unclaimed the chunk at " + chunkX + ", " + chunkZ));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length < 3) {
            return getListOfStringsMatchingLastWord(args, ImmutableList.of("-2", "0", "100", "420", "666"));
        }

        return Collections.emptyList();
    }
}
