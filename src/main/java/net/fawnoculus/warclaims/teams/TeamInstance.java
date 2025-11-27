package net.fawnoculus.warclaims.teams;

import net.minecraft.entity.player.EntityPlayerMP;

import java.util.HashSet;
import java.util.UUID;

public class TeamInstance {
    public final UUID teamId;
    public final UUID owner;
    public final HashSet<UUID> officers;
    public final HashSet<UUID> members;
    public final HashSet<UUID> allies;

    public TeamInstance(EntityPlayerMP owner) {
        this.teamId = UUID.randomUUID();
        this.owner = owner.getGameProfile().getId();
        officers = new HashSet<>();
        members = new HashSet<>();
        allies = new HashSet<>();
    }
}
