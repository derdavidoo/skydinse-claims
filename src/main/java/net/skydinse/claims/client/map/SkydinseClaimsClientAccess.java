package net.skydinse.claims.client.map;

import net.skydinse.claims.client.SkydinseClaimsClient;

public final class SkydinseClaimsClientAccess {
    private SkydinseClaimsClientAccess() {
    }

    public static ClaimedChunkStore store() {
        return SkydinseClaimsClient.CLAIMED_CHUNKS;
    }
}
