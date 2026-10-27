package net.skydinse.claims.client.map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SkydinseServer {
    private static final String ROOT_DOMAIN = "skydinse.net";
    private static final Logger LOGGER = LoggerFactory.getLogger("skydinseclaims");

    private static String lastAddress;
    private static Boolean lastActive;

    private SkydinseServer() {
    }

    public static boolean isActive() {
        ServerData server = Minecraft.getInstance().getCurrentServer();
        if (server == null || server.ip == null) {
            logState(null, false);
            return false;
        }

        String address = server.ip.trim();
        String host = address.toLowerCase(java.util.Locale.ROOT);
        int slash = host.indexOf('/');
        if (slash >= 0) {
            host = host.substring(slash + 1);
        }
        if (host.startsWith("https://")) {
            host = host.substring("https://".length());
        } else if (host.startsWith("http://")) {
            host = host.substring("http://".length());
        }
        if (host.startsWith("[")) {
            int closingBracket = host.indexOf(']');
            if (closingBracket >= 0) {
                host = host.substring(1, closingBracket);
            }
        } else {
            int colon = host.lastIndexOf(':');
            if (colon >= 0 && host.indexOf(':') == colon) {
                host = host.substring(0, colon);
            }
        }

        boolean active = host.equals(ROOT_DOMAIN) || host.endsWith("." + ROOT_DOMAIN);
        logState(address, active);
        return active;
    }

    public static boolean refreshState() {
        Boolean previous = lastActive;
        boolean active = isActive();
        return previous != null && previous != active;
    }

    public static String address() {
        ServerData server = Minecraft.getInstance().getCurrentServer();
        return server == null || server.ip == null ? "unknown" : server.ip.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static void logState(String address, boolean active) {
        if (!java.util.Objects.equals(lastAddress, address) || !java.util.Objects.equals(lastActive, active)) {
            LOGGER.info("Skydinse Claims server filter: address={}, active={}", address, active);
            lastAddress = address;
            lastActive = active;
        }
    }
}
