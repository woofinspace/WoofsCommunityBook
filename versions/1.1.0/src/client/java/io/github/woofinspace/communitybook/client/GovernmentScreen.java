package io.github.woofinspace.communitybook.client;

import io.github.woofinspace.communitybook.GovActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The Government menu. Buttons send ordinary /government commands to the server, so the server
 * does all the checks (config on/off, roles, bill numbers). This screen only shows the result.
 */
public class GovernmentScreen extends Screen {
    private static final int W = 340;
    private static final int PANEL_H = 330;
    private static final int LIST_TOP = 36;
    private static final int LIST_LINES = 12;
    private static final int LINE_H = 10;
    private static final int TEXT_COLOR = 0xFF3A2414;

    private String roles;
    private String actions;
    private String listing;
    private String savedText = "";
    private String savedNumber = "";
    private EditBox textBox;
    private EditBox numberBox;

    public GovernmentScreen(String roles, String actions, String listing) {
        super(Component.literal("Government"));
        this.roles = roles;
        this.actions = actions;
        this.listing = listing;
    }

    /** New state from the server. Keeps whatever the player has typed. */
    public void applyState(String roles, String actions, String listing) {
        if (textBox != null) savedText = textBox.getValue();
        if (numberBox != null) savedNumber = numberBox.getValue();
        this.roles = roles;
        this.actions = actions;
        this.listing = listing;
        this.rebuildWidgets();
    }

    /** True if the server says this player may use the action in the current mode. */
    private boolean can(String action) {
        return ("," + actions + ",").contains("," + action + ",");
    }

    @Override
    protected void init() {
        int left = (this.width - W) / 2;
        int top = Math.max(10, (this.height - PANEL_H) / 2);

        textBox = addRenderableWidget(new EditBox(this.font, left, top + 164, W, 18, Component.literal("Text")));
        textBox.setMaxLength(1000);
        textBox.setHint(Component.literal("Text for proposals, bills, decrees, and split votes"));
        textBox.setValue(savedText);

        numberBox = addRenderableWidget(new EditBox(this.font, left, top + 188, 120, 18, Component.literal("Number")));
        numberBox.setMaxLength(12);
        numberBox.setHint(Component.literal("Bill number (merge: two numbers)"));
        numberBox.setValue(savedNumber);

        // Each button only appears if the server says this player may use it in the current mode.
        int y = top + 214;
        int x = left;
        if (can("propose")) add(x, y, "Propose", () -> sendText("propose"));
        if (can("submit")) add(x += 80, y, "Submit", () -> sendText("submit"));
        x = left;
        y += 22;
        if (can("forward")) add(x, y, "Forward", () -> sendNumber("forward"));
        if (can("reject")) add(x += 80, y, "Reject", () -> sendNumber("reject"));
        x = left;
        y += 22;
        if (can("vote")) add(x, y, "Yes", () -> send("government vote " + number() + " yes"));
        if (can("vote")) add(x += 80, y, "No", () -> send("government vote " + number() + " no"));
        if (can("vote")) add(x += 80, y, "Split", () -> send("government vote " + number() + " split " + text()));
        if (can("merge")) add(x += 80, y, "Merge", () -> sendNumber("merge"));
        x = left;
        y += 22;
        if (can("approve")) add(x, y, "Approve", () -> sendNumber("approve"));
        if (can("veto")) add(x += 80, y, "Veto", () -> sendNumber("veto"));
        if (can("decree")) add(x += 80, y, "Decree", () -> sendText("decree"));
        x = left;
        y += 22;
        add(left, y, "Refresh", () -> send("government menu"));
        add(left + W - 76, y, "Close", this::onClose);
    }

    private void add(int x, int y, String label, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> action.run())
                .bounds(x, y, 76, 20).build());
    }

    private String text() {
        return textBox == null ? "" : textBox.getValue().trim();
    }

    private String number() {
        return numberBox == null ? "" : numberBox.getValue().trim();
    }

    private void sendText(String sub) {
        send("government " + sub + " " + text());
    }

    private void sendNumber(String sub) {
        send("government " + sub + " " + number());
    }

    private void send(String command) {
        ClientPlayNetworking.send(new GovActionPayload(command));
    }

    @Override
    public void removed() {
        super.removed();
        if (WoofsCommunityBookClient.govScreen == this) {
            WoofsCommunityBookClient.govScreen = null;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int left = (this.width - W) / 2;
        int top = Math.max(10, (this.height - PANEL_H) / 2);

        graphics.fill(left - 12, top - 18, left + W + 12, top + PANEL_H, 0xFFEEE5D0);
        graphics.outline(left - 12, top - 18, W + 24, PANEL_H + 18, 0xFF6A4A2A);

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.text(this.font, this.title, left, top - 8, TEXT_COLOR, true);
        graphics.text(this.font, Component.literal(roles), left, top + 14, TEXT_COLOR, false);

        String[] lines = listing.isEmpty() ? new String[0] : listing.split("\n", -1);
        int shown = Math.min(lines.length, LIST_LINES);
        for (int i = 0; i < shown; i++) {
            String line = this.font.plainSubstrByWidth(lines[i], W);
            graphics.text(this.font, Component.literal(line), left, top + LIST_TOP + i * LINE_H, TEXT_COLOR, false);
        }
        if (lines.length > LIST_LINES) {
            graphics.text(this.font,
                    Component.literal("...and " + (lines.length - LIST_LINES) + " more lines. Use /government list in chat."),
                    left, top + LIST_TOP + LIST_LINES * LINE_H, TEXT_COLOR, false);
        }
    }
}
