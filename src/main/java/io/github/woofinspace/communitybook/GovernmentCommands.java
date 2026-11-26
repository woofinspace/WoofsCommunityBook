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
 * /government commands. Everything here does nothing unless government.enabled is true in the config.
 *
 *   Anyone:     status, list, laws, propose (citizens)
 *   Congress:   submit, forward, reject
 *   Senate:     vote yes|no|split, merge
 *   President:  approve, veto, decree
 */
public final class GovernmentCommands {
    private GovernmentCommands() {}

    public static void init() {
        CommandRegistrationCallback.EVENT.register(GovernmentCommands::register);
    }

    /** Runs a command after the player, config, and role checks have passed. */
    private interface Action {
        int run(CommandContext<CommandSourceStack> ctx, String name, GovConfig.Data cfg, GovernmentData gov);
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context,
                         Commands.CommandSelection selection) {
        dispatcher.register(Commands.literal("government")
                .then(Commands.literal("status").executes(c -> run(c, null, GovernmentCommands::status)))
                .then(Commands.literal("list").executes(c -> run(c, null, GovernmentCommands::list)))
                .then(Commands.literal("laws").executes(c -> run(c, null, GovernmentCommands::laws)))

                .then(Commands.literal("propose")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(c -> run(c, "citizen", GovernmentCommands::propose))))

                .then(Commands.literal("submit")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(c -> run(c, "congress", GovernmentCommands::submit))))
                .then(Commands.literal("forward")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .executes(c -> run(c, "congress", GovernmentCommands::forward))))
                .then(Commands.literal("reject")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .executes(c -> run(c, "congress", GovernmentCommands::reject))))

                .then(Commands.literal("vote")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .then(Commands.literal("yes")
                                        .executes(c -> run(c, "senate", voteAction("yes", ""))))
                                .then(Commands.literal("no")
                                        .executes(c -> run(c, "senate", voteAction("no", ""))))
                                .then(Commands.literal("split")
                                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                                .executes(c -> run(c, "senate", (ctx, n, cfg, gov) ->
                                                        vote(ctx, n, cfg, gov, "split",
                                                                StringArgumentType.getString(ctx, "text"))))))))
                .then(Commands.literal("merge")
                        .then(Commands.argument("a", IntegerArgumentType.integer(1))
                                .then(Commands.argument("b", IntegerArgumentType.integer(1))
                                        .executes(c -> run(c, "senate", GovernmentCommands::merge)))))

                .then(Commands.literal("approve")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .executes(c -> run(c, "president", GovernmentCommands::approve))))
                .then(Commands.literal("veto")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .executes(c -> run(c, "president", GovernmentCommands::veto))))
                .then(Commands.literal("decree")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(c -> run(c, "president", GovernmentCommands::decree))))
        );
    }

    private static Action voteAction(String choice, String text) {
        return (ctx, n, cfg, gov) -> vote(ctx, n, cfg, gov, choice, text);
    }

    /** Checks the player, that government is on, and the role. Then runs the action. */
    private static int run(CommandContext<CommandSourceStack> c, String role, Action action) {
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
        if (role != null && !GovConfig.contains(group(cfg, role), name)) {
            fail(src, "Only members of the " + role + " group can do that.");
            return 0;
        }

        return action.run(c, name, cfg, GovernmentData.get(src.getServer()));
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

    private static int status(CommandContext<CommandSourceStack> c, String name, GovConfig.Data cfg, GovernmentData gov) {
        List<String> roles = new ArrayList<>();
        for (String role : List.of("citizen", "congress", "senate", "president")) {
            if (GovConfig.contains(group(cfg, role), name)) roles.add(role);
        }
        String you = roles.isEmpty() ? "no role" : String.join(", ", roles);
        say(c.getSource(), "Government is on. You are: " + you + ". Senate has "
                + cfg.groups.senate.size() + " members, Congress has " + cfg.groups.congress.size() + ".");
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> c, String name, GovConfig.Data cfg, GovernmentData gov) {
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
        say(c.getSource(), sb.toString());
        return 1;
    }

    private static int laws(CommandContext<CommandSourceStack> c, String name, GovConfig.Data cfg, GovernmentData gov) {
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

    private static int propose(CommandContext<CommandSourceStack> c, String name, GovConfig.Data cfg, GovernmentData gov) {
        String text = StringArgumentType.getString(c, "text").trim();
        if (!checkText(c.getSource(), text)) return 0;
        GovernmentData.Bill b = gov.propose(name, text);
        say(c.getSource(), "Proposal #" + b.id() + " sent to Congress.");
        return 1;
    }

    private static int submit(CommandContext<CommandSourceStack> c, String name, GovConfig.Data cfg, GovernmentData gov) {
        String text = StringArgumentType.getString(c, "text").trim();
        if (!checkText(c.getSource(), text)) return 0;
        GovernmentData.Bill b = gov.submitToSenate(name, text);
        say(c.getSource(), "Bill #" + b.id() + " sent to the Senate.");
        return 1;
    }

    private static int forward(CommandContext<CommandSourceStack> c, String name, GovConfig.Data cfg, GovernmentData gov) {
        int id = IntegerArgumentType.getInteger(c, "id");
        GovernmentData.Bill b = gov.forward(id);
        if (b == null) {
            fail(c.getSource(), "No citizen proposal #" + id + ".");
            return 0;
        }
        say(c.getSource(), "Proposal #" + id + " forwarded to the Senate.");
        return 1;
    }

    private static int reject(CommandContext<CommandSourceStack> c, String name, GovConfig.Data cfg, GovernmentData gov) {
        int id = IntegerArgumentType.getInteger(c, "id");
        if (!gov.rejectProposal(id)) {
            fail(c.getSource(), "No citizen proposal #" + id + ".");
            return 0;
        }
        say(c.getSource(), "Proposal #" + id + " rejected.");
        return 1;
    }

    private static int vote(CommandContext<CommandSourceStack> c, String name, GovConfig.Data cfg, GovernmentData gov,
                            String choice, String text) {
        int id = IntegerArgumentType.getInteger(c, "id");
        if (choice.equals("split") && !checkText(c.getSource(), text.trim())) return 0;
        int senateSize = cfg.groups.senate.size();
        GovernmentData.VoteResult result = gov.vote(id, name, choice, text.trim(), senateSize);
        switch (result) {
            case NOT_FOUND -> {
                fail(c.getSource(), "No Senate bill #" + id + ".");
                return 0;
            }
            case PASSED -> say(c.getSource(), "Bill #" + id + " passed the Senate and is now with the President.");
            case REJECTED -> say(c.getSource(), "Bill #" + id + " was rejected by the Senate.");
            case RECORDED -> say(c.getSource(), "Your vote on bill #" + id + " is recorded (" + choice + ").");
        }
        return 1;
    }

    private static int merge(CommandContext<CommandSourceStack> c, String name, GovConfig.Data cfg, GovernmentData gov) {
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

    private static int approve(CommandContext<CommandSourceStack> c, String name, GovConfig.Data cfg, GovernmentData gov) {
        int id = IntegerArgumentType.getInteger(c, "id");
        GovernmentData.Law law = gov.approve(id);
        if (law == null) {
            fail(c.getSource(), "No bill #" + id + " waiting for the President.");
            return 0;
        }
        say(c.getSource(), "Bill #" + id + " signed. It is now law #" + law.number() + ".");
        return 1;
    }

    private static int veto(CommandContext<CommandSourceStack> c, String name, GovConfig.Data cfg, GovernmentData gov) {
        int id = IntegerArgumentType.getInteger(c, "id");
        if (!gov.veto(id)) {
            fail(c.getSource(), "No bill #" + id + " waiting for the President.");
            return 0;
        }
        say(c.getSource(), "Bill #" + id + " vetoed. The veto is final.");
        return 1;
    }

    private static int decree(CommandContext<CommandSourceStack> c, String name, GovConfig.Data cfg, GovernmentData gov) {
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
