package net.fawnoculus.warclaims.commands;

import com.google.common.collect.ImmutableList;
import net.fawnoculus.warclaims.claims.ClaimManager;
import net.fawnoculus.warclaims.claims.faction.FactionManager;
import net.minecraft.command.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ForceClaimCommand extends CommandBase {
    @Override
    public String getName() {
        return "force-claim";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "force-claim <chunkX> <chunkZ> <level>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender.getCommandSenderEntity() instanceof EntityPlayerMP)) {
            throw new CommandException("Must be executed by a Player");
        }

        EntityPlayerMP player = (EntityPlayerMP) sender.getCommandSenderEntity();

        if (args.length < 3) {
            throw new SyntaxErrorException("Not enough arguments, force-claim <chunk-x> <chunk-z> <level>");
        }

        if (args.length > 3) {
            throw new SyntaxErrorException("To many arguments, force-claim <chunk-x> <chunk-z> <level>");
        }

        int chunkX;
        int chunkZ;
        int level;

        if(args[0].equals("~")) {
            chunkX = sender.getPosition().getX() >> 4;
        } else {
            try {
                chunkX = Integer.parseInt(args[0]);
            }catch (NumberFormatException ignored) {
                throw new NumberInvalidException("chunk-x invalid");
            }
        }

        if(args[1].equals("~")) {
            chunkZ = sender.getPosition().getZ() >> 4;
        } else {
            try {
                chunkZ = Integer.parseInt(args[1]);
            }catch (NumberFormatException ignored) {
                throw new NumberInvalidException("chunk-z invalid");
            }
        }

            try {
                level = Integer.parseInt(args[2]);
            } catch (NumberFormatException ignored) {
                throw new NumberInvalidException("level invalid");
            }

        UUID selectedTeam = FactionManager.getSelectedFaction(player);
        if (selectedTeam == null) {
            throw new CommandException("You must select or create a faction with /faction");
        }

        ClaimManager.claim(player.dimension, chunkX, chunkZ, selectedTeam, level);
        sender.sendMessage(new TextComponentString("Claimed chunk at " + chunkX + " " + chunkZ + " at level " + level + " for selected Team"));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length < 3) {
            return getListOfStringsMatchingLastWord(args, ImmutableList.of("~"));
        }

        if (args.length == 3) {
            return getListOfStringsMatchingLastWord(args, ImmutableList.of("0", "1", "2", "3", "4", "5"));
        }

        return Collections.emptyList();
    }
}
