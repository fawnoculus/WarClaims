package net.fawnoculus.warclaims.commands;

import com.google.common.collect.ImmutableList;
import net.fawnoculus.warclaims.WarClaimsConfig;
import net.fawnoculus.warclaims.claims.ClaimManager;
import net.fawnoculus.warclaims.claims.faction.FactionInstance;
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

public class FactionCommand extends CommandBase {
    private static final ImmutableList<String> POSSIBLE_SUB_COMMANDS = ImmutableList.of("create", "delete", "set-current", "list-all");

    @Override
    public String getName() {
        return "faction";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/faction <create, delete, set-current, list-all> (Use /current-team to modify stuff)";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) {
            throw new SyntaxErrorException("Not enough Arguments: " + this.getUsage(sender));
        }

        if ("list-all".equals(args[0])) {
            sender.sendMessage(new TextComponentString("All current teams: "));
            for (String name : FactionManager.getFactionNames()) {
                sender.sendMessage(new TextComponentString(name + ": " + FactionManager.getFactionFromName(name)));
            }
            return;
        }

        if ("set-current".equals(args[0])) {
            if (!(sender.getCommandSenderEntity() instanceof EntityPlayerMP)) {
                throw new CommandException("must be executed by a player");
            }
            EntityPlayerMP playerMP = (EntityPlayerMP) sender.getCommandSenderEntity();

            if (args.length < 2) {
                throw new SyntaxErrorException("usage: /team set-current <team-name>");
            }

            UUID uuid = FactionManager.getFactionFromName(args[1]);
            if (uuid == null) {
                throw new CommandException("Not team with the name '" + args[1] + "' exists");
            }

            FactionInstance team = FactionManager.getFaction(uuid);
            if (team == null) {
                throw new CommandException("Not team with uuid '" + uuid + "' exists, how?????");
            }

            FactionManager.setSelectedFaction(playerMP, uuid);
            sender.sendMessage(new TextComponentString("Selected Team '" + args[1] + "'"));
            return;
        }

        if ("create".equals(args[0])) {
            if (!(sender.getCommandSenderEntity() instanceof EntityPlayerMP)) {
                throw new CommandException("must be executed by a player");
            }
            EntityPlayerMP playerMP = (EntityPlayerMP) sender.getCommandSenderEntity();
            int dimension = playerMP.dimension;

            if (args.length < 4) {
                throw new SyntaxErrorException("usage: /team create <team-name> <capital-chunk-x> <capital-chunk-z>");
            }

            int chunkX;
            if ("~".equals(args[2])) {
                chunkX = playerMP.chunkCoordX;
            } else {
                try {
                    chunkX = Integer.parseInt(args[2]);
                } catch (NumberFormatException ignored) {
                    throw new NumberInvalidException(String.format("%1$s is not a valid integer (capital-chunk-x)", args[0]));
                }
            }

            int chunkZ;
            if ("~".equals(args[3])) {
                chunkZ = playerMP.chunkCoordZ;
            } else {
                try {
                    chunkZ = Integer.parseInt(args[3]);
                } catch (NumberFormatException ignored) {
                    throw new NumberInvalidException(String.format("%1$s is not a valid integer (capital-chunk-z)", args[1]));
                }
            }

            if (WarClaimsConfig.isOutOfClaimRange(playerMP.getPosition(), chunkX, chunkZ)) {
                throw new CommandException(String.format("Capital Chunk is to far away, Max Claim Distance is: %1$d chunks", WarClaimsConfig.claimDistance));
            }

            FactionInstance claimingFaction = ClaimManager.getFaction(dimension, chunkX, chunkZ);
            if (claimingFaction != null) {
                throw new CommandException(String.format("Capital Chunk is already Claimed by %1$s", claimingFaction.name));
            }

            if (!ClaimManager.takeRequiredItems(playerMP, 5)) {
                throw new CommandException("You don't have the resources required to claim your capital chunk");
            }

            UUID factionId = FactionManager.makeFaction(playerMP, args[1]);
            ClaimManager.claim(dimension, chunkX, chunkZ, factionId, 5);
            FactionManager.setSelectedFaction(playerMP, factionId);
            sender.sendMessage(new TextComponentString(String.format(
                    "Successfully created and selected new Team '%1$s' with capital at %2$d, %3$d",
                    args[1], chunkX, chunkZ
            )));
            return;
        }

        if ("delete".equals(args[0])) {
            if (!(sender.getCommandSenderEntity() instanceof EntityPlayerMP)) {
                throw new CommandException("must be executed by a player");
            }
            EntityPlayerMP playerMP = (EntityPlayerMP) sender.getCommandSenderEntity();

            if (args.length < 2) {
                throw new SyntaxErrorException("usage: /team delete <team-name>");
            }

            UUID uuid = FactionManager.getFactionFromName(args[1]);
            if (uuid == null) {
                throw new CommandException("Not team with the name '" + args[1] + "' exists");
            }

            FactionInstance team = FactionManager.getFaction(uuid);
            if (team == null) {
                throw new CommandException("Not team with uuid '" + uuid + "' exists (how?????)");
            }

            if (!team.isOwner(playerMP)) {
                throw new CommandException("Only the owner of a team can delete it. you don't own " + args[1]);
            }

            FactionManager.removeFaction(uuid);
            sender.sendMessage(new TextComponentString("Successfully deleted Team '" + args[1] + "'"));
            return;
        }

        throw new SyntaxErrorException("Invalid Arguments: " + this.getUsage(sender));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, POSSIBLE_SUB_COMMANDS);
        }

        if ("list-all".equals(args[0])) {
            return Collections.emptyList();
        }

        if ("create".equals(args[0]) && args.length >= 3 && args.length <= 4) {
            return ImmutableList.of("~");
        }

        return getListOfStringsMatchingLastWord(args, FactionManager.getFactionNames());
    }
}
