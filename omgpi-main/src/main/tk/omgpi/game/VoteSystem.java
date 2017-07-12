package tk.omgpi.game;

import org.bukkit.ChatColor;
import tk.omgpi.OMGPI;
import tk.omgpi.files.OMGMap;
import tk.omgpi.utils.OMGHashMap;
import tk.omgpi.utils.OMGList;
import tk.omgpi.utils.ObjectiveBuffer;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Voting system. Usually has 5 maps.
 */
public class VoteSystem {
    /**
     * Player votes
     */
    public OMGHashMap<OMGMap, OMGList<OMGPlayer>> votes;

    /**
     * Voting players
     */
    public boolean voting;

    /**
     * Struct. Calls start.
     */
    public VoteSystem() {
        voting = false;
        votes = new OMGHashMap<>();
        for (String s : OMGMap.getFiveMaps()) votes.put(new OMGMap(s), new OMGList<>());
        start();
        scoreboardVotes();
    }

    /**
     * Allow voting and delete game world folder.
     */
    public void start() {
        voting = true;
        OMGPI.gameworld.unload();
    }

    /**
     * Disallow voting and prepare the map for playing. Used on 10 seconds before start or on /setmap.
     *
     * @param m Map to load or null for most voted.
     */
    public void stop(OMGMap m) {
        if (voting) {
            voting = false;
            OMGPI.g.broadcast(ChatColor.DARK_GREEN + "Voting ended!");
            OMGPI.g.broadcast(ChatColor.AQUA + "Selected map is " + (OMGPI.g.loadedMap = m == null ? getMostVotedMap() : m) + ".");
            OMGPI.g.loadedMap.load();
        }
    }

    /**
     * Get most voted map.
     *
     * @return First most voted map.
     */
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public OMGMap getMostVotedMap() {
        return votes.entrySet().stream().max((o1, o2) -> new Integer(o1.getValue().size()).compareTo(o2.getValue().size())).get().getKey();
    }

    /**
     * Used by default in modules to make player vote.
     *
     * @param p       Voting player.
     * @param map Name of voted map or its number.
     * @return True if vote is successful, else false.
     */
    public boolean vote(OMGPlayer p, String map) {
        for (OMGMap m : new LinkedList<>(votes.keySet())) votes.get(m).remove(p);
        try {
            int i = Integer.parseInt(map) - 1;
            OMGList<OMGMap> ms = new OMGList<>(votes.keySet());
            OMGMap m = ms.get(i);
            OMGList<OMGPlayer> ll = votes.get(m);
            ll.add(p);
            votes.put(m, ll);
            scoreboardVotes();
            return true;
        } catch (Exception e) {
            for (OMGMap m : new OMGList<>(votes.keySet())) {
                if (m.name.equalsIgnoreCase(map)) {
                    OMGList<OMGPlayer> ll = votes.get(m);
                    ll.add(p);
                    votes.put(m, ll);
                    scoreboardVotes();
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Update scoreboard info.
     */
    public void scoreboardVotes() {
        OMGPlayer.link.values().forEach(p -> {
            ObjectiveBuffer buffer = new ObjectiveBuffer();
            if (votes.keySet().size() == 1) {buffer.lines.add("");
                buffer.lines.add(ChatColor.AQUA + "Next map:");
                buffer.lines.add(ChatColor.GRAY + new ArrayList<>(votes.keySet()).get(0).name);
                buffer.loadInto(p.displayObjective);
                return;
            }
            buffer.lines.add(ChatColor.DARK_GREEN + "Vote! /v 1-" + votes.keySet().size());
            buffer.lines.add("");
            for (int i = 0; i < votes.keySet().size(); i++)
                buffer.lines.add(ChatColor.GRAY + "" + (i + 1) + " " + new ArrayList<>(votes.keySet()).get(i).name);
            buffer.loadInto(p.displayObjective);
        });
    }
}
