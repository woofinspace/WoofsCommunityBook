package io.github.woofinspace.communitybook.client;

import io.github.woofinspace.communitybook.SaveBookPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommunityBookScreen extends Screen {
    private static final int MAX_PAGES = 8;

    private int revision;
    private List<String> pages;
    private final List<EditBox> editors = new ArrayList<>();
    private int pageIndex = 0;
    private boolean dirty = false;

    public CommunityBookScreen(Component title, int revision, String document) {
        super(title);
        this.revision = revision;
        this.pages = decode(document);
    }

    private static List<String> decode(String document) {
        List<String> list = new ArrayList<>(Arrays.asList(document.split("\u001E", -1)));
        while (list.size() < MAX_PAGES) list.add("");
        return new ArrayList<>(list.subList(0, MAX_PAGES));
    }

    private static String encode(List<String> pages) {
        return String.join("\u001E", pages);
    }

    @Override
    protected void init() {
        editors.clear();

        int left = Math.max(10, (this.width - 340) / 2);
        int top = Math.max(25, (this.height - 250) / 2);

        for (int i = 0; i < 12; i++) {
            EditBox box = new EditBox(this.font, left, top + 30 + i * 18, 340, 17, Component.empty());
            box.setMaxLength(256);
            String page = pages.get(pageIndex);
            String[] lines = page.split("\n", -1);
            if (i < lines.length) box.setValue(lines[i]);
            box.setResponder(value -> dirty = true);
            editors.add(this.addRenderableWidget(box));
        }

        this.addRenderableWidget(Button.builder(
                Component.literal("<"), b -> {
                    collectPage();
                    pageIndex = (pageIndex + MAX_PAGES - 1) % MAX_PAGES;
                    clearAndRebuild();
                }).bounds(left, top + 255, 55, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.woofscommunitybook.page", pageIndex + 1),
                b -> {}).bounds(left + 60, top + 255, 120, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(">"), b -> {
                    collectPage();
                    pageIndex = (pageIndex + 1) % MAX_PAGES;
                    clearAndRebuild();
                }).bounds(left + 185, top + 255, 55, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.woofscommunitybook.save"),
                b -> save()).bounds(left + 245, top + 255, 95, 20).build());
    }

    private void clearAndRebuild() {
        this.rebuildWidgets();
    }

    private void collectPage() {
        List<String> lines = new ArrayList<>();
        for (EditBox editor : editors) {
            String value = editor.getValue();
            if (!value.isEmpty() || !lines.isEmpty()) lines.add(value);
        }
        pages.set(pageIndex, String.join("\n", lines));
    }

    private void save() {
        collectPage();
        ClientPlayNetworking.send(new SaveBookPayload(revision, encode(pages)));
        dirty = false;
    }

    public void applyServerState(int newRevision, String document) {
        if (newRevision < revision) return;
        this.revision = newRevision;
        this.pages = decode(document);
        this.dirty = false;
        this.rebuildWidgets();
    }

    @Override
    public void onClose() {
        if (dirty) {
            collectPage();
        }
        super.onClose();
    }

    @Override
    public void removed() {
        super.removed();
        if (WoofsCommunityBookClient.openScreen == this) {
            WoofsCommunityBookClient.openScreen = null;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int left = Math.max(10, (this.width - 340) / 2);
        int top = Math.max(25, (this.height - 250) / 2);

        graphics.fill(left - 12, top - 18, left + 352, top + 290, 0xFFEEE5D0);
        graphics.outline(left - 12, top - 18, 364, 308, 0xFF6A4A2A);

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.text(this.font, this.title, left, top - 8, 0xFF3A2414, true);
        graphics.text(this.font,
                Component.translatable("screen.woofscommunitybook.synced") + " v" + revision,
                left + 200, top - 8, 0xFF3A2414, false);
    }
}
