package io.github.woofinspace.communitybook;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

public class CommunityBookItem extends Item {
    public CommunityBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BookSavedData.openFor((net.minecraft.server.MinecraftServer) level.getServer(), serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }
}
