package io.github.woofinspace.communitybook;

import java.util.ArrayList;
import java.util.List;

/**
 * The government shapes. Only one may be on at a time, and only when government.enabled is true.
 *
 *   NORMAL            Citizen proposes -> Congress forwards -> Senate votes -> President signs or vetoes.
 *   CITIZEN_CONGRESS  Citizens and Congress are one body. They submit bills straight to the Senate.
 *   CONGRESS_SENATE   Congress and Senate are one body. It submits and votes on bills itself.
 *   PRESIDENT_ONLY    Only the President. Their bills go to their own sign-or-veto queue, and they decree laws.
 */
public enum GovMode {
    NORMAL,
    CITIZEN_CONGRESS,
    CONGRESS_SENATE,
    PRESIDENT_ONLY;

    /**
     * The players allowed to use an action in this mode, or null if the action isn't used in this mode.
     * Actions: propose, submit, forward, reject, vote, merge, approve, veto, decree.
     */
    public List<String> allowed(GovConfig.Data cfg, String action) {
        GovConfig.Groups g = cfg.groups;
        return switch (this) {
            case NORMAL -> switch (action) {
                case "propose" -> g.citizen;
                case "forward", "reject", "submit" -> g.congress;
                case "vote", "merge" -> g.senate;
                case "approve", "veto", "decree" -> g.president;
                default -> null;
            };
            case CITIZEN_CONGRESS -> switch (action) {
                case "propose", "submit" -> union(g.citizen, g.congress);
                case "vote", "merge" -> g.senate;
                case "approve", "veto", "decree" -> g.president;
                default -> null; // forward and reject aren't used here
            };
            case CONGRESS_SENATE -> switch (action) {
                case "propose" -> g.citizen;
                case "forward", "reject", "submit", "vote", "merge" -> union(g.congress, g.senate);
                case "approve", "veto", "decree" -> g.president;
                default -> null;
            };
            case PRESIDENT_ONLY -> switch (action) {
                case "submit", "approve", "veto", "decree" -> g.president;
                default -> null; // propose, forward, reject, vote, and merge aren't used here
            };
        };
    }

    /** Human-readable name used in messages and the menu. */
    public String label() {
        return switch (this) {
            case NORMAL -> "normal";
            case CITIZEN_CONGRESS -> "citizen + congress";
            case CONGRESS_SENATE -> "congress + senate";
            case PRESIDENT_ONLY -> "president only";
        };
    }

    /** Names from both lists, with duplicates removed (case-insensitive). */
    static List<String> union(List<String> a, List<String> b) {
        List<String> out = new ArrayList<>();
        for (String n : a) if (!GovConfig.contains(out, n)) out.add(n);
        for (String n : b) if (!GovConfig.contains(out, n)) out.add(n);
        return out;
    }
}
