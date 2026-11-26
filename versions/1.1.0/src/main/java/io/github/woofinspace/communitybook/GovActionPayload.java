package io.github.woofinspace.communitybook;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client -> server: a government command typed by the menu's buttons, e.g. "government vote 3 yes".
 * The server runs it as the player, so every role and config check still applies.
 */
public record GovActionPayload(String command) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(
            WoofsCommunityBook.MOD_ID, "government_action"
    );
    public static final Type<GovActionPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, GovActionPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.stringUtf8(4096), GovActionPayload::command,
                    GovActionPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
