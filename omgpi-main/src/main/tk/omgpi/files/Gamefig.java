package tk.omgpi.files;

import tk.omgpi.OMGPI;

/**
 * game.omgc representation.
 */
public class Gamefig extends OMGConfig {
    public Gamefig() {
        super(OMGPI.g.getDataFolder(), "game.omgc");
        setUnpresent("minPlayers", 2);
        setUnpresent("waitTime", 60);
        setUnpresent("discoveryLength", 500);
        setUnpresent("gameLength", 1800);
        setUnpresent("gameShopSize", 54);
        setUnpresent("gameShop", (Object) new String[]{"{id:stone,Count:1,Cost:10,tag:{display:{Lore:[\"§aPrice: 10\"]}}}"});
        OMGPI.g.event_preGamefigSave(this);
        save();
    }

    /**
     * Minimum players required to start countdown.
     *
     * @return minPlayers value
     */
    public int getMinPlayers() {
        return Math.max(getInt("minPlayers"), 2);
    }

    /**
     * Countdown time
     *
     * @return waitTime value
     */
    public int getWaitTime() {
        return getInt("waitTime");
    }
}
