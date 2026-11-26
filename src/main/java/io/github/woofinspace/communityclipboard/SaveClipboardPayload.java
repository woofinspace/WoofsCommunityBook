package io.github.woofinspace.communityclipboard;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SaveClipboardPayload(int baseRevision, String document) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(
            WoofsCommunityClipboard.MOD_ID, "save_clipboard"
    );
    public static final Type<SaveClipboardPayload> TYPE = new Type<>(ID);

    public static final net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, SaveClipboardPayload> CODEC =
            net.minecraft.network.codec.StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SaveClipboardPayload::baseRevision,
                    ByteBufCodecs.STRING_UTF8, SaveClipboardPayload::document,
                    SaveClipboardPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
