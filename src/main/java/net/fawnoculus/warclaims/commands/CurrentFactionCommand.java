package net.fawnoculus.warclaims.commands;

import com.google.common.collect.ImmutableList;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class CurrentFactionCommand extends CommandBase {
    private static final ImmutableList<String> POSSIBLE_SUB_COMMANDS = ImmutableList.of("modify", "invite", "kick", "promote", "demote", "give-ownership");
    private static final ImmutableList<String> MODIFY_SUB_COMMANDS = ImmutableList.of("name", "color");

    @Override
    public String getName() {
        return "current-faction";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "current-faction <modify, invite, kick, promote, demote, give-ownership>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        throw  new CommandException("TODO: this"); // TODO: this
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return args.length >= 1 && !Objects.equals(args[0], "modify") && index == 2;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if(args.length == 1) {
            return getListOfStringsMatchingLastWord(args, POSSIBLE_SUB_COMMANDS);
        }

        if(args.length == 2) {
            if (args[0].equals("modify")) {
                return getListOfStringsMatchingLastWord(args, MODIFY_SUB_COMMANDS);
            }
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }

        return Collections.emptyList();
    }
}
