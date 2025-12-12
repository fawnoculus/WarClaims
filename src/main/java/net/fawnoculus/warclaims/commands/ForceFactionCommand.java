package net.fawnoculus.warclaims.commands;

import com.google.common.collect.ImmutableList;
import net.fawnoculus.warclaims.claims.faction.FactionInstance;
import net.fawnoculus.warclaims.claims.faction.FactionManager;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.SyntaxErrorException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ForceFactionCommand extends CommandBase {
    private static final ImmutableList<String> POSSIBLE_SUB_COMMANDS = ImmutableList.of("create", "delete", "set-current", "list-all");

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getName() {
        return "force-faction";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/force-faction <create, delete, set-current, list-all> (Use /current-team to modify stuff)";
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

            if (args.length < 2) {
                throw new SyntaxErrorException("usage: /team create <team-name>");
            }
            UUID uuid = FactionManager.makeFaction(playerMP, args[1]);
            FactionManager.setSelectedFaction(playerMP, uuid);
            sender.sendMessage(new TextComponentString("Successfully created and selected new Team '" + args[1] + "'"));
            return;
        }

        if ("delete".equals(args[0])) {
            if (args.length < 2) {
                throw new SyntaxErrorException("usage: /team delete <team-name>");
            }

            UUID uuid = FactionManager.getFactionFromName(args[1]);
            if (uuid == null) {
                throw new CommandException("Not team with the name '" + args[1] + "' exists");
            }

            FactionInstance team = FactionManager.getFaction(uuid);
            if (team == null) {
                throw new CommandException("Not team with uuid '" + uuid + "' exists, how?????");
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

        if (args[0].equals("list-all") || args.length > 2) {
            return Collections.emptyList();
        }

        return getListOfStringsMatchingLastWord(args, FactionManager.getFactionNames());
    }
}
