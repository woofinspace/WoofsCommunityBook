package io.github.woofinspace.communitybook;

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

public class BookSavedData extends SavedData {
    public static final int MAX_PAGES = 8;
    public static final int MAX_PAGE_LENGTH = 4096;

    private int revision;
    private List<String> pages;

    public BookSavedData() {
        this(0, new ArrayList<>(List.of("")));
    }

    public BookSavedData(int revision, List<String> pages) {
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

    private static final Codec<BookSavedData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.INT.fieldOf("revision").forGetter(BookSavedData::revision),
            Codec.STRING.listOf().fieldOf("pages").forGetter(d -> d.pages)
        ).apply(instance, BookSavedData::new)
    );

    private static final SavedDataType<BookSavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(WoofsCommunityBook.MOD_ID, "community_book"),
        BookSavedData::new,
        CODEC,
        null
    );

    public static BookSavedData get(MinecraftServer server) {
        var level = server.getLevel(net.minecraft.server.level.ServerLevel.OVERWORLD);
        if (level == null) return new BookSavedData();
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public static void openFor(MinecraftServer server, ServerPlayer player) {
        BookSavedData data = get(server);
        ServerPlayNetworking.send(player, new OpenBookPayload(data.revision, encode(data.pages), true));
    }

    public static void handleSave(MinecraftServer server, ServerPlayer player, SaveBookPayload payload) {
        BookSavedData data = get(server);

        // Only accept saves from a player who is actually holding the custom book.
        if (!hasBook(player)) {
            return;
        }

        if (payload.baseRevision() != data.revision) {
            ServerPlayNetworking.send(player, new OpenBookPayload(
                    data.revision,
                    encode(data.pages),
                    true
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
        // Everyone gets the new text, but only players who already have the editor open will show it.
        for (ServerPlayer target : PlayerLookup.all(server)) {
            ServerPlayNetworking.send(target, new OpenBookPayload(data.revision, document, target == player));
        }
    }

    public static void ensureBookForPlayer(MinecraftServer server, ServerPlayer player) {
        if (!hasBook(player)) {
            ItemStack stack = WoofsCommunityBook.newBookStack();
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }

    private static boolean hasBook(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (WoofsCommunityBook.isBook(player.getInventory().getItem(i))) {
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
