package io.github.woofinspace.communitybook.client;

import io.github.woofinspace.communitybook.OpenBookPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class WoofsCommunityBookClient implements ClientModInitializer {
    // The editor that is currently open, or null. Set by the screen itself and cleared in CommunityBookScreen.removed().
    static CommunityBookScreen openScreen;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(OpenBookPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (openScreen != null) {
                    openScreen.applyServerState(payload.revision(), payload.document());
                } else {
                    openScreen = new CommunityBookScreen(Component.translatable(
                            "screen.woofscommunitybook.title"
                    ), payload.revision(), payload.document());
                    Minecraft.getInstance().setScreenAndShow(openScreen);
                }
            });
        });
    }
}
