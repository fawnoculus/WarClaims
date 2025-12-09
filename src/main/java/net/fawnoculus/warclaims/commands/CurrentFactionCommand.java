package net.fawnoculus.warclaims.commands;

import com.google.common.collect.ImmutableList;
import com.mojang.realmsclient.util.Pair;
import net.fawnoculus.warclaims.claims.faction.FactionInstance;
import net.fawnoculus.warclaims.claims.faction.FactionManager;
import net.fawnoculus.warclaims.utils.ColorUtil;
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

public class CurrentFactionCommand extends CommandBase {
    private static final ImmutableList<String> POSSIBLE_SUB_COMMANDS = ImmutableList.of("get", "set", "association", "set-status", "give-ownership", "add-ally", "remove-ally");
    private static final ImmutableList<String> MODIFY_SUB_COMMANDS = ImmutableList.of("name", "color");

    @Override
    public String getName() {
        return "current-faction";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "current-faction <set, get, list-members, list-officers, association, set-status, give-ownership>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) {
            throw new CommandException("Not Enough Arguments: " + this.getUsage(sender));
        }

        if (!(sender.getCommandSenderEntity() instanceof EntityPlayerMP)) {
            throw new CommandException("Must be executed by a Player");
        }
        EntityPlayerMP playerMP = (EntityPlayerMP) sender.getCommandSenderEntity();
        Pair<UUID, FactionInstance> pair = getCurrentFaction(playerMP);

        if ("set".equals(args[0])) {
            if (args.length < 3) {
                throw new CommandException("Not Enough Arguments: /current-faction set <name, color>");
            }

            checkOfficer(playerMP, pair.second(), "modify");

            if ("name".equals(args[1])) {
                FactionManager.setFaction(pair.first(), pair.second().withName(args[2]));
                sender.sendMessage(new TextComponentString(
                        String.format("Changed Faction name from \"%1$s\" to \"%2$s\"", pair.second().name, args[2])
                ));
                return;
            }

            if ("color".equals(args[1])) {
                if (args.length < 5) {
                    throw new CommandException("Not Enough Arguments: /current-faction set color <red> <green> <blue>");
                }

                int red;
                try {
                    red = Integer.parseInt(args[2]);
                } catch (NumberFormatException ignored) {
                    throw new NumberInvalidException("%1$s is not a valid integer (red)", args[2]);
                }
                if (red < 0 || red > 255) {
                    throw new NumberInvalidException("%1$s is out of range, must be between 0 and 255 (red)", args[2]);
                }

                int green;
                try {
                    green = Integer.parseInt(args[3]);
                } catch (NumberFormatException ignored) {
                    throw new NumberInvalidException("%1$s is not a valid integer (green)", args[3]);
                }
                if (green < 0 || green > 255) {
                    throw new NumberInvalidException("%1$s is out of range, must be between 0 and 255 (green)", args[3]);
                }

                int blue;
                try {
                    blue = Integer.parseInt(args[4]);
                } catch (NumberFormatException ignored) {
                    throw new NumberInvalidException("%1$s is not a valid integer (blue)", args[4]);
                }
                if (blue < 0 || blue > 255) {
                    throw new NumberInvalidException("%1$s is out of range, must be between 0 and 255 (blue)", args[4]);
                }

                int newColor = ColorUtil.fromRGB(red, green, blue);

                FactionManager.setFaction(pair.first(), pair.second().withColor(newColor));
                sender.sendMessage(new TextComponentString(
                        String.format("Changed Faction color from %1$s to %2$s", ColorUtil.getRGB(pair.second().color), ColorUtil.getRGB(newColor))
                ));
                return;
            }
        }

        if ("get".equals(args[0])) {
            if (args.length < 2) {
                throw new CommandException("Not Enough Arguments: /current-faction get <uuid, name, color>");
            }

            if ("uuid".equals(args[1])) {
                sender.sendMessage(new TextComponentString(
                        String.format("Current team uuid: %1$s", pair.first().toString())
                ));
                return;
            }

            if ("name".equals(args[1])) {
                sender.sendMessage(new TextComponentString(
                        String.format("Current team name: %1$s", pair.second().name)
                ));
                return;
            }

            if ("color".equals(args[1])) {
                sender.sendMessage(new TextComponentString(
                        String.format("Current team color: %1$s", ColorUtil.getRGB(pair.second().color))
                ));
                return;
            }
        }

        if ("give-ownership".equals(args[0])) {
            if (args.length < 2) {
                throw new CommandException("Not Enough Arguments: /current-faction give-ownership <player-name, player-uuid>");
            }

            if (!pair.second().isOwner(playerMP)) {
                throw new CommandException("You must own this faction to give ownership of it to someone");
            }

            UUID playerId = playerNameOrUuid(args[1], server);

            FactionManager.setFaction(pair.first(), pair.second().withOwner(playerId));
            sender.sendMessage(new TextComponentString(
                    String.format("Transferred ownership of \"%1$s\" to \"%2$s\"", pair.second().name, args[1])
            ));
            return;
        }

        if ("association".equals(args[0])) {
            if (args.length < 2) {
                throw new CommandException("Not Enough Arguments: /current-faction association <player-name, player-uuid>");
            }

            UUID playerId = playerNameOrUuid(args[1], server);

            if (pair.second().isOwner(playerId)) {
                sender.sendMessage(new TextComponentString(
                        String.format("\"%1$s\" is the Owner of \"%2$s\"", args[1], pair.second().name)
                ));
                return;
            }

            if (pair.second().officers.contains((playerId))) {
                sender.sendMessage(new TextComponentString(
                        String.format("\"%1$s\" is an Officer of \"%2$s\"", args[1], pair.second().name)
                ));
                return;
            }

            if (pair.second().members.contains(playerId)) {
                sender.sendMessage(new TextComponentString(
                        String.format("\"%1$s\" is a Member of \"%2$s\"", args[1], pair.second().name)
                ));
                return;
            }

            for (UUID allyId : pair.second().allies) {
                FactionInstance ally = FactionManager.getFaction(allyId);
                if (ally != null && ally.isMember(playerId)) {
                    sender.sendMessage(new TextComponentString(
                            String.format("\"%1$s\" is associated with \"%2$s\" which is an ally of \"%3$s\"", args[1], ally.name, pair.second().name)
                    ));
                    return;
                }
            }

            sender.sendMessage(new TextComponentString(
                    String.format("\"%1$s\" is not associated with \"%2$s\"", args[1], pair.second().name)
            ));
            return;
        }

        if ("set-status".equals(args[0])) {
            if (args.length < 3) {
                throw new CommandException("Not Enough Arguments: /current-faction set-status <player-name, player-uuid> <none, member, officer>");
            }

            checkOfficer(playerMP, pair.second(), "modify player permissions for");

            if ("none".equals(args[2])) {
                pair.second().setStatusNone(playerNameOrUuid(args[1], server));
                return;
            }

            if ("member".equals(args[2])) {
                pair.second().setStatusMember(playerNameOrUuid(args[1], server));
                return;
            }

            if ("officer".equals(args[2])) {
                pair.second().setStatusOfficer(playerNameOrUuid(args[1], server));
                return;
            }

            throw new CommandException("Invalid Argument: " + args[2] + " expected: none, member, officer");
        }

        if ("add-ally".equals(args[0])) {
            if (args.length < 2) {
                throw new CommandException("Not Enough Arguments: /current-faction add-ally <faction-name>");
            }

            UUID allyId = getFactionForAlly(pair.first(), args[1]);
            if (pair.second().allies.contains(allyId)) {
                throw new CommandException(String.format("Your faction (%1$s) is already allied to \"%2$s\"", pair.second().name, args[1]));
            }

            pair.second().allies.add(allyId);
            sender.sendMessage(new TextComponentString(
                    String.format("Your faction (%1$s) is now allied to \"%2$s\"", pair.second().name, args[1])
            ));

            return;
        }

        if ("remove-ally".equals(args[0])) {
            if (args.length < 2) {
                throw new CommandException("Not Enough Arguments: /current-faction remove-ally <faction-name>");
            }

            UUID allyId = getFactionForAlly(pair.first(), args[1]);
            if (!pair.second().allies.contains(allyId)) {
                throw new CommandException(String.format("Your faction (%1$s) is not allied to \"%2$s\"", pair.second().name, args[1]));
            }

            pair.second().allies.remove(allyId);
            sender.sendMessage(new TextComponentString(
                    String.format("Your faction (%1$s) is now no longer allied to \"%2$s\"", pair.second().name, args[1])
            ));

            return;
        }

        throw new CommandException("Invalid Argument: " + args[0]);
    }

    private void checkOfficer(EntityPlayerMP playerMP, FactionInstance faction, String permission) throws CommandException {
        if (!faction.isOfficer(playerMP)) {
            throw new CommandException(String.format(
                    "You do not have permission %1$s \"%2$s\" you must be an officer or the owner",
                    permission, faction.name
            ));
        }
    }

    private UUID getFactionForAlly(UUID currentFaction, String allyName) throws CommandException {
        UUID allyId = FactionManager.getFactionFromName(allyName);
        if (allyId == null) {
            throw new CommandException(String.format("No team with the name \"%1$s\" exits", allyName));
        }

        if (currentFaction.equals(allyId)) {
            throw new CommandException("You can not ally your team to itself");
        }

        return allyId;
    }

    private UUID playerNameOrUuid(String nameOrUuid, MinecraftServer server) throws CommandException {
        UUID playerId;
        try {
            EntityPlayerMP targetPlayer = server.getPlayerList().getPlayerByUsername(nameOrUuid);
            if (targetPlayer != null) {
                playerId = targetPlayer.getGameProfile().getId();
            } else {
                playerId = UUID.fromString(nameOrUuid);
            }
        } catch (IllegalArgumentException ignored) {
            throw new CommandException(String.format("\"%1$s\" is neither a currently online player nor a uuid", nameOrUuid));
        }

        return playerId;
    }

    private Pair<UUID, FactionInstance> getCurrentFaction(EntityPlayerMP playerMP) throws CommandException {
        UUID factionId = FactionManager.getSelectedFaction(playerMP);
        if (factionId == null) {
            throw new CommandException("You must select or create a faction with /faction");
        }

        FactionInstance faction = FactionManager.getFaction(factionId);
        if (faction == null) {
            throw new CommandException("The Team you have selected does not exist");
        }
        return Pair.of(factionId, faction);
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 2 && !("get".equals(args[0]) || "set".equals(args[0]) || "add-ally".equals(args[0]) || "remove-ally".equals(args[0]));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, POSSIBLE_SUB_COMMANDS);
        }

        if (args.length == 2) {
            if ("get".equals(args[0]) || "set".equals(args[0])) {
                return getListOfStringsMatchingLastWord(args, MODIFY_SUB_COMMANDS);
            }
            if ("add-ally".equals(args[0]) || "remove-ally".equals(args[0])) {
                return getListOfStringsMatchingLastWord(args, FactionManager.getFactionNames());
            }
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }

        return Collections.emptyList();
    }
}
