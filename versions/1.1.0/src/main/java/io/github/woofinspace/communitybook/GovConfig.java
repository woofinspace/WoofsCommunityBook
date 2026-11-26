package io.github.woofinspace.communitybook;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * config/woofscommunitybook.json
 *
 * government.enabled  - false by default. Nothing government-related works until this is true.
 * joinedPlayers       - filled in automatically as players join (copy names from here into groups).
 * groups              - copy player names into these lists: citizen, congress, senate, president.
 */
public final class GovConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final class Government {
        public boolean enabled = false;
        public Modes modes = new Modes();
    }

    /** Only checked when government.enabled is true. Turn on exactly one. Normal is on by default. */
    public static final class Modes {
        public boolean normal = true;
        public boolean citizenCongress = false;
        public boolean congressSenate = false;
        public boolean presidentOnly = false;
    }

    public static final class Groups {
        public List<String> citizen = new ArrayList<>();
        public List<String> congress = new ArrayList<>();
        public List<String> senate = new ArrayList<>();
        public List<String> president = new ArrayList<>();
    }

    public static final class Data {
        public Government government = new Government();
        public Groups groups = new Groups();
        public List<String> joinedPlayers = new ArrayList<>();
    }

    private GovConfig() {}

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("woofscommunitybook.json");
    }

    /** Reads the config. If the file is missing or unreadable, defaults are returned (government stays off). */
    public static synchronized Data load() {
        Data d = read();
        return d != null ? d : new Data();
    }

    /**
     * Returns the config, or null if the file exists but can't be parsed.
     * A missing file is created with defaults.
     */
    private static Data read() {
        Path p = path();
        try {
            if (!Files.exists(p)) {
                Data defaults = new Data();
                write(defaults);
                return defaults;
            }
            Data d = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), Data.class);
            return d == null ? new Data() : normalize(d);
        } catch (IOException | JsonParseException e) {
            WoofsCommunityBook.LOGGER.error("Could not read {}. Government stays off until it is fixed.", p, e);
            return null;
        }
    }

    private static void write(Data d) {
        try {
            Files.writeString(path(), GSON.toJson(d), StandardCharsets.UTF_8);
        } catch (IOException e) {
            WoofsCommunityBook.LOGGER.error("Could not write {}", path(), e);
        }
    }

    /** The modes that are switched on, by their config names. */
    public static List<String> enabledModes(Data d) {
        List<String> on = new ArrayList<>();
        if (d.government.modes.normal) on.add("normal");
        if (d.government.modes.citizenCongress) on.add("citizenCongress");
        if (d.government.modes.congressSenate) on.add("congressSenate");
        if (d.government.modes.presidentOnly) on.add("presidentOnly");
        return on;
    }

    /**
     * The mode to use right now. Returns null if more than one mode is on, which is an error.
     * If none is on, Normal is used. Modes are ignored when government is disabled.
     */
    public static GovMode mode(Data d) {
        List<String> on = enabledModes(d);
        if (on.size() > 1) return null;
        if (on.isEmpty()) return GovMode.NORMAL;
        return switch (on.get(0)) {
            case "citizenCongress" -> GovMode.CITIZEN_CONGRESS;
            case "congressSenate" -> GovMode.CONGRESS_SENATE;
            case "presidentOnly" -> GovMode.PRESIDENT_ONLY;
            default -> GovMode.NORMAL;
        };
    }

    /** Message for a startup crash when more than one mode is on. Null when the config is fine. */
    public static String modeProblem(Data d) {
        if (!d.government.enabled) return null;
        List<String> on = enabledModes(d);
        if (on.size() <= 1) return null;
        return "More than one government mode is turned on in config/woofscommunitybook.json: "
                + String.join(", ", on) + ". Under government.modes, leave only one set to true, "
                + "then start the server again.";
    }

    /** Fills in any lists or sections missing from a hand-edited file. */
    private static Data normalize(Data d) {
        if (d.government == null) d.government = new Government();
        if (d.government.modes == null) d.government.modes = new Modes();
        if (d.groups == null) d.groups = new Groups();
        if (d.groups.citizen == null) d.groups.citizen = new ArrayList<>();
        if (d.groups.congress == null) d.groups.congress = new ArrayList<>();
        if (d.groups.senate == null) d.groups.senate = new ArrayList<>();
        if (d.groups.president == null) d.groups.president = new ArrayList<>();
        if (d.joinedPlayers == null) d.joinedPlayers = new ArrayList<>();
        return d;
    }

    /** Adds a player to joinedPlayers the first time they join. Leaves a broken file alone. */
    public static synchronized void recordJoin(String name) {
        Data d = read();
        if (d == null) return;
        if (contains(d.joinedPlayers, name)) return;
        d.joinedPlayers.add(name);
        write(d);
    }

    /** Case-insensitive name check, since Minecraft names are not case-sensitive. */
    public static boolean contains(List<String> names, String name) {
        for (String n : names) {
            if (n != null && n.equalsIgnoreCase(name)) return true;
        }
        return false;
    }
}
