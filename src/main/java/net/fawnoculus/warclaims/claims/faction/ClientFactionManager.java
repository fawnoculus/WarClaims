package net.fawnoculus.warclaims.claims.faction;

import net.fawnoculus.warclaims.claims.ClientClaimManager;
import net.fawnoculus.warclaims.networking.messages.FactionSyncMessage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SideOnly(Side.CLIENT)
public class ClientFactionManager {
    private static final Map<UUID, FactionInstance> TEAMS = new HashMap<>();

    public static @Nullable FactionInstance get(UUID factionId) {
        return TEAMS.get(factionId);
    }

    public static void update(FactionSyncMessage message) {
        for (Map.Entry<UUID, FactionInstance> entry : message.getMap().entrySet()) {
            if (entry.getValue() == null) {
                TEAMS.remove(entry.getKey());
                continue;
            }

            if (TEAMS.containsKey(entry.getKey())) {
                ClientClaimManager.updateClaimIf(claim -> claim.factionId.equals(entry.getKey()));
            }

            TEAMS.put(entry.getKey(), entry.getValue());
        }
    }

    public static void clear() {
        TEAMS.clear();
    }
}
