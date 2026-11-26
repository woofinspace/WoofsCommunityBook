package io.github.woofinspace.communitybook;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Server side of the Government menu: runs the buttons' commands and sends the menu its state. */
public final class GovernmentMenu {
    /** Keeps the menu payload under the network size limit. */
    public static final int MAX_LISTING = 8000;

    private GovernmentMenu() {}

    /** Registers the handler for button presses. Call once at startup. */
    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(GovActionPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            MinecraftServer server = context.server();
            server.execute(() -> {
                String command = payload.command();
                // Only government commands can come through here.
                if (command.startsWith("government ")) {
                    server.getCommands().performPrefixedCommand(player.createCommandSourceStack(), command);
                }
                send(player, server);
            });
        });
    }

    /** Sends the current roles and listing to one player. Opens the menu on their client if it isn't open. */
    public static void send(ServerPlayer player, MinecraftServer server) {
        GovConfig.Data cfg = GovConfig.load();
        String roles;
        String listing;
        if (!cfg.government.enabled) {
            roles = "Government is turned off in config/woofscommunitybook.json.";
            listing = "";
        } else {
            roles = "You are: " + GovernmentCommands.rolesOf(cfg, player.getName().getString());
            listing = GovernmentCommands.listing(GovernmentData.get(server));
            if (listing.length() > MAX_LISTING) {
                listing = listing.substring(0, MAX_LISTING) + "\n...(list shortened)";
            }
        }
        ServerPlayNetworking.send(player, new GovMenuPayload(roles, listing));
    }
}
