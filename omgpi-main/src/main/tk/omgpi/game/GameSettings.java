package tk.omgpi.game;

import org.bukkit.Bukkit;
import tk.omgpi.files.OMGKit;
import tk.omgpi.files.OMGLoot;

/**
 * Game settings class
 */
public class GameSettings {
    /**
     * Associated game class
     */
    public Game game;
    /**
     * Allow players to join when game is running
     */
    public boolean allowIngameJoin;
    /**
     * Allow players to use game shop
     */
    public boolean allowGameShop;
    /**
     * Allow players to select kits
     */
    public boolean allowKits;
    /**
     * Allow players to use hotbar editor
     */
    public boolean allowHotbarEdit;
    /**
     * Have a discovery period
     */
    public boolean hasDiscovery;
    /**
     * Check if chests with loots were accessed
     */
    public boolean isLootingOn;
    /**
     * Give lastdamagers kill rewards if player left
     */
    public boolean countLeaveAsKill;
    /**
     * Amount of players allowed to play
     */
    public int maxPlayers;
    /**
     * Length of discovery period
     */
    public int discoveryLength;
    /**
     * Time left set on game start
     */
    public long gameLength;
    /**
     * Do players lose if they die
     */
    public boolean oneLife;

    public GameSettings(Game g) {
        this.game = g;
        allowIngameJoin = false;
        allowGameShop = true;
        allowKits = OMGKit.kits.size() > 1;
        allowHotbarEdit = true;
        hasDiscovery = true;
        isLootingOn = OMGLoot.loots.size() > 0;
        countLeaveAsKill = true;
        maxPlayers = Bukkit.getServer().getMaxPlayers();
        discoveryLength = g.gamefig.getInt("discoveryLength", 500);
        gameLength = g.gamefig.getLong("gameLength", 1800);
        oneLife = false;
    }
}
