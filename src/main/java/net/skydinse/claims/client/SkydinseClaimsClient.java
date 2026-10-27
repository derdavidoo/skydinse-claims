package net.skydinse.claims.client;

import java.util.List;
import java.util.Set;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.level.Level;
import net.skydinse.claims.client.map.ClaimedChunk;
import net.skydinse.claims.client.map.ClaimedChunkStore;
import net.skydinse.claims.client.map.ClaimedChunkPersistence;
import net.skydinse.claims.client.map.SkydinseServer;
import org.slf4j.LoggerFactory;
import xaero.map.WorldMapSession;

public final class SkydinseClaimsClient implements ClientModInitializer {
    private static final double MAX_CAPTURE_SPEED_BLOCKS_PER_SECOND = 20.0;
    private static final int MAX_CAPTURE_PING_MS = 90;
    public static final ClaimedChunkStore CLAIMED_CHUNKS = new ClaimedChunkStore();
    private final ClaimListParser parser = new ClaimListParser();
    private final ScoreboardRecorder scoreboardRecorder = new ScoreboardRecorder();
    private CloudSyncManager cloudSync;
    private static boolean highlightsEnabled = true;
    private String loadedServer;
    private double lastPlayerX;
    private double lastPlayerY;
    private double lastPlayerZ;
    private long lastPlayerSampleNanos;
    private boolean hasPlayerSample;

    public static boolean highlightsEnabled() {
        return highlightsEnabled;
    }

    public static void setHighlightsEnabled(boolean enabled) {
        highlightsEnabled = enabled;
        clearMapHighlightCache();
    }

    public static boolean isLocalOwner(String owner, Minecraft client) {
        if (client.player == null || owner == null) return false;
        String profileName = client.player.getGameProfile().name();
        String displayName = client.player.getName().getString();
        return owner.trim().equalsIgnoreCase(profileName) || owner.trim().equalsIgnoreCase(displayName);
    }

    public static void clearMapHighlightCache() {
        WorldMapSession session = WorldMapSession.getCurrentSession();
        if (session != null && session.isUsable()) {
            session.getMapProcessor().getMapWorld().clearAllCachedHighlightHashes();
        }
    }

    @Override
    public void onInitializeClient() {
        cloudSync = new CloudSyncManager(CloudSyncConfig.load(Minecraft.getInstance()));
        ClientSendMessageEvents.COMMAND.register(command -> {
            if (SkydinseServer.isActive()) parser.commandSent(command);
        });
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay && SkydinseServer.isActive()) parser.accept(message.getString());
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean changed = SkydinseServer.refreshState();
            boolean active = SkydinseServer.isActive();
            if (!active || client.level == null) {
                parser.reset();
                scoreboardRecorder.reset();
                if (!CLAIMED_CHUNKS.snapshot().isEmpty()) {
                    CLAIMED_CHUNKS.clear();
                    changed = true;
                }
                loadedServer = null;
                resetCaptureSample();
                cloudSync.stop();
            } else {
                String serverAddress = SkydinseServer.address();
                boolean serverChanged = !serverAddress.equals(loadedServer);
                boolean localChanged = false;
                if (serverChanged) {
                    CLAIMED_CHUNKS.replace(ClaimedChunkPersistence.load(serverAddress));
                    loadedServer = serverAddress;
                    resetCaptureSample();
                    changed = true;
                }
                Set<ClaimListParser.Entry> claims = parser.tick();
                String scoreboardOwner = scoreboardRecorder.findClaimsOwner(client);
                scoreboardRecorder.tick(client);
                boolean captureAllowed = claimCaptureAllowed(client);
                if (captureAllowed && scoreboardOwner != null && client.player != null) {
                    ClaimedChunk current = new ClaimedChunk(client.level.dimension(),
                            client.player.chunkPosition().x(), client.player.chunkPosition().z(), scoreboardOwner);
                    localChanged |= CLAIMED_CHUNKS.put(current);
                    changed |= localChanged;
                }
                if (captureAllowed && claims != null && scoreboardOwner != null) {
                    localChanged |= CLAIMED_CHUNKS.append(claims.stream().map(entry -> new ClaimedChunk(
                            switch (entry.dimension()) {
                                case "minecraft:the_nether" -> Level.NETHER;
                                case "minecraft:the_end" -> Level.END;
                                default -> Level.OVERWORLD;
                            }, entry.chunkX(), entry.chunkZ(), entry.owner())).toList());
                    changed |= localChanged;
                    LoggerFactory.getLogger("skydinseclaims").info("Updated {} claimed chunks from /claim list", claims.size());
                }
                if (serverChanged) {
                    cloudSync.start(serverAddress, CLAIMED_CHUNKS.snapshot());
                } else {
                    cloudSync.tick();
                }
                List<ClaimedChunk> pulled = cloudSync.drainPulled();
                boolean pulledChanged = CLAIMED_CHUNKS.append(pulled);
                changed |= pulledChanged;
                if (localChanged) cloudSync.push(serverAddress, CLAIMED_CHUNKS.snapshot());
            }
            if (active && loadedServer != null && changed) {
                ClaimedChunkPersistence.save(loadedServer, CLAIMED_CHUNKS.snapshot());
            }
            if (changed) clearMapHighlightCache();
        });
    }

    private boolean claimCaptureAllowed(Minecraft client) {
        if (client.player == null || client.getConnection() == null) {
            resetCaptureSample();
            return false;
        }

        long now = System.nanoTime();
        double x = client.player.getX();
        double y = client.player.getY();
        double z = client.player.getZ();
        boolean validSample = hasPlayerSample && now > lastPlayerSampleNanos;
        double speed = 0.0;
        if (validSample) {
            double elapsedSeconds = (now - lastPlayerSampleNanos) / 1_000_000_000.0;
            double dx = x - lastPlayerX;
            double dy = y - lastPlayerY;
            double dz = z - lastPlayerZ;
            speed = Math.sqrt(dx * dx + dy * dy + dz * dz) / elapsedSeconds;
        }
        lastPlayerX = x;
        lastPlayerY = y;
        lastPlayerZ = z;
        lastPlayerSampleNanos = now;
        hasPlayerSample = true;

        PlayerInfo info = client.getConnection().getPlayerInfo(client.player.getUUID());
        int ping = info == null ? Integer.MAX_VALUE : info.getLatency();
        return validSample && speed <= MAX_CAPTURE_SPEED_BLOCKS_PER_SECOND && ping < MAX_CAPTURE_PING_MS;
    }

    private void resetCaptureSample() {
        hasPlayerSample = false;
        lastPlayerSampleNanos = 0L;
    }
}
