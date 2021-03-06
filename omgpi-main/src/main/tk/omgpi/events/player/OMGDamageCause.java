package tk.omgpi.events.player;

import org.bukkit.ChatColor;
import org.bukkit.event.entity.EntityDamageEvent;
import tk.omgpi.OMGPI;
import tk.omgpi.game.OMGPlayer;
import tk.omgpi.utils.OMGList;

import java.util.Arrays;
import java.util.Random;

public abstract class OMGDamageCause {
    public static OMGList<OMGDamageCause> values = new OMGList<>();
    public static String SPACING = ChatColor.DARK_AQUA + "";

    public static OMGDamageCause VOID;
    public static OMGDamageCause ATTACK;
    public static OMGDamageCause SHOT;
    public static OMGDamageCause CACTUS;
    public static OMGDamageCause WATER;
    public static OMGDamageCause SUFFOCATE;
    public static OMGDamageCause SUICIDE;
    public static OMGDamageCause LAVA;
    public static OMGDamageCause FIRE;
    public static OMGDamageCause EXPLODE;
    public static OMGDamageCause FALL;
    public static OMGDamageCause SQUISH;
    public static OMGDamageCause POTION;
    public static OMGDamageCause POISON;
    public static OMGDamageCause STARVE;
    public static OMGDamageCause WALL;
    public static OMGDamageCause LIGHTNING;
    public static OMGDamageCause OVERCROWDED;
    public static OMGDamageCause MAGMA;
    static {
        try {
            VOID = new OMGDamageCause("void", EntityDamageEvent.DamageCause.VOID) {
                public String getDeathMessage(OMGPlayer damaged) {
                    return rDMBLD(damaged, " fell into the void", " has gone too low");
                }
            };
            ATTACK = new OMGDamageCause("attack", EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                public String getDeathMessage(OMGPlayer damaged) {
                    String[] s = new String[]{" was killed by " + damaged.lastDamager, " was slain by " + damaged.lastDamager};
                    return SPACING + damaged + SPACING + s[new Random().nextInt(s.length)] + SPACING + "!";
                }
            };
            SHOT = new OMGDamageCause("shot", EntityDamageEvent.DamageCause.PROJECTILE) {
                public String getDeathMessage(OMGPlayer damaged) {
                    String r = new String[]{" was shot by ", " caught an arrow from "}[new Random().nextInt(2)];
                    double sh = damaged.lastProjectileShotBy.getLocation().distance(OMGPI.g.shootSource(damaged.lastProjectileShotBy));
                    return SPACING + damaged + SPACING + r + damaged.lastDamager + SPACING + "!" + (damaged.lastDamager == null ? "" : " (From " + (sh > 200 ? ChatColor.DARK_RED + "" + ChatColor.BOLD : (sh > 150 ? ChatColor.WHITE : (sh > 100 ? ChatColor.LIGHT_PURPLE : (sh > 50 ? ChatColor.RED : (sh > 25 ? ChatColor.YELLOW : ChatColor.GREEN))))) + (sh + "").split("\\.")[0] + "." + (sh + "").split("\\.")[1].substring(0, 1) + SPACING + " blocks)");
                }
            };
            CACTUS = new OMGDamageCause("cactus", EntityDamageEvent.DamageCause.CONTACT) {
                public String getDeathMessage(OMGPlayer damaged) {
                    return rDMBLD(damaged, " hugged a spiky thing", " hugged a cactus");
                }
            };
            WATER = new OMGDamageCause("water", EntityDamageEvent.DamageCause.DROWNING) {
                public String getDeathMessage(OMGPlayer damaged) {
                    return rDMBLD(damaged, " inhaled too much water", " thought he was a fish");
                }
            };
            SUFFOCATE = new OMGDamageCause("suffocate", EntityDamageEvent.DamageCause.SUFFOCATION) {
                public String getDeathMessage(OMGPlayer damaged) {
                    return rDMBLD(damaged, " forgot how to breathe", " hid in the blocks for too long");
                }
            };
            SUICIDE = new OMGDamageCause("suicide", EntityDamageEvent.DamageCause.SUICIDE) {
                public String getDeathMessage(OMGPlayer damaged) {
                    return rDMBLD(damaged, " killed himself", " was bored to death");
                }
            };
            LAVA = new OMGDamageCause("lava", EntityDamageEvent.DamageCause.LAVA) {
                public String getDeathMessage(OMGPlayer damaged) {
                    return rDMBLD(damaged, " inhaled too much lava", ", no! That is not juice");
                }
            };
            FIRE = new OMGDamageCause("fire", EntityDamageEvent.DamageCause.FIRE, EntityDamageEvent.DamageCause.FIRE_TICK) {
                public String getDeathMessage(OMGPlayer damaged) {
                    return rDMBLD(damaged, " got burned down alive", " tried to play with fire");
                }
            };
            EXPLODE = new OMGDamageCause("explode", EntityDamageEvent.DamageCause.ENTITY_EXPLOSION, EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
                public String getDeathMessage(OMGPlayer damaged) {
                    return rDMBLD(damaged, " wasn't fast enough to run away from explosion", " got nuked");
                }
            };
            FALL = new OMGDamageCause("fall", EntityDamageEvent.DamageCause.FALL) {
                public String getDeathMessage(OMGPlayer damaged) {
                    return rDMBLD(damaged, " slipped on a fish", " slipped on a banana");
                }
            };
            SQUISH = new OMGDamageCause("squish", EntityDamageEvent.DamageCause.FALLING_BLOCK) {
                public String getDeathMessage(OMGPlayer damaged) {
                    return rDMBLD(damaged, " was squished", " didn't look up");
                }
            };
            POTION = new OMGDamageCause("potion", EntityDamageEvent.DamageCause.MAGIC) {
                public String getDeathMessage(OMGPlayer damaged) {
                    return rDMBLD(damaged, " fell under damage spell rain", " got into damage spell range");
                }
            };
            POISON = new OMGDamageCause("poison", EntityDamageEvent.DamageCause.POISON) {
                public String getDeathMessage(OMGPlayer damaged) {
                    return rDMBLD(damaged, " was death-poisoned");
                }
            };
            STARVE = new OMGDamageCause("starve", EntityDamageEvent.DamageCause.MAGIC) {
                public String getDeathMessage(OMGPlayer damaged) {
                    return rDMBLD(damaged, " starved to death", " forgot to eat");
                }
            };
            LIGHTNING = new OMGDamageCause("lightning", EntityDamageEvent.DamageCause.LIGHTNING) {
                public String getDeathMessage(OMGPlayer damaged) {
                    return rDMBLD(damaged, " was literally shocked", " caught a plasma bolt");
                }
            };
            WALL = new OMGDamageCause("wall", EntityDamageEvent.DamageCause.FLY_INTO_WALL) {
                public String getDeathMessage(OMGPlayer damaged) {
                    return rDMBLD(damaged, " was not accurate with his elytra", " has experienced kinetic energy");
                }
            };
            OVERCROWDED = new OMGDamageCause("overcrowded", EntityDamageEvent.DamageCause.valueOf("CRAMMING")) {
                public String getDeathMessage(OMGPlayer damaged) {
                    return rDMBLD(damaged, " got crammed in the crowd", " got compressed");
                }
            };
            MAGMA = new OMGDamageCause("magma", EntityDamageEvent.DamageCause.valueOf("HOT_FLOOR")) {
                public String getDeathMessage(OMGPlayer damaged) {
                    return rDMBLD(damaged, ", the floor is magma", " has melt to the floor");
                }
            };
        } catch (IllegalArgumentException ignored) {
        }
    }

    public EntityDamageEvent.DamageCause[] bukkit;
    public String name;

    /**
     * Use this to create custom damage causes.
     *
     * @param name Name to access cause in config
     * @param e When no args are written, this counts as a CUSTOM bukkit damage cause.
     */
    public OMGDamageCause(String name, EntityDamageEvent.DamageCause... e) {
        this.name = name;
        bukkit = e.length == 0 ? new EntityDamageEvent.DamageCause[]{EntityDamageEvent.DamageCause.CUSTOM} : e;
        values.add(this);
    }

    /**
     * Get kill message by cause and player.
     *
     * @param damaged Player that died.
     * @return The kill message.
     */
    public abstract String getDeathMessage(OMGPlayer damaged);

    /**
     * Get OMGDamageCause by bukkit damage cause.
     *
     * @param e Bukkit cause.
     * @return OMGDamageCause.
     */
    public static OMGDamageCause getByBukkit(EntityDamageEvent.DamageCause e) {
        return values.stream().filter(b -> Arrays.asList(b.bukkit).contains(e)).findFirst().orElse(null);
    }

    /**
     * Random Death Message by Last Damager.
     *
     * @param damaged Player that died
     * @param s Message variants
     * @return Damaged [random message]! (Last damaged by LastDamager)
     */
    public static String rDMBLD(OMGPlayer damaged, String... s) {
        return SPACING + damaged + SPACING + s[new Random().nextInt(s.length)] + SPACING + "!" + (damaged.lastDamager == null ? "" : " (Last damaged by " + damaged.lastDamager + SPACING + ")");
    }

    /**
     * Get a cause by name
     *
     * @param m Cause name
     * @return A found cause or null
     */
    public static OMGDamageCause valueOf(String m) {
        return values.omgstream().filter(dc -> dc.name.equalsIgnoreCase(m)).findFirst().orElse(null);
    }
}
