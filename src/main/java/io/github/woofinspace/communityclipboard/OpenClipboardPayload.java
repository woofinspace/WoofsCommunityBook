package io.github.woofinspace.communityclipboard;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenClipboardPayload(int revision, String document) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(
            WoofsCommunityClipboard.MOD_ID, "open_clipboard"
    );
    public static final Type<OpenClipboardPayload> TYPE = new Type<>(ID);

    public static final net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, OpenClipboardPayload> CODEC =
            net.minecraft.network.codec.StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, OpenClipboardPayload::revision,
                    ByteBufCodecs.STRING_UTF8, OpenClipboardPayload::document,
                    OpenClipboardPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
