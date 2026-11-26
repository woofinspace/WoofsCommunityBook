package io.github.woofinspace.communitybook;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WoofsCommunityBook implements ModInitializer {
    public static final String MOD_ID = "woofscommunitybook";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final ResourceKey<Item> BOOK_KEY = ResourceKey.create(
            BuiltInRegistries.ITEM.key(),
            Identifier.fromNamespaceAndPath(MOD_ID, "community_book")
    );

    public static final Item community_book = new CommunityBookItem(
            new Item.Properties().setId(BOOK_KEY).stacksTo(1)
    );

    @Override
    public void onInitialize() {
        net.minecraft.core.Registry.register(BuiltInRegistries.ITEM, BOOK_KEY, community_book);

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            entries.accept(community_book);
        });

        PayloadTypeRegistry.serverboundPlay().register(SaveBookPayload.TYPE, SaveBookPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OpenBookPayload.TYPE, OpenBookPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SaveBookPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            var server = context.server();
            server.execute(() -> BookSavedData.handleSave(server, player, payload));
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            server.execute(() -> BookSavedData.ensureBookForPlayer(server, player));
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            BookSavedData.get(server);
        });

        LOGGER.info("Woof's Community Book loaded.");
    }

    public static ItemStack newBookStack() {
        ItemStack stack = new ItemStack(community_book);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                Component.literal("Woof's Community Book"));
        return stack;
    }

    public static boolean isBook(ItemStack stack) {
        return stack.is(community_book);
    }
}
