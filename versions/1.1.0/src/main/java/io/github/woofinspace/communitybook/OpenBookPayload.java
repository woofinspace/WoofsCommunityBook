package io.github.woofinspace.communitybook;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// open = true: the player asked for the book (right-click or their own save), so open the editor.
// open = false: a silent refresh sent to everyone else; only applied if their editor is already open.
public record OpenBookPayload(int revision, String document, boolean open) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(
            WoofsCommunityBook.MOD_ID, "open_book"
    );
    public static final Type<OpenBookPayload> TYPE = new Type<>(ID);

    public static final net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, OpenBookPayload> CODEC =
            net.minecraft.network.codec.StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, OpenBookPayload::revision,
                    ByteBufCodecs.STRING_UTF8, OpenBookPayload::document,
                    ByteBufCodecs.BOOL, OpenBookPayload::open,
                    OpenBookPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
