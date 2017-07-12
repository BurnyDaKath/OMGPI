package tk.omgpi.game;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Team;
import tk.omgpi.OMGPI;
import tk.omgpi.events.BukkitEventHandler;
import tk.omgpi.events.player.OMGDamageCause;
import tk.omgpi.events.player.OMGDamageEvent;
import tk.omgpi.events.player.OMGDeathEvent;
import tk.omgpi.events.player.OMGJumpEvent;
import tk.omgpi.files.*;
import tk.omgpi.utils.MovingTwinkle;
import tk.omgpi.utils.NBTParser;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The game class that needs to be overridden.
 *
 * @author BurnyDaKath The whole plugin.
 * @author Clijmart The teams sorting algorithm.
 */
public class Game extends JavaPlugin implements Listener {
    /**
     * Name used for default VoteSystem and recommended for SQL access.
     */
    public String name = "Unknown game";

    /**
     * Game configuration containing data for all maps and prelobby.
     */
    public Gamefig gamefig;

    /**
     * Current game state.
     */
    public GameState state;

    /**
     * Game settings used for default OMGPI features.
     */
    public GameSettings settings;

    /**
     * A folder containing maps.
     */
    public File mapsDirectory;

    /**
     * Currently loaded map ready to play.
     */
    public OMGMap loadedMap;

    /**
     * Updator which calls every update actions every game tick.
     */
    public BukkitRunnable updator;

    /**
     * Team used by default or used for selecting random team.
     */
    public OMGTeam defaultTeam;

    /**
     * Team containing spectators.
     */
    public OMGTeam spectatorTeam;

    /**
     * Sources for shooting
     */
    public HashMap<Projectile, Location> shootSources;

    /**
     * Bar on top of screen. By default contains time left.
     */
    public BossBar infoBar;

    /**
     * Voting for maps system and prelobby scoreboard.
     */
    public VoteSystem voteSystem;

    /**
     * Timer before game starts. Never use equals after reruns.
     */
    public Countdown countdown;

    /**
     * Time left for game.
     */
    public long timerTicks = 0;

    /**
     * Game timer.
     */
    public BukkitRunnable timer;

    /**
     * Game preparer: sets up players.
     */
    public GamePreparer gamePreparer;

    /**
     * Delay used for discovery.
     */
    public BukkitRunnable discoveryStartDelay;

    /**
     * Basic game enabling and loading method.<br>
     * <br>
     * For proper runs use:<br>
     * name = "GameName";<br>
     * super.onEnable();<br>
     * <br>
     * If you create teams add:<br>
     * team = new OMGTeam(...);<br>
     * Inventories.update();
     */
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new BukkitEventHandler(), OMGPI.instance);

        gamefig = new Gamefig();
        state = GameState.PRELOBBY;
        gamePreparer = new ClijmartTeamAsserter();
        shootSources = new HashMap<>();

        mapsDirectory = new File(getDataFolder() + File.separator + "maps");

        defaultTeam = new OMGTeam("default", ChatColor.YELLOW + "Default", ChatColor.YELLOW + "", true, GameMode.ADVENTURE);
        spectatorTeam = new OMGTeam("spectator", ChatColor.GRAY + "Spectator", ChatColor.GRAY + "", false, GameMode.SPECTATOR);

        OMGKit.dir = new File(getDataFolder() + File.separator + "kits");
        OMGKit def = new OMGKit("Default");
        def.setUnpresent("displayItem", "{id:stone,Count:1,tag:{display:{Name:\"Default\"}}}");
        def.save();
        String[] kitarr = OMGKit.dir.list();
        List<String> kits = kitarr == null ? new ArrayList<>() : Arrays.asList(kitarr);
        kits.stream().filter(k -> k.endsWith(".kit") && OMGKit.kits.stream().noneMatch(k1 -> (k1 + ".kit").equalsIgnoreCase(k))).forEach(k -> new OMGKit(k.replaceAll("\\.kit", "")));

        OMGLoot.dir = new File(getDataFolder() + File.separator + "loots");
        String[] lootarr = OMGLoot.dir.list();
        List<String> loots = lootarr == null ? new ArrayList<>() : Arrays.asList(lootarr);
        loots.stream().filter(l -> l.endsWith(".loot") && OMGLoot.loots.stream().noneMatch(l1 -> (l1 + ".loot").equalsIgnoreCase(l))).forEach(l -> new OMGLoot(l.replaceAll("\\.loot", "")));

        settings = new GameSettings(this);

        infoBar = Bukkit.createBossBar(ChatColor.AQUA + "", BarColor.WHITE, BarStyle.SOLID);
        infoBar.setVisible(false);

        voteSystem = new VoteSystem();

        countdown = new Countdown();
        updator = new BukkitRunnable() {
            public void run() {
                update();
            }
        };
        updator.runTaskTimer(this, 0, 1);

        Inventories.update();

        Bukkit.getOnlinePlayers().forEach(p -> {
            while (OMGPlayer.get(p) == null) event_player_join(new OMGPlayer(p));
        });
        OMGPI.iLog(name + " initialized, can be played.");
    }

    public void onDisable() {
        infoBar.removeAll();
        updator.cancel();
        if (timer != null) timer.cancel();
    }

    /**
     * Updates every server tick. Use super.update() to not to break anything.
     */
    public void update() {
        OMGPlayer.link.values().forEach(OMGPlayer::update);
        MovingTwinkle.twinkles.forEach(MovingTwinkle::draw);
        if (state == GameState.DISCOVERY)
            OMGPlayer.link.values().forEach(p -> {
                p.actionbar = "Discover the map";
                p.displayObjective.setDisplaySlot(null);
            });
        else
            OMGPlayer.getFiltered(p -> p.displayObjective.getDisplaySlot() != DisplaySlot.SIDEBAR).forEach(p -> p.displayObjective.setDisplaySlot(DisplaySlot.SIDEBAR));
        if (state == GameState.INGAME) {
            spectatorTeam.unpresent().forEach(p -> p.actionbar = settings.allowGameShop ? "You have " + p.gameCoins + " game coins. /shop" : "You are now playing " + name);
            spectatorTeam.list().forEach(p -> p.actionbar = "You are now spectating " + name);
        }
        event_afterUpdateTick();
    }

    /**
     * Called after update().
     */
    public void event_afterUpdateTick() {
    }

    /**
     * Called before mapfig is saved.
     * Add your mapfig values here with setUnpresent().
     *
     * @param mapfig Mapfig that will be saved
     */
    public void event_preMapfigSave(Mapfig mapfig) {
    }

    /**
     * Called before gamefig is saved.
     * Add your gamefig values here with setUnpresent().
     *
     * @param gamefig Gamefig that will be saved
     */
    public void event_preGamefigSave(Gamefig gamefig) {
    }

    /**
     * Check if there is enough players and OMGPI can start counter.
     * This needs to be modified if you need to separate last 10 seconds as a custom GameState.
     * You need 10 seconds for the map to load properly even on the slowest computers.
     */
    public void game_checkForStart() {
        if (state == GameState.PRELOBBY) {
            if (OMGPlayer.getFiltered(p -> p.requestedTeam != spectatorTeam).size() >= settings.maxPlayers) {
                if (state == GameState.PRELOBBY && countdown.isRunning && countdown.time > 10) {
                    broadcast(ChatColor.GREEN + "Enough players to start game!");
                    countdown.rerun(10);
                }
            } else if (countdown.isRunning) {
                if (OMGPlayer.getFiltered(p -> p.requestedTeam != spectatorTeam).size() < gamefig.getMinPlayers()) {
                    broadcast(ChatColor.AQUA + "Not enough players, count stopped.");
                    countdown.cancel();
                    voteSystem.start();
                }
            } else if (OMGPlayer.getFiltered(p -> p.requestedTeam != spectatorTeam).size() >= gamefig.getMinPlayers())
                countdown.rerun(Math.max(gamefig.getWaitTime(), 10));
        }
    }

    /**
     * Start game: discovery or get ready to start.
     */
    public void game_start() {
        if (state == GameState.PRELOBBY) {
            event_game_preStart();
            if (settings.hasDiscovery) {
                OMGPlayer.link.values().forEach(p -> gamePreparer.player_start_discovery(p));
                event_game_discovery();
                state = GameState.DISCOVERY;
                broadcast(ChatColor.AQUA + "You have " + (settings.discoveryLength / 20) + " seconds to discover the map.");
                discoveryStartDelay = new BukkitRunnable() {
                    public void run() {
                        game_readyToStart();
                    }
                };
                discoveryStartDelay.runTaskLater(this, settings.discoveryLength);
            } else {
                OMGPlayer.link.values().forEach(p -> gamePreparer.player_start_nonDiscovery(p));
                game_readyToStart();
            }
        }
    }

    /**
     * End discovery and start the game.
     */
    public void game_readyToStart() {
        event_game_preReadyToStart();
        gamePreparer.sortOutPlayers();
        state = GameState.INGAME;
        timer_start();
        broadcast(ChatColor.AQUA + "" + ChatColor.BOLD + "Game started!");
        game_checkForEnd();
    }

    /**
     * Check if winners are found out and game can end. Call after any player-related event.
     */
    public void game_checkForEnd() {
        if (state == GameState.INGAME) {
            team_lose(OMGTeam.getFiltered(t -> t != spectatorTeam && t.isEmpty()).toArray(new OMGTeam[0]));
            player_updateScoreboardTeams();
            if (OMGTeam.anyElseRegistered() ? OMGPlayer.oneLeft() : OMGTeam.somebodyWonOrEveryoneLost()) {
                broadcast_win();
                game_stop();
            }
        }
    }

    /**
     * Stop the game and reload OMGPI.
     */
    public void game_stop() {
        if (state == GameState.INGAME) {
            OMGPlayer.link.values().forEach(p -> {
                if (p.team != spectatorTeam) {
                    p.setTeam(spectatorTeam);
                    if (p.played) player_reward(p, "winner");
                } else if (p.played) player_reward(p, "loser");
                p.play_sound_levelup();
            });
            broadcast(ChatColor.AQUA + "You will be sent to the prelobby in 10 seconds.");
            state = GameState.ENDING;
            if (infoBar != null) infoBar.removeAll();
            new BukkitRunnable() {
                public void run() {
                    event_game_stop();
                    OMGPI.instance.reload();
                }
            }.runTaskLater(OMGPI.instance, 200L);
        }
    }

    /**
     * Set loadedMap and stop vote system.
     *
     * @param map Map name
     * @return True if success.
     */
    public boolean game_setMap(String map) {
        for (String name : new LinkedList<>(OMGMap.getAllMaps()))
            if (name.equalsIgnoreCase(map)) {
                OMGMap buffer = voteSystem.votes.keySet().stream().anyMatch(m -> m.name.equals(name)) ? voteSystem.votes.keySet().stream().filter(m -> m.name.equals(name)).findFirst().orElse(null) : new OMGMap(name);
                if (loadedMap == null) voteSystem.stop(buffer);
                else {
                    loadedMap = buffer;
                    broadcast(ChatColor.AQUA + "Selected map is " + loadedMap + ".");
                    loadedMap.load();
                }
                return true;
            }
        return false;
    }

    /**
     * Win message. Not important and can be replaced by any string in game_checkForEnd().
     * By default if there are any teams besides default and spectator, Team Wins are checked, else Player Teams.
     *
     * @return %winner%
     */
    public String game_winMessage() {
        if (OMGTeam.anyElseRegistered()) {
            Optional<OMGTeam> o = OMGTeam.getFiltered(t -> t != defaultTeam && t != spectatorTeam && t.state == OMGTeam.TeamState.WON).stream().findFirst();
            return o.map(omgTeam -> omgTeam.displayName + " won!").orElse("Nobody won!");
        }
        return (spectatorTeam.unpresent().size() < 1 ? "Nobody" : spectatorTeam.unpresent().get(0).bukkit.getName()) + " won!";
    }

    /**
     * Called before any game ready to start actions.
     */
    public void event_game_preReadyToStart() {
    }

    /**
     * Called before any game start actions.
     */
    public void event_game_preStart() {
    }

    /**
     * Called before any discovery actions.
     */
    public void event_game_discovery() {
    }

    /**
     * Called on game end.
     */
    public void event_game_stop() {
    }

    /**
     * Called on player join.
     *
     * @param p Player that joined
     */
    public void event_player_join(OMGPlayer p) {
        p.requestedTeam = defaultTeam;
        game_checkForStart();
        p.broadcast_join();
        game_checkForEnd();
    }

    /**
     * Called on player leave.
     *
     * @param p Player that left
     */
    public void event_player_leave(OMGPlayer p) {
        if (state == GameState.PRELOBBY) game_checkForStart();
        if (state == GameState.INGAME && settings.countLeaveAsKill && p.played && p.lastDamager != null && p != p.lastDamager)
            player_reward(p.lastDamager, "kill");
        p.broadcast_leave();
        game_checkForEnd();
    }

    /**
     * Called after player has been asserted.
     *
     * @param p Player that was asserted by GamePreparer
     */
    public void event_player_assert(OMGPlayer p) {
    }

    /**
     * Called when player joins prelobby world.
     *
     * @param p Player that joined
     */
    public void event_player_joinPrelobby(OMGPlayer p) {
    }

    /**
     * Called when player gets damaged.
     *
     * @param e Associated OMGEvent
     */
    public void event_player_damage(OMGDamageEvent e) {
        if (e.damage > 0.1) e.damaged.play_particle_blood();
        if (e.isDead()) {
            e.setCancelled(true);
            new OMGDeathEvent(e);
            game_checkForEnd();
        }
    }

    /**
     * Called when damage event finds out that player is dead.
     *
     * @param e Associated OMGEvent
     */
    public void event_player_death(OMGDeathEvent e) {
        if (settings.oneLife) {
            e.damaged.dropItems();
            e.damaged.setTeam(spectatorTeam);
            if (e.damageEvent.reason == OMGDamageCause.VOID)
                e.damaged.bukkit.teleport(e.damaged.bukkit.getWorld().getSpawnLocation());
            e.sendDeathMessage();
            e.damaged.played = false;
            player_reward(e.damaged, "loser");
            if (e.damaged.lastDamager != null && e.damaged.lastDamager != e.damaged) {
                e.damaged.lastDamager.play_sound_ding();
                player_reward(e.damaged.lastDamager, "kill");
            }
        } else {
            e.damaged.dropItems();
            e.damaged.respawn();
            e.sendDeathMessage();
            player_reward(e.damaged, "death");
            if (e.damaged.lastDamager != null && e.damaged.lastDamager != e.damaged) {
                e.damaged.lastDamager.play_sound_ding();
                player_reward(e.damaged.lastDamager, "kill");
            }
        }
    }

    /**
     * Called when player jumps.
     *
     * @param e Associated OMGEvent
     */
    public void event_player_jump(OMGJumpEvent e) {
    }

    /**
     * Called on team creation.
     *
     * @param team Created team
     */
    public void event_team_creation(OMGTeam team) {
    }

    /**
     * Add a player to your SQL table if you need to.
     * Use MySQL.add("UUID", p.bukkit.getUniqueId(), table) inside.
     * Table is usually game <i>name</i> variable.
     *
     * @param p Player to add
     */
    public void player_addToSQL(OMGPlayer p) {
    }

    /**
     * Give any reward to the player.
     *
     * @param p  Player that needs the reward.
     * @param id Reward ID.
     */
    public void player_reward(OMGPlayer p, String id) {
    }

    /**
     * Update scoreboard teams for all players.
     */
    public void player_updateScoreboardTeams() {
        OMGPlayer.link.values().forEach(p -> OMGTeam.registeredTeams.forEach(t -> {
            Team tt = p.bukkit.getScoreboard().getTeam(t.id);
            if (tt == null) tt = p.bukkit.getScoreboard().registerNewTeam(t.id);
            tt.setPrefix(t.prefix);
            tt.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
            for (OMGPlayer px : OMGPlayer.link.values()) {
                if (!t.contains(px)) tt.removeEntry(px.bukkit.getName());
                else if (!tt.hasEntry(px.bukkit.getName())) tt.addEntry(px.bukkit.getName());
            }
        }));
    }

    /**
     * Set player's team independently from him.
     *
     * @param p Player to set team of
     * @param t Team to set
     */
    public void player_set_team(OMGPlayer p, OMGTeam t) {
        p.fullReset();
        p.setTeam(t);
        gamePreparer.setupPlayerNoAssert(p);
        game_checkForEnd();
    }

    /**
     * Used when player asks to join the game.
     *
     * @param p Player that wants to join
     */
    public void player_request_join(OMGPlayer p) {
        if (p.team != spectatorTeam) {
            p.bukkit.sendMessage(ChatColor.DARK_AQUA + "You should be spectating to join game.");
            return;
        }
        if (settings.allowIngameJoin) {
            p.requestedTeam = defaultTeam;
            gamePreparer.setupPlayer(p);
        } else p.bukkit.sendMessage(ChatColor.DARK_AQUA + "This game does not allow joining in running game.");
        game_checkForEnd();
    }

    /**
     * Used when player asks to spectate the game.
     *
     * @param p Player to make spectator
     */
    public void player_request_spectate(OMGPlayer p) {
        if (p.team == spectatorTeam) {
            p.bukkit.sendMessage(ChatColor.DARK_AQUA + "You are already spectating.");
            return;
        }
        if (state == GameState.INGAME && settings.countLeaveAsKill && p.played && p.lastDamager != null && p != p.lastDamager)
            player_reward(p.lastDamager, "kill");
        p.fullReset();
        broadcast(ChatColor.AQUA + p.bukkit.getName() + " is now spectating.");
        game_checkForEnd();
    }

    /**
     * Called when player selects a team.
     *
     * @param p Player to check
     * @param t Team to request
     */
    public void player_request_team(OMGPlayer p, OMGTeam t) {
        p.requestedTeam = t;
        p.bukkit.sendMessage(ChatColor.DARK_AQUA + "Your team request has been set to " + t.displayName + ChatColor.DARK_AQUA + ".");
        game_checkForStart();
    }

    /**
     * Called when player selects a kit.
     *
     * @param p Player to check
     * @param kit Kit to request
     */
    public void player_request_kit(OMGPlayer p, OMGKit kit) {
        if (!settings.allowKits) {
            p.bukkit.sendMessage(ChatColor.RED + "You are not allowed to set kits in this game.");
            return;
        }
        if (kit.name.equals("Default") || player_hasKit(p, kit)) {
            p.setKit(kit, true);
            p.bukkit.sendMessage(ChatColor.GREEN + "Your kit has been set to " + kit + ChatColor.GREEN + ".");
        } else p.bukkit.sendMessage(ChatColor.GREEN + "Get the kit in Game Rolls!");
    }

    /**
     * Check if player has the kit.
     *
     * @param p Player to check
     * @param k Kit to check
     * @return True if player has kit
     */
    public boolean player_hasKit(OMGPlayer p, OMGKit k) {
        return settings.allowKits;
    }

    /**
     * Give a kit to a player.
     *
     * @param p Player to give kit to
     */
    public void player_giveKit(OMGPlayer p) {
        p.bukkit.closeInventory();
        p.bukkit.getInventory().clear();
        kit_contents(p, p.kit).forEach(nbt -> {
            int slot = nbt.getByte("Slot");
            if (slot < 9) slot = player_hotbarOrder(p).indexOf(slot + "");
            if (slot == 103) p.bukkit.getInventory().setHelmet(nbt.toItem());
            else if (slot == 102) p.bukkit.getInventory().setChestplate(nbt.toItem());
            else if (slot == 101) p.bukkit.getInventory().setLeggings(nbt.toItem());
            else if (slot == 100) p.bukkit.getInventory().setBoots(nbt.toItem());
            else p.bukkit.getInventory().setItem(slot, nbt.toItem());
        });
    }

    /**
     * Set the kit that player used last time.
     *
     * @param p Player to get kit from
     * @param kit A latest OMGKit
     */
    public void player_set_latestKit(OMGPlayer p, OMGKit kit) {
    }

    /**
     * Kit that player used last time. Used for auto-setting kits on join.
     *
     * @param p Player to get kit from
     * @return A latest OMGKit
     */
    public OMGKit player_latestKit(OMGPlayer p) {
        return OMGKit.kits.get(0);
    }

    /**
     * Get string of hotbar items order from database.
     * It is recommended to use SQL instead of hashdata here.
     *
     * @param p Player to get hotbar order from
     * @return A string with numbers 0-8 meaning slots
     */
    public String player_hotbarOrder(OMGPlayer p) {
        String s = (String) p.hashdata_get("DEFAULT_HBO");
        if (s == null) p.hashdata_set("DEFAULT_HBO", "012345678");
        return s == null || !settings.allowHotbarEdit ? "012345678" : s;
    }

    /**
     * Set string of hotbar items order in database.
     * It is recommended to use SQL instead of hashdata here.
     *
     * @param p Player to set hotbar order of
     * @param s Hotbar order itself - example, "012345678"
     */
    public void player_set_hotbarOrder(OMGPlayer p, String s) {
        p.hashdata_set("DEFAULT_HBO", s);
    }

    /**
     * Open game shop menu.
     *
     * @param p Player that need shop opened
     */
    public void player_openGameShop(OMGPlayer p) {
        if (state == GameState.INGAME && settings.allowGameShop) {
            Inventories.gameShopUpdate();
            p.bukkit.openInventory(Inventories.gameShop);
        }
    }

    /**
     * Give an item from shop to the player. You can get any value from nbt, even those set in the config.
     *
     * @param p Shop item to give to player
     * @param nbt NBT to give (will have Cost tag)
     */
    public void player_giveShopItem(OMGPlayer p, NBTParser nbt) {
        int cost = nbt.getInt("Cost");
        if (cost <= p.gameCoins) {
            p.addGameCoins(-cost);
            if (p.bukkit.getInventory().firstEmpty() == -1)
                p.bukkit.getWorld().dropItemNaturally(p.bukkit.getLocation(), nbt.toItem());
            else p.bukkit.getInventory().addItem(nbt.toItem());
            p.bukkit.sendMessage(ChatColor.DARK_AQUA + "Item bought.");
        } else p.bukkit.sendMessage(ChatColor.DARK_AQUA + "Not enough game coins.");
        game_checkForEnd();
    }

    /**
     * Location where player will spawn.
     *
     * @param p Player to get spawn of
     * @return Spawn location
     */
    public Location player_spawnLocation(OMGPlayer p) {
        return OMGPI.gameworld.bukkit.getSpawnLocation().add(0.5, 0, 0.5);
    }

    /**
     * Get a compass location to track for the player.
     *
     * @param p Player to check
     * @return Compass target location
     */
    public Location player_compassLocation(OMGPlayer p) {
        Optional<OMGPlayer> a = OMGPlayer.getFiltered(c -> c.team != spectatorTeam && c != p && (!OMGTeam.anyElseRegistered() || c.team != p.team)).stream().sorted(Comparator.comparingDouble(c -> c.bukkit.getLocation().distance(p.bukkit.getLocation()))).findFirst();
        return a.map(omgPlayer -> omgPlayer.bukkit.getLocation()).orElseGet(() -> p.bukkit.getWorld().getSpawnLocation());
    }

    /**
     * Get the place where the projectile first appeared (player hands or dispenser face).
     *
     * @param p Projectile to check
     * @return Location of projectile source
     */
    public Location shootSource(Projectile p) {
        return shootSources.get(p);
    }

    /**
     * Check if any of the states is equal to current one.
     *
     * @param states States to check
     * @return any of the states is equal to current state
     */
    public boolean checkStates(GameState... states) {
        for (GameState state1 : states) {
            if (state1 == state) return true;
        }
        return false;
    }

    /**
     * Sends a win message defined in mapfig to all players.
     */
    public void broadcast_win() {
        broadcast(ChatColor.GREEN + loadedMap.mapfig.winMessage().replaceAll("%winner%", game_winMessage()));
    }

    /**
     * Broadcast a message without unnecessary checks.
     *
     * @param message Text to send to all players and to log.
     */
    public void broadcast(String message) {
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(message));
        OMGPI.iLog(message);
    }

    /**
     * Set InfoBar with some info.
     */
    public void bar_set() {
        if (infoBar != null) {
            if (state == GameState.INGAME) {
                if (settings.gameLength != 0) {
                    infoBar.setVisible(true);
                    infoBar.setProgress(Math.min(1, (1d / settings.gameLength) * (double) timerTicks));
                    infoBar.setTitle(ChatColor.AQUA + "Time left: " + ChatColor.RESET + timer_toString());
                    Bukkit.getOnlinePlayers().stream().filter(p -> !infoBar.getPlayers().contains(p)).forEach(p -> infoBar.addPlayer(p));
                } else {
                    infoBar.setVisible(false);
                    infoBar.setProgress(1);
                    infoBar.setTitle(ChatColor.AQUA + "Game running!");
                    Bukkit.getOnlinePlayers().stream().filter(p -> !infoBar.getPlayers().contains(p)).forEach(p -> infoBar.addPlayer(p));
                }
            } else if (countdown.isRunning) {
                infoBar.setVisible(true);
                infoBar.setProgress(Math.min(1, (1d / gamefig.getWaitTime()) * (double) countdown.time));
                infoBar.setTitle(ChatColor.AQUA + "Game will run soon...");
                Bukkit.getOnlinePlayers().stream().filter(p -> !infoBar.getPlayers().contains(p)).forEach(p -> infoBar.addPlayer(p));
            } else {
                infoBar.setVisible(true);
                infoBar.setProgress(1);
                infoBar.setTitle(ChatColor.AQUA + "Game will run soon...");
                Bukkit.getOnlinePlayers().stream().filter(p -> !infoBar.getPlayers().contains(p)).forEach(p -> infoBar.addPlayer(p));
            }
        }
    }

    /**
     * Initialize timer.
     */
    public void timer_start() {
        timerTicks = settings.gameLength;
        timer = new BukkitRunnable() {
            public void run() {
                bar_set();
                if (settings.gameLength != 0) {
                    timer_tick();
                    if (state == GameState.INGAME) {
                        if (--timerTicks <= 0) {
                            timer_out();
                            cancel();
                        }
                    } else cancel();
                }
            }
        };
        timer.runTaskTimer(OMGPI.instance, 0L, 20L);
    }

    /**
     * Called on every timer tick for safe usage.
     */
    public void timer_tick() {
    }

    /**
     * Called when time is out.
     */
    public void timer_out() {
        broadcast(ChatColor.YELLOW + "" + ChatColor.BOLD + "Time is out!");
        game_stop();
    }

    /**
     * Get seconds as hh:mm:ss format.
     *
     * @return Converted game time from UNIX-like to readable format.
     */
    public String timer_toString() {
        return (timerTicks / 3600 < 1 ? "" : timerTicks / 3600 + ":") + ((timerTicks / 60) % 60 < 10 ? "0" + (timerTicks / 60) % 60 : (timerTicks / 60) % 60) + ":" + (timerTicks % 60 < 10 ? "0" + timerTicks % 60 : timerTicks % 60);
    }

    /**
     * Set team state to LOST and make all players in it spectators, not played and give loser rewards.
     *
     * @param t Teams that will lose
     */
    public void team_lose(OMGTeam... t) {
        for (OMGTeam t1 : t) {
            t1.state = OMGTeam.TeamState.LOST;
            t1.list().forEach(p -> {
                p.played = false;
                player_reward(p, "loser");
                p.setTeam(spectatorTeam);
            });
        }
    }

    /**
     * Get player-specific kit contents.
     *
     * @param p Player that needs kit
     * @param k Kit that has the contents
     * @return Kit contents
     */
    public List<NBTParser> kit_contents(OMGPlayer p, OMGKit k) {
        return (loadedMap != null && loadedMap.mapfig.contains("teams." + p.team + ".kits." + k) ? loadedMap.mapfig.getStringList("teams." + p.team + ".kits." + k) : k.getStringList("contents")).stream().map(NBTParser::new).collect(Collectors.toList());
    }

    /**
     * Get player-specific loot contents.
     * Warning: p may be null if the chest was not destroyed by player.
     *
     * @param p Player that needs loots
     * @param l Loot ID
     * @return Loot contents
     */
    public OMGLoot.LootParser loot_contents(OMGPlayer p, String l) {
        return loadedMap != null && loadedMap.mapfig.contains("loots." + l) ? new OMGLoot.LootParser(loadedMap.mapfig.getStringList("loots." + l)) : OMGLoot.nullFreeContents(l);
    }
}