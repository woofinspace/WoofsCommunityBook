package io.github.woofinspace.communityclipboard;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.resources.Identifier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

public class ClipboardSavedData extends SavedData {
    public static final int MAX_PAGES = 8;
    public static final int MAX_PAGE_LENGTH = 4096;

    private int revision;
    private List<String> pages;

    public ClipboardSavedData() {
        this(0, new ArrayList<>(List.of("")));
    }

    public ClipboardSavedData(int revision, List<String> pages) {
        this.revision = revision;
        this.pages = normalize(pages);
    }

    public int revision() {
        return revision;
    }

    public List<String> pages() {
        return List.copyOf(pages);
    }

    private static List<String> normalize(List<String> input) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < Math.min(MAX_PAGES, input.size()); i++) {
            String s = input.get(i) == null ? "" : input.get(i);
            if (s.length() > MAX_PAGE_LENGTH) s = s.substring(0, MAX_PAGE_LENGTH);
            out.add(s);
        }
        if (out.isEmpty()) out.add("");
        return out;
    }

    private static final Codec<ClipboardSavedData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.INT.fieldOf("revision").forGetter(ClipboardSavedData::revision),
            Codec.STRING.listOf().fieldOf("pages").forGetter(d -> d.pages)
        ).apply(instance, ClipboardSavedData::new)
    );

    private static final SavedDataType<ClipboardSavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(WoofsCommunityClipboard.MOD_ID, "community_book"),
        ClipboardSavedData::new,
        CODEC,
        null
    );

    public static ClipboardSavedData get(MinecraftServer server) {
        var level = server.getLevel(net.minecraft.server.level.ServerLevel.OVERWORLD);
        if (level == null) return new ClipboardSavedData();
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public static void openFor(MinecraftServer server, ServerPlayer player) {
        ClipboardSavedData data = get(server);
        ServerPlayNetworking.send(player, new OpenClipboardPayload(data.revision, encode(data.pages)));
    }

    public static void handleSave(MinecraftServer server, ServerPlayer player, SaveClipboardPayload payload) {
        ClipboardSavedData data = get(server);

        // Only accept saves from a player who is actually holding the custom clipboard.
        if (!hasClipboard(player)) {
            return;
        }

        if (payload.baseRevision() != data.revision) {
            ServerPlayNetworking.send(player, new OpenClipboardPayload(
                    data.revision,
                    encode(data.pages)
            ));
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "screen.woofscommunitybook.conflict"
            ));
            return;
        }

        List<String> newPages = decode(payload.document());
        data.pages = normalize(newPages);
        data.revision++;
        data.setDirty();

        String document = encode(data.pages);
        for (ServerPlayer target : PlayerLookup.all(server)) {
            ServerPlayNetworking.send(target, new OpenClipboardPayload(data.revision, document));
        }
    }

    public static void ensureClipboardForPlayer(MinecraftServer server, ServerPlayer player) {
        if (!hasClipboard(player)) {
            ItemStack stack = WoofsCommunityClipboard.newClipboardStack();
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }

    private static boolean hasClipboard(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (WoofsCommunityClipboard.isClipboard(player.getInventory().getItem(i))) {
                return true;
            }
        }
        return false;
    }

    private static String encode(List<String> pages) {
        return String.join("\u001E", pages);
    }

    private static List<String> decode(String document) {
        String[] split = document.split("\u001E", -1);
        List<String> pages = new ArrayList<>();
        for (int i = 0; i < Math.min(MAX_PAGES, split.length); i++) {
            pages.add(split[i]);
        }
        if (pages.isEmpty()) pages.add("");
        return pages;
    }
}
