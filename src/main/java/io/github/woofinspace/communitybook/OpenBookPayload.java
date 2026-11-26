package io.github.woofinspace.communitybook;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenBookPayload(int revision, String document) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(
            WoofsCommunityBook.MOD_ID, "open_book"
    );
    public static final Type<OpenBookPayload> TYPE = new Type<>(ID);

    public static final net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, OpenBookPayload> CODEC =
            net.minecraft.network.codec.StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, OpenBookPayload::revision,
                    ByteBufCodecs.STRING_UTF8, OpenBookPayload::document,
                    OpenBookPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
