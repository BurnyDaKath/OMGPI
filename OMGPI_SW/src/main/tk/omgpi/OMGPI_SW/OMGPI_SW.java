package tk.omgpi.OMGPI_SW;

import org.bukkit.GameMode;
import org.bukkit.Location;
import tk.omgpi.OMGPI;
import tk.omgpi.files.Mapfig;
import tk.omgpi.game.Game;
import tk.omgpi.game.GameState;
import tk.omgpi.game.OMGPlayer;
import tk.omgpi.game.OMGTeam;
import tk.omgpi.utils.Coordinates;
import tk.omgpi.utils.OMGList;
import tk.omgpi.utils.ObjectiveBuffer;

/**
 * Easiest game to make on OMGPI.
 *
 * @author BurnyDaKath - OMGPI and the example game Skywars.
 */
public class OMGPI_SW extends Game {
    public static int counter = 0;

    public void onEnable() {
        name = "Skywars";
        super.onEnable();
        settings.hasDiscovery = false;
        settings.isLootingOn = true;
        settings.oneLife = true;
    }

    public Location player_spawnLocation(OMGPlayer p) {
        double[] coords = Coordinates.parse(loadedMap.mapfig.getStringList("spawns").get(counter++), Coordinates.CoordinateType.ROTATION);
        return new Location(OMGPI.gameworld.bukkit, coords[0], coords[1], coords[2], (float) (coords.length > 3 ? coords[3] : 0), (float) (coords.length > 3 ? coords[4] : 0));
    }

    public void event_preMapfigSave(Mapfig m) {
        m.setUnpresent("spawns", new OMGList<String>(){{
            for (int i = 0; i < m.getInt("players"); i++) add("0,0,0");
        }});
    }

    public void game_checkForEnd() {
        if (state == GameState.INGAME)
            OMGPlayer.link.values().forEach(p -> ObjectiveBuffer.createPlayerBuffer().loadInto(p.displayObjective));
        super.game_checkForEnd();
    }

    public void event_team_creation(OMGTeam t) {
        if (t.id.equals("default")) t.gameMode = GameMode.SURVIVAL;
    }
}
