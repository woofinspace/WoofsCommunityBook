package io.github.woofinspace.communitybook;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server -> client: the player's roles and the current government listing. Opens or refreshes the menu. */
public record GovMenuPayload(String roles, String listing) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(
            WoofsCommunityBook.MOD_ID, "government_menu"
    );
    public static final Type<GovMenuPayload> TYPE = new Type<>(ID);

    // The listing is capped at GovernmentMenu.MAX_LISTING characters, so this limit always fits.
    public static final StreamCodec<RegistryFriendlyByteBuf, GovMenuPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.stringUtf8(40000), GovMenuPayload::roles,
                    ByteBufCodecs.stringUtf8(40000), GovMenuPayload::listing,
                    GovMenuPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
