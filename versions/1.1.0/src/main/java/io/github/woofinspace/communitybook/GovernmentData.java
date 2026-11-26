package io.github.woofinspace.communitybook;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * The bill pipeline, saved with the world.
 *
 *   Citizen proposal -> Congress (forward or reject) -> Senate (majority vote) -> President (approve or veto)
 *
 * Congress can also submit a bill straight to the Senate. The President can enact a law directly (decree).
 * Laws are only recorded; the mod does not enforce them.
 */
public class GovernmentData extends SavedData {
    public static final int MAX_TEXT = 1000;

    public enum VoteResult { RECORDED, PASSED, REJECTED, NOT_FOUND }

    /** choice is "yes", "no", or "split". For "split", text is the part of the bill the senator approves. */
    public record Vote(String senator, String choice, String text) {
        static final Codec<Vote> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("senator").forGetter(Vote::senator),
                Codec.STRING.fieldOf("choice").forGetter(Vote::choice),
                Codec.STRING.fieldOf("text").forGetter(Vote::text)
        ).apply(i, Vote::new));
    }

    public record Bill(int id, String text, String author, List<Vote> votes) {
        static final Codec<Bill> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("id").forGetter(Bill::id),
                Codec.STRING.fieldOf("text").forGetter(Bill::text),
                Codec.STRING.fieldOf("author").forGetter(Bill::author),
                Vote.CODEC.listOf().fieldOf("votes").forGetter(Bill::votes)
        ).apply(i, Bill::new));
    }

    public record Law(int number, String text, String author) {
        static final Codec<Law> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("number").forGetter(Law::number),
                Codec.STRING.fieldOf("text").forGetter(Law::text),
                Codec.STRING.fieldOf("author").forGetter(Law::author)
        ).apply(i, Law::new));
    }

    private List<Bill> proposals = new ArrayList<>();
    private List<Bill> senate = new ArrayList<>();
    private List<Bill> presidency = new ArrayList<>();
    private List<Law> laws = new ArrayList<>();
    private int nextId = 1;

    public GovernmentData() {}

    private GovernmentData(List<Bill> proposals, List<Bill> senate, List<Bill> presidency, List<Law> laws, int nextId) {
        this.proposals = new ArrayList<>(proposals);
        this.senate = new ArrayList<>(senate);
        this.presidency = new ArrayList<>(presidency);
        this.laws = new ArrayList<>(laws);
        this.nextId = nextId;
    }

    private static final Codec<GovernmentData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Bill.CODEC.listOf().fieldOf("proposals").forGetter(d -> d.proposals),
            Bill.CODEC.listOf().fieldOf("senate").forGetter(d -> d.senate),
            Bill.CODEC.listOf().fieldOf("presidency").forGetter(d -> d.presidency),
            Law.CODEC.listOf().fieldOf("laws").forGetter(d -> d.laws),
            Codec.INT.fieldOf("nextId").forGetter(d -> d.nextId)
    ).apply(i, GovernmentData::new));

    private static final SavedDataType<GovernmentData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(WoofsCommunityBook.MOD_ID, "government"),
            GovernmentData::new,
            CODEC,
            null
    );

    public static GovernmentData get(MinecraftServer server) {
        ServerLevel level = server.getLevel(ServerLevel.OVERWORLD);
        if (level == null) return new GovernmentData();
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public List<Bill> proposals() { return List.copyOf(proposals); }
    public List<Bill> senate() { return List.copyOf(senate); }
    public List<Bill> presidency() { return List.copyOf(presidency); }
    public List<Law> laws() { return List.copyOf(laws); }

    /** A citizen's proposal, waiting for Congress. */
    public Bill propose(String author, String text) {
        Bill b = new Bill(nextId++, text, author, new ArrayList<>());
        proposals.add(b);
        setDirty();
        return b;
    }

    /** Congress sends a citizen proposal on to the Senate. */
    public Bill forward(int id) {
        Bill b = take(proposals, id);
        if (b == null) return null;
        Bill moved = new Bill(b.id(), b.text(), b.author(), new ArrayList<>());
        senate.add(moved);
        setDirty();
        return moved;
    }

    /** Congress rejects a citizen proposal. */
    public boolean rejectProposal(int id) {
        boolean removed = take(proposals, id) != null;
        if (removed) setDirty();
        return removed;
    }

    /** Congress writes a bill directly for the Senate. */
    public Bill submitToSenate(String author, String text) {
        Bill b = new Bill(nextId++, text, author, new ArrayList<>());
        senate.add(b);
        setDirty();
        return b;
    }

    /**
     * Records one senator's vote (replacing any earlier vote by that senator).
     * Passes when more than half of the Senate has yes or split votes.
     * Rejects when at least half has voted no.
     */
    public VoteResult vote(int id, String senator, String choice, String text, int senateSize) {
        int idx = indexOf(senate, id);
        if (idx < 0) return VoteResult.NOT_FOUND;
        Bill b = senate.get(idx);

        List<Vote> votes = new ArrayList<>();
        for (Vote v : b.votes()) {
            if (!v.senator().equalsIgnoreCase(senator)) votes.add(v);
        }
        votes.add(new Vote(senator, choice, text));

        int yes = 0;
        int no = 0;
        String split = null;
        for (Vote v : votes) {
            if (v.choice().equals("no")) {
                no++;
            } else {
                yes++;
                if (v.choice().equals("split")) split = v.text();
            }
        }

        setDirty();
        if (yes * 2 > senateSize) {
            senate.remove(idx);
            String passedText = split != null ? split : b.text();
            presidency.add(new Bill(b.id(), passedText, b.author(), new ArrayList<>()));
            return VoteResult.PASSED;
        }
        if (no * 2 >= senateSize) {
            senate.remove(idx);
            return VoteResult.REJECTED;
        }
        senate.set(idx, new Bill(b.id(), b.text(), b.author(), votes));
        return VoteResult.RECORDED;
    }

    /** Joins two Senate bills into one new bill. Its votes start over. Returns null if it can't be done. */
    public Bill merge(int a, int b) {
        int ia = indexOf(senate, a);
        int ib = indexOf(senate, b);
        if (ia < 0 || ib < 0 || a == b) return null;
        Bill ba = senate.get(ia);
        Bill bb = senate.get(ib);
        String text = ba.text() + "\n" + bb.text();
        if (text.length() > MAX_TEXT) return null;

        senate.remove(Math.max(ia, ib));
        senate.remove(Math.min(ia, ib));
        Bill merged = new Bill(nextId++, text, ba.author(), new ArrayList<>());
        senate.add(merged);
        setDirty();
        return merged;
    }

    /** The President signs a bill into law. */
    public Law approve(int id) {
        Bill b = take(presidency, id);
        if (b == null) return null;
        Law law = new Law(laws.size() + 1, b.text(), b.author());
        laws.add(law);
        setDirty();
        return law;
    }

    /** The President vetoes a bill. A veto is final. */
    public boolean veto(int id) {
        boolean removed = take(presidency, id) != null;
        if (removed) setDirty();
        return removed;
    }

    /** The President writes a bill for their own sign-or-veto queue (President-only mode). */
    public Bill addToPresidency(String author, String text) {
        Bill b = new Bill(nextId++, text, author, new ArrayList<>());
        presidency.add(b);
        setDirty();
        return b;
    }

    /** The President enacts a law directly, without a bill. */
    public Law decree(String author, String text) {
        Law law = new Law(laws.size() + 1, text, author);
        laws.add(law);
        setDirty();
        return law;
    }

    private static Bill take(List<Bill> list, int id) {
        int idx = indexOf(list, id);
        return idx < 0 ? null : list.remove(idx);
    }

    private static int indexOf(List<Bill> list, int id) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id() == id) return i;
        }
        return -1;
    }
}
