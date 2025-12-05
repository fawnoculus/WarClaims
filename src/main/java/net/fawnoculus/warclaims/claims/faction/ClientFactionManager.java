package net.fawnoculus.warclaims.claims.faction;

import net.fawnoculus.warclaims.claims.ClientClaimManager;
import net.fawnoculus.warclaims.networking.messages.FactionSyncMessage;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.UUID;

public class ClientFactionManager {
    private static final HashMap<UUID, FactionInstance> TEAMS = new HashMap<>();

    public static @Nullable FactionInstance get(UUID factionId) {
        return TEAMS.get(factionId);
    }

    public static void update(FactionSyncMessage message) {
        for (UUID uuid : message.getMap().keySet()) {
            FactionInstance team = message.getMap().get(uuid);

            if (team == null) {
                TEAMS.remove(uuid);
                continue;
            }

            if (TEAMS.containsKey(uuid)) {
                ClientClaimManager.updateClaimIf(claim -> claim.factionId.equals(uuid));
            }

            TEAMS.put(uuid, team);
        }
    }

    public static void clear() {
        TEAMS.clear();
    }
}
