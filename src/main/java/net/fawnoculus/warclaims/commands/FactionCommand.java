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

public class FactionCommand extends CommandBase {
    private static final ImmutableList<String> POSSIBLE_SUB_COMMANDS = ImmutableList.of("create", "delete", "set-current", "list-all", "accept-invite");

    @Override
    public String getName() {
        return "faction";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/faction <create, delete, set-current, list-all, accept-invite> (Use /current-team to modify stuff)";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) {
            throw new SyntaxErrorException("possible options: <create, delete, set-current, accept-invite>");
        }

        if (args[0].equals("accept-invite")) {
            throw new SyntaxErrorException("mendes (TODO)"); // TODO
        }

        if (args[0].equals("list-all")) {
            sender.sendMessage(new TextComponentString("All current teams: "));
            for (String name : FactionManager.getFactionNames()) {
                sender.sendMessage(new TextComponentString(name + ": " + FactionManager.getFactionFromName(name)));
            }
            return;
        }

        if (args[0].equals("set-current")) {
            if(!(sender.getCommandSenderEntity() instanceof EntityPlayerMP)) {
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

            if (!team.isMember(playerMP)) {
                throw new CommandException("You are not a member of '" + args[1] + "' you cannot select it");
            }

            FactionManager.setSelectedFaction(playerMP, uuid);
            sender.sendMessage(new TextComponentString("Selected Team '" + args[1] + "'"));
            return;
        }

        if (args[0].equals("create")) {
            if(!(sender.getCommandSenderEntity() instanceof EntityPlayerMP)) {
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

        if (args[0].equals("delete")) {
            if(!(sender.getCommandSenderEntity() instanceof EntityPlayerMP)) {
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
                throw new CommandException("Not team with uuid '" + uuid + "' exists, how?????");
            }

            if (!team.isOfficer(playerMP)) {
                throw new CommandException("Only the owner of a team can delete it. you don't own " + args[1]);
            }

            FactionManager.removeFaction(uuid);
            sender.sendMessage(new TextComponentString("Successfully deleted Team '" + args[1] + "'"));
            return;
        }

        throw new SyntaxErrorException("invalid argument, possible options: <create, delete, set-current, accept-invite>");
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if(args.length == 1) {
            return getListOfStringsMatchingLastWord(args, POSSIBLE_SUB_COMMANDS);
        }

        if (args[0].equals("list-all") || args[0].equals("accept-invite")) {
            return Collections.emptyList();
        }

        return getListOfStringsMatchingLastWord(args, FactionManager.getFactionNames());
    }
}
