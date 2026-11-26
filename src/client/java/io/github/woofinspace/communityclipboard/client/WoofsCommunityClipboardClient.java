package io.github.woofinspace.communityclipboard.client;

import io.github.woofinspace.communityclipboard.OpenClipboardPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class WoofsCommunityClipboardClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(OpenClipboardPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().screen instanceof CommunityClipboardScreen screen) {
                    screen.applyServerState(payload.revision(), payload.document());
                } else {
                    Minecraft.getInstance().setScreen(
                            new CommunityClipboardScreen(Component.translatable(
                                    "screen.woofscommunitybook.title"
                            ), payload.revision(), payload.document())
                    );
                }
            });
        });
    }
}
