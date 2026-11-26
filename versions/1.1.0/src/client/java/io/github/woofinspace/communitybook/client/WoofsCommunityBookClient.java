package io.github.woofinspace.communitybook.client;

import io.github.woofinspace.communitybook.GovMenuPayload;
import io.github.woofinspace.communitybook.OpenBookPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class WoofsCommunityBookClient implements ClientModInitializer {
    // The editor that is currently open, or null. Set by the screen itself and cleared in CommunityBookScreen.removed().
    static CommunityBookScreen openScreen;
    // The Government menu that is currently open, or null. Cleared in GovernmentScreen.removed().
    static GovernmentScreen govScreen;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(GovMenuPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (govScreen != null) {
                    govScreen.applyState(payload.roles(), payload.actions(), payload.listing());
                } else {
                    govScreen = new GovernmentScreen(payload.roles(), payload.actions(), payload.listing());
                    Minecraft.getInstance().setScreenAndShow(govScreen);
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(OpenBookPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (openScreen != null) {
                    openScreen.applyServerState(payload.revision(), payload.document());
                } else if (payload.open()) {
                    openScreen = new CommunityBookScreen(Component.translatable(
                            "screen.woofscommunitybook.title"
                    ), payload.revision(), payload.document());
                    Minecraft.getInstance().setScreenAndShow(openScreen);
                }
            });
        });
    }
}
