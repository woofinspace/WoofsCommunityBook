package io.github.woofinspace.communitybook;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * /government commands. Nothing works unless government.enabled is true in the config.
 * Which actions a player may use depends on the government mode (see GovMode).
 *
 *   Anyone:  status, list, laws, menu
 *   Actions: propose, submit, forward, reject, vote, merge, approve, veto, decree
 */
public final class GovernmentCommands {
    private GovernmentCommands() {}

    public static void init() {
        CommandRegistrationCallback.EVENT.register(GovernmentCommands::register);
    }

    /** Runs a command after the player, config, mode, and action checks have passed. */
    private interface Action {
        int run(CommandContext<CommandSourceStack> ctx, String name, GovConfig.Data cfg,
                GovernmentData gov, GovMode mode);
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context,
                         Commands.CommandSelection selection) {
        dispatcher.register(Commands.literal("government")
                .then(Commands.literal("status").executes(c -> run(c, null, GovernmentCommands::status)))
                .then(Commands.literal("list").executes(c -> run(c, null, GovernmentCommands::list)))
                .then(Commands.literal("laws").executes(c -> run(c, null, GovernmentCommands::laws)))
                .then(Commands.literal("menu").executes(c -> run(c, null, GovernmentCommands::menu)))

                .then(Commands.literal("propose")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(c -> run(c, "propose", GovernmentCommands::propose))))

                .then(Commands.literal("submit")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(c -> run(c, "submit", GovernmentCommands::submit))))
                .then(Commands.literal("forward")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .executes(c -> run(c, "forward", GovernmentCommands::forward))))
                .then(Commands.literal("reject")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .executes(c -> run(c, "reject", GovernmentCommands::reject))))

                .then(Commands.literal("vote")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .then(Commands.literal("yes")
                                        .executes(c -> run(c, "vote", voteAction("yes", ""))))
                                .then(Commands.literal("no")
                                        .executes(c -> run(c, "vote", voteAction("no", ""))))
                                .then(Commands.literal("split")
                                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                                .executes(c -> run(c, "vote", (ctx, n, cfg, gov, mode) ->
                                                        vote(ctx, n, cfg, gov, mode, "split",
                                                                StringArgumentType.getString(ctx, "text"))))))))
                .then(Commands.literal("merge")
                        .then(Commands.argument("a", IntegerArgumentType.integer(1))
                                .then(Commands.argument("b", IntegerArgumentType.integer(1))
                                        .executes(c -> run(c, "merge", GovernmentCommands::merge)))))

                .then(Commands.literal("approve")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .executes(c -> run(c, "approve", GovernmentCommands::approve))))
                .then(Commands.literal("veto")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .executes(c -> run(c, "veto", GovernmentCommands::veto))))
                .then(Commands.literal("decree")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(c -> run(c, "decree", GovernmentCommands::decree))))
        );
    }

    private static Action voteAction(String choice, String text) {
        return (ctx, n, cfg, gov, mode) -> vote(ctx, n, cfg, gov, mode, choice, text);
    }

    /**
     * Checks the player, that government is on, the mode, and the action. Then runs it.
     * action is null for commands anyone can use.
     */
    private static int run(CommandContext<CommandSourceStack> c, String action, Action body) {
        CommandSourceStack src = c.getSource();
        ServerPlayer player = src.getPlayer();
        if (player == null) {
            fail(src, "Only players can use /government.");
            return 0;
        }
        String name = player.getName().getString();

        GovConfig.Data cfg = GovConfig.load();
        if (!cfg.government.enabled) {
            fail(src, "Government is turned off. An admin can turn it on with government.enabled in config/woofscommunitybook.json.");
            return 0;
        }
        GovMode mode = GovConfig.mode(cfg);
        if (mode == null) {
            fail(src, GovConfig.modeProblem(cfg));
            return 0;
        }
        if (action != null) {
            List<String> who = mode.allowed(cfg, action);
            if (who == null) {
                fail(src, "\"" + action + "\" isn't used in " + mode.label() + " mode.");
                return 0;
            }
            if (!GovConfig.contains(who, name)) {
                fail(src, "You can't " + action + " in " + mode.label() + " mode. Your roles: " + rolesOf(cfg, name) + ".");
                return 0;
            }
        }

        return body.run(c, name, cfg, GovernmentData.get(src.getServer()), mode);
    }

    static List<String> group(GovConfig.Data cfg, String role) {
        return switch (role) {
            case "citizen" -> cfg.groups.citizen;
            case "congress" -> cfg.groups.congress;
            case "senate" -> cfg.groups.senate;
            case "president" -> cfg.groups.president;
            default -> List.of();
        };
    }

    /** "citizen, senate" or "no role". */
    static String rolesOf(GovConfig.Data cfg, String name) {
        List<String> roles = new ArrayList<>();
        for (String role : List.of("citizen", "congress", "senate", "president")) {
            if (GovConfig.contains(group(cfg, role), name)) roles.add(role);
        }
        return roles.isEmpty() ? "no role" : String.join(", ", roles);
    }

    /** The actions this player may use in this mode, comma-separated. Used by the menu. */
    static String actionsOf(GovConfig.Data cfg, GovMode mode, String name) {
        List<String> out = new ArrayList<>();
        for (String action : List.of("propose", "submit", "forward", "reject", "vote", "merge",
                "approve", "veto", "decree")) {
            List<String> who = mode.allowed(cfg, action);
            if (who != null && GovConfig.contains(who, name)) out.add(action);
        }
        return String.join(",", out);
    }

    /** The queues as plain text, for /government list and the menu. */
    static String listing(GovernmentData gov) {
        StringBuilder sb = new StringBuilder();
        sb.append("Citizen proposals (waiting for Congress):");
        if (gov.proposals().isEmpty()) sb.append("\n  none");
        for (GovernmentData.Bill b : gov.proposals()) {
            sb.append("\n  #").append(b.id()).append(" by ").append(b.author()).append(": ").append(b.text());
        }
        sb.append("\nSenate bills:");
        if (gov.senate().isEmpty()) sb.append("\n  none");
        for (GovernmentData.Bill b : gov.senate()) {
            sb.append("\n  #").append(b.id()).append(" by ").append(b.author()).append(": ").append(b.text())
                    .append(" (").append(b.votes().size()).append(" votes)");
        }
        sb.append("\nWaiting for the President:");
        if (gov.presidency().isEmpty()) sb.append("\n  none");
        for (GovernmentData.Bill b : gov.presidency()) {
            sb.append("\n  #").append(b.id()).append(" by ").append(b.author()).append(": ").append(b.text());
        }
        return sb.toString();
    }

    private static int status(CommandContext<CommandSourceStack> c, String name, GovConfig.Data cfg,
                              GovernmentData gov, GovMode mode) {
        say(c.getSource(), "Government is on, in " + mode.label() + " mode. You are: " + rolesOf(cfg, name)
                + ". Senate has " + cfg.groups.senate.size() + " members, Congress has "
                + cfg.groups.congress.size() + ".");
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> c, String name, GovConfig.Data cfg,
                            GovernmentData gov, GovMode mode) {
        say(c.getSource(), listing(gov));
        return 1;
    }

    private static int laws(CommandContext<CommandSourceStack> c, String name, GovConfig.Data cfg,
                            GovernmentData gov, GovMode mode) {
        if (gov.laws().isEmpty()) {
            say(c.getSource(), "No laws have been passed yet.");
            return 1;
        }
        StringBuilder sb = new StringBuilder("Laws:");
        for (GovernmentData.Law law : gov.laws()) {
            sb.append("\n  #").append(law.number()).append(": ").append(law.text())
                    .append(" (by ").append(law.author()).append(")");
        }
        say(c.getSource(), sb.toString());
        return 1;
    }

    /** Opens the menu on the player's screen. */
    private static int menu(CommandContext<CommandSourceStack> c, String name, GovConfig.Data cfg,
                            GovernmentData gov, GovMode mode) {
        ServerPlayer player = c.getSource().getPlayer();
        if (player == null) return 0;
        GovernmentMenu.send(player, c.getSource().getServer());
        return 1;
    }

    private static int propose(CommandContext<CommandSourceStack> c, String name, GovConfig.Data cfg,
                               GovernmentData gov, GovMode mode) {
        String text = StringArgumentType.getString(c, "text").trim();
        if (!checkText(c.getSource(), text)) return 0;
        if (mode == GovMode.CITIZEN_CONGRESS) {
            // Citizens and Congress are one body here, so a proposal goes straight to the Senate.
            GovernmentData.Bill b = gov.submitToSenate(name, text);
            say(c.getSource(), "Bill #" + b.id() + " sent to the Senate.");
        } else {
            GovernmentData.Bill b = gov.propose(name, text);
            say(c.getSource(), "Proposal #" + b.id() + " sent to Congress.");
        }
        return 1;
    }

    private static int submit(CommandContext<CommandSourceStack> c, String name, GovConfig.Data cfg,
                              GovernmentData gov, GovMode mode) {
        String text = StringArgumentType.getString(c, "text").trim();
        if (!checkText(c.getSource(), text)) return 0;
        if (mode == GovMode.PRESIDENT_ONLY) {
            GovernmentData.Bill b = gov.addToPresidency(name, text);
            say(c.getSource(), "Bill #" + b.id() + " is waiting for your signature or veto.");
        } else {
            GovernmentData.Bill b = gov.submitToSenate(name, text);
            say(c.getSource(), "Bill #" + b.id() + " sent to the Senate.");
        }
        return 1;
    }

    private static int forward(CommandContext<CommandSourceStack> c, String name, GovConfig.Data cfg,
                               GovernmentData gov, GovMode mode) {
        int id = IntegerArgumentType.getInteger(c, "id");
        GovernmentData.Bill b = gov.forward(id);
        if (b == null) {
            fail(c.getSource(), "No citizen proposal #" + id + ".");
            return 0;
        }
        say(c.getSource(), "Proposal #" + id + " forwarded to the Senate.");
        return 1;
    }

    private static int reject(CommandContext<CommandSourceStack> c, String name, GovConfig.Data cfg,
                              GovernmentData gov, GovMode mode) {
        int id = IntegerArgumentType.getInteger(c, "id");
        if (!gov.rejectProposal(id)) {
            fail(c.getSource(), "No citizen proposal #" + id + ".");
            return 0;
        }
        say(c.getSource(), "Proposal #" + id + " rejected.");
        return 1;
    }

    private static int vote(CommandContext<CommandSourceStack> c, String name, GovConfig.Data cfg,
                            GovernmentData gov, GovMode mode, String choice, String text) {
        int id = IntegerArgumentType.getInteger(c, "id");
        if (choice.equals("split") && !checkText(c.getSource(), text.trim())) return 0;
        int electorate = mode.allowed(cfg, "vote").size();
        GovernmentData.VoteResult result = gov.vote(id, name, choice, text.trim(), electorate);
        switch (result) {
            case NOT_FOUND -> {
                fail(c.getSource(), "No Senate bill #" + id + ".");
                return 0;
            }
            case PASSED -> say(c.getSource(), "Bill #" + id + " passed and is now with the President.");
            case REJECTED -> say(c.getSource(), "Bill #" + id + " was rejected.");
            case RECORDED -> say(c.getSource(), "Your vote on bill #" + id + " is recorded (" + choice + ").");
        }
        return 1;
    }

    private static int merge(CommandContext<CommandSourceStack> c, String name, GovConfig.Data cfg,
                             GovernmentData gov, GovMode mode) {
        int a = IntegerArgumentType.getInteger(c, "a");
        int b = IntegerArgumentType.getInteger(c, "b");
        GovernmentData.Bill merged = gov.merge(a, b);
        if (merged == null) {
            fail(c.getSource(), "Could not merge #" + a + " and #" + b + ". Both must be Senate bills, different numbers, "
                    + "and the combined text must be " + GovernmentData.MAX_TEXT + " characters or fewer.");
            return 0;
        }
        say(c.getSource(), "Bills #" + a + " and #" + b + " merged into #" + merged.id() + ". Votes start over.");
        return 1;
    }

    private static int approve(CommandContext<CommandSourceStack> c, String name, GovConfig.Data cfg,
                               GovernmentData gov, GovMode mode) {
        int id = IntegerArgumentType.getInteger(c, "id");
        GovernmentData.Law law = gov.approve(id);
        if (law == null) {
            fail(c.getSource(), "No bill #" + id + " waiting for the President.");
            return 0;
        }
        say(c.getSource(), "Bill #" + id + " signed. It is now law #" + law.number() + ".");
        return 1;
    }

    private static int veto(CommandContext<CommandSourceStack> c, String name, GovConfig.Data cfg,
                            GovernmentData gov, GovMode mode) {
        int id = IntegerArgumentType.getInteger(c, "id");
        if (!gov.veto(id)) {
            fail(c.getSource(), "No bill #" + id + " waiting for the President.");
            return 0;
        }
        say(c.getSource(), "Bill #" + id + " vetoed. The veto is final.");
        return 1;
    }

    private static int decree(CommandContext<CommandSourceStack> c, String name, GovConfig.Data cfg,
                              GovernmentData gov, GovMode mode) {
        String text = StringArgumentType.getString(c, "text").trim();
        if (!checkText(c.getSource(), text)) return 0;
        GovernmentData.Law law = gov.decree(name, text);
        say(c.getSource(), "Decree enacted as law #" + law.number() + ".");
        return 1;
    }

    private static boolean checkText(CommandSourceStack src, String text) {
        if (text.isEmpty()) {
            fail(src, "Write something first.");
            return false;
        }
        if (text.length() > GovernmentData.MAX_TEXT) {
            fail(src, "That is too long. The limit is " + GovernmentData.MAX_TEXT + " characters.");
            return false;
        }
        return true;
    }

    private static void say(CommandSourceStack src, String message) {
        src.sendSuccess(() -> Component.literal(message), false);
    }

    private static void fail(CommandSourceStack src, String message) {
        src.sendFailure(Component.literal(message));
    }
}
