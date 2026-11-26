package io.github.woofinspace.communitybook;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SaveBookPayload(int baseRevision, String document) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(
            WoofsCommunityBook.MOD_ID, "save_book"
    );
    public static final Type<SaveBookPayload> TYPE = new Type<>(ID);

    public static final net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, SaveBookPayload> CODEC =
            net.minecraft.network.codec.StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SaveBookPayload::baseRevision,
                    ByteBufCodecs.STRING_UTF8, SaveBookPayload::document,
                    SaveBookPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
