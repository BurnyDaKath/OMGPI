package tk.omgpi.utils;

import org.bukkit.ChatColor;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Team;
import tk.omgpi.OMGPI;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Scoreboard controller.
 * Recommended to set lines in scoreboard on sidebar.
 */
public class ObjectiveBuffer {
    /**
     * ObjectiveBuffer lines. Maximum size is 16 (idiot-proof so you cannot add more than minecraft can handle).
     */
    public List<String> lines;

    public ObjectiveBuffer() {
        lines = new ArrayList<>(16);
    }

    public ObjectiveBuffer(List<String> lines) {
        this.lines = new ArrayList<>(16);
        this.lines.addAll(lines);
    }

    /**
     * Load scoreboard into custom objective.
     * @param o Given objective.
     */
    public void loadInto(Objective o) {
        for (int i = 0; i < 16; i++)
            if (lines.size() > i && lines.get(i) != null) {
                o.getScore(ChatColor.values()[i] + "").setScore(lines.size() - i);
                Team t = o.getScoreboard().getTeam(i + "");
                if (t == null) t = o.getScoreboard().registerNewTeam(i + "");
                t.addEntry(ChatColor.values()[i] + "");
                t.setPrefix(lines.get(i).substring(0, Math.min(14, lines.get(i).length())));
            } else o.getScoreboard().resetScores(ChatColor.values()[i] + "");
    }

    /**
     * Get buffer containing all playing players.
     *
     * @return An ObjectiveBuffer.
     */
    public static ObjectiveBuffer createPlayerBuffer() {
        return new ObjectiveBuffer(OMGPI.g.spectatorTeam.unpresent().omgstream().map(p -> ChatColor.AQUA + p.bukkit.getName()).collect(Collectors.toList()));
    }
}
