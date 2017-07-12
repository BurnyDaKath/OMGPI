package tk.omgpi.utils;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import static tk.omgpi.utils.ReflectionUtils.*;

/**
 * Utils for parsing an item from NBT strings.
 * Suppressing all warning since I know what am I doing, and ok with throwing an exception.
 */
@SuppressWarnings("all")
public class NBTParser {
    /**
     * Parsed tag, use of it is not recommended
     */
    public Object nbtTagCompound;

    /**
     * Parse a string
     *
     * @param s String to parse
     */
    public NBTParser(String s) {
        nbtTagCompound = null;
        try {
            nbtTagCompound = getClazz(nmsclasses, "MojangsonParser").getDeclaredMethod("parse", String.class).invoke(null, ChatColor.translateAlternateColorCodes('&', s));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get item from stored compound.
     *
     * @return Bukkit ItemStack, null if no compound is stored.
     */
    public org.bukkit.inventory.ItemStack toItem() {
        Class craftItemStack = getClazz(cbclasses, "CraftItemStack");
        Class itemStack = getClazz(nmsclasses, "ItemStack");
        Class nbtTagCompound = getClazz(nmsclasses, "NBTTagCompound");
        try {
            if (intVer() > 10) { //v1_11_R1 or later
                Object rf = craftItemStack.getDeclaredMethod("asBukkitCopy", itemStack).invoke(null, itemStack.getDeclaredConstructor(nbtTagCompound).newInstance(this.nbtTagCompound));
                return nbtTagCompound != null ? (ItemStack) rf : null;
            }
            Object rf = craftItemStack.getDeclaredMethod("asBukkitCopy", itemStack).invoke(null, itemStack.getDeclaredMethod("createStack", nbtTagCompound).invoke(null, this.nbtTagCompound));
            return nbtTagCompound != null ? (ItemStack) rf : null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get NBTParser of "tag" compound of already existing ItemStack.
     *
     * @param i Bukkit ItemStack.
     * @return Parsed NBT tag.
     */
    public static NBTParser getTagCompound(ItemStack i) {
        Class craftItemStack = getClazz(cbclasses, "CraftItemStack");
        Class itemStack = getClazz(nmsclasses, "ItemStack");
        try {
            Object tag = itemStack.getDeclaredMethod("getTag").invoke(craftItemStack.getDeclaredMethod("asNMSCopy", ItemStack.class).invoke(null, i));
            if (tag != null) return new NBTParser(tag + "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new NBTParser("{}");
    }

    /**
     * Get Block NBT.
     *
     * @param b Bukkit Block.
     * @return Parsed NBT tag.
     */
    public static NBTParser getTagCompound(Block b) {
        NBTParser nbt = new NBTParser("{}");
        Class item = getClazz(nmsclasses, "Item");
        Class tileEntity = getClazz(nmsclasses, "TileEntity");
        Class registryMaterials = getClazz(nmsclasses, "RegistryMaterials");
        Class minecraftKey = getClazz(nmsclasses, "MinecraftKey");
        Class craftWorld = getClazz(cbclasses, "CraftWorld");
        try {
            Object te = craftWorld.getDeclaredMethod("getTileEntityAt", int.class, int.class, int.class).invoke(b.getWorld(), b.getX(), b.getY(), b.getZ());
            if (te == null) {
                nbt.setString("id", (String) minecraftKey.getDeclaredMethod("getKey").invoke(registryMaterials.getMethod("b", item).invoke(item.getDeclaredField("REGISTRY").get(null), item.getDeclaredMethod("getById", int.class).invoke(null, b.getTypeId()))));
                nbt.setInt("x", b.getX());
                nbt.setInt("y", b.getY());
                nbt.setInt("z", b.getZ());
            } else nbt.nbtTagCompound = tileEntity.getDeclaredMethod("save", getClazz(nmsclasses, "NBTTagCompound")).invoke(te, nbt.nbtTagCompound);
        } catch (Exception e) {
            e.printStackTrace();
        }
        nbt.setByte("data", b.getData());
        return nbt;
    }

    /**
     * Set NBT "tag" compound of ItemStack clone.
     *
     * @param i Bukkit ItemStack.
     * @return ItemStack with changed tag.
     */
    public ItemStack setTagCompound(org.bukkit.inventory.ItemStack i) {
        Class craftItemStack = getClazz(cbclasses, "CraftItemStack");
        Class itemStack = getClazz(nmsclasses, "ItemStack");
        try {
            Object r = craftItemStack.getDeclaredMethod("asNMSCopy", ItemStack.class).invoke(null, i);
            itemStack.getDeclaredMethod("setTag", getClazz(nmsclasses, "NBTBase").getClass()).invoke(r, getClazz(nmsclasses, "NBTBase"));
            return (ItemStack) craftItemStack.getDeclaredMethod("asBukkitCopy", itemStack).invoke(null, r);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Set NBT of Block.
     *
     * @param b Bukkit Block.
     * @return Block with changed tags.
     */
    public Block setTagCompound(Block b) {
        setInt("x", b.getX());
        setInt("y", b.getY());
        setInt("z", b.getZ());
        if (hasKey("id")) b.setType(new NBTParser("{id:" + getString("id") + ",Count:1}").toItem().getType());
        if (hasKey("data")) b.setData(getByte("data"));
        Class tileEntity = getClazz(nmsclasses, "TileEntity");
        Class craftWorld = getClazz(cbclasses, "CraftWorld");
        try {
            Object te = craftWorld.getDeclaredMethod("getTileEntityAt", int.class, int.class, int.class).invoke(b.getWorld(), b.getX(), b.getY(), b.getZ());
            if (te != null) tileEntity.getDeclaredMethod("a", getClazz(nmsclasses, "NBTTagCompound")).invoke(te, nbtTagCompound);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return b;
    }
    
    /* -=-=-=-=-=-=-=-=- NBT TAGS MANIPULATION -=-=-=-=-=-=-=-=- */

    //STRING

    /**
     * Set string value for given tag
     *
     * @param tag   Tag to set value in
     * @param key   Key to set value in, separate compounds by dots
     * @param value Value to set
     */
    public static void setString(Object tag, String key, String value) {
        Class nbtTagCompound = getClazz(nmsclasses, "NBTTagCompound");
        try {
            if (key.contains(".")) {
                String subkey = key.split("\\.")[0];
                Object t = nbtTagCompound.getDeclaredMethod("getCompound", String.class).invoke(tag, subkey);
                setString(t, key.substring(key.indexOf('.') + 1), value);
                nbtTagCompound.getDeclaredMethod("set", String.class, getClazz(nmsclasses, "NBTBase")).invoke(tag, subkey, t);
            } else nbtTagCompound.getDeclaredMethod("setString", String.class, String.class).invoke(tag, key, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Set string value in NBTParser's tag
     *
     * @param key   Key to set value in, separate compounds by dots
     * @param value Value to set
     */
    public void setString(String key, String value) {
        setString(nbtTagCompound, key, value);
    }

    /**
     * Get string value from given tag
     *
     * @param tag Tag to get value from
     * @param key Key to get value from, separate compounds by dots
     * @return Value
     */
    public static String getString(Object tag, String key) {
        Class nbtTagCompound = getClazz(nmsclasses, "NBTTagCompound");
        try {
            if (key.contains(".")) {
                String subkey = key.split("\\.")[0];
                return getString(nbtTagCompound.getDeclaredMethod("getCompound", String.class).invoke(tag, subkey), key.substring(key.indexOf('.') + 1));
            } else return (String) nbtTagCompound.getDeclaredMethod("getString", String.class).invoke(tag, key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get string value from NBTParser's tag
     *
     * @param key Key to get value from, separate compounds by dots
     * @return Value
     */
    public String getString(String key) {
        return getString(nbtTagCompound, key);
    }

    //INT

    /**
     * Set int value for given tag
     *
     * @param tag   Tag to set value in
     * @param key   Key to set value in, separate compounds by dots
     * @param value Value to set
     */
    public static void setInt(Object tag, String key, int value) {
        Class nbtTagCompound = getClazz(nmsclasses, "NBTTagCompound");
        try {
            if (key.contains(".")) {
                String subkey = key.split("\\.")[0];
                Object t = nbtTagCompound.getDeclaredMethod("getCompound", String.class).invoke(tag, subkey);
                setInt(t, key.substring(key.indexOf('.') + 1), value);
                nbtTagCompound.getDeclaredMethod("set", String.class, getClazz(nmsclasses, "NBTBase")).invoke(tag, subkey, t);
            } else nbtTagCompound.getDeclaredMethod("setInt", String.class, int.class).invoke(tag, key, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Set int value in NBTParser's tag
     *
     * @param key   Key to set value in, separate compounds by dots
     * @param value Value to set
     */
    public void setInt(String key, int value) {
        setInt(nbtTagCompound, key, value);
    }

    /**
     * Get int value from given tag
     *
     * @param tag Tag to get value from
     * @param key Key to get value from, separate compounds by dots
     * @return Value
     */
    public static int getInt(Object tag, String key) {
        Class nbtTagCompound = getClazz(nmsclasses, "NBTTagCompound");
        try {
            if (key.contains(".")) {
                String subkey = key.split("\\.")[0];
                return getInt(nbtTagCompound.getDeclaredMethod("getCompound", String.class).invoke(tag, subkey), key.substring(key.indexOf('.') + 1));
            } else return (int) nbtTagCompound.getDeclaredMethod("getInt", String.class).invoke(tag, key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Get int value from NBTParser's tag
     *
     * @param key Key to get value from, separate compounds by dots
     * @return Value
     */
    public int getInt(String key) {
        return getInt(nbtTagCompound, key);
    }

    //LONG

    /**
     * Set long value for given tag
     *
     * @param tag   Tag to set value in
     * @param key   Key to set value in, separate compounds by dots
     * @param value Value to set
     */
    public static void setLong(Object tag, String key, long value) {
        Class nbtTagCompound = getClazz(nmsclasses, "NBTTagCompound");
        try {
            if (key.contains(".")) {
                String subkey = key.split("\\.")[0];
                Object t = nbtTagCompound.getDeclaredMethod("getCompound", String.class).invoke(tag, subkey);
                setLong(t, key.substring(key.indexOf('.') + 1), value);
                nbtTagCompound.getDeclaredMethod("set", String.class, getClazz(nmsclasses, "NBTBase")).invoke(tag, subkey, t);
            } else nbtTagCompound.getDeclaredMethod("setLong", String.class, long.class).invoke(tag, key, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Set long value in NBTParser's tag
     *
     * @param key   Key to set value in, separate compounds by dots
     * @param value Value to set
     */
    public void setLong(String key, long value) {
        setLong(nbtTagCompound, key, value);
    }

    /**
     * Get long value from given tag
     *
     * @param tag Tag to get value from
     * @param key Key to get value from, separate compounds by dots
     * @return Value
     */
    public static long getLong(Object tag, String key) {
        Class nbtTagCompound = getClazz(nmsclasses, "NBTTagCompound");
        try {
            if (key.contains(".")) {
                String subkey = key.split("\\.")[0];
                return getLong(nbtTagCompound.getDeclaredMethod("getCompound", String.class).invoke(tag, subkey), key.substring(key.indexOf('.') + 1));
            } else return (long) nbtTagCompound.getDeclaredMethod("getLong", String.class).invoke(tag, key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Get long value from NBTParser's tag
     *
     * @param key Key to get value from, separate compounds by dots
     * @return Value
     */
    public long getLong(String key) {
        return getLong(nbtTagCompound, key);
    }

    //SHORT

    /**
     * Set short value for given tag
     *
     * @param tag   Tag to set value in
     * @param key   Key to set value in, separate compounds by dots
     * @param value Value to set
     */
    public static void setShort(Object tag, String key, short value) {
        Class nbtTagCompound = getClazz(nmsclasses, "NBTTagCompound");
        try {
            if (key.contains(".")) {
                String subkey = key.split("\\.")[0];
                Object t = nbtTagCompound.getDeclaredMethod("getCompound", String.class).invoke(tag, subkey);
                setShort(t, key.substring(key.indexOf('.') + 1), value);
                nbtTagCompound.getDeclaredMethod("set", String.class, getClazz(nmsclasses, "NBTBase")).invoke(tag, subkey, t);
            } else nbtTagCompound.getDeclaredMethod("setShort", String.class, short.class).invoke(tag, key, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Set short value in NBTParser's tag
     *
     * @param key   Key to set value in, separate compounds by dots
     * @param value Value to set
     */
    public void setShort(String key, short value) {
        setShort(nbtTagCompound, key, value);
    }

    /**
     * Get short value from given tag
     *
     * @param tag Tag to get value from
     * @param key Key to get value from, separate compounds by dots
     * @return Value
     */
    public static short getShort(Object tag, String key) {
        Class nbtTagCompound = getClazz(nmsclasses, "NBTTagCompound");
        try {
            if (key.contains(".")) {
                String subkey = key.split("\\.")[0];
                return getShort(nbtTagCompound.getDeclaredMethod("getCompound", String.class).invoke(tag, subkey), key.substring(key.indexOf('.') + 1));
            } else return (short) nbtTagCompound.getDeclaredMethod("getShort", String.class).invoke(tag, key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Get short value from NBTParser's tag
     *
     * @param key Key to get value from, separate compounds by dots
     * @return Value
     */
    public short getShort(String key) {
        return getShort(nbtTagCompound, key);
    }

    //BYTE

    /**
     * Set byte value for given tag
     *
     * @param tag   Tag to set value in
     * @param key   Key to set value in, separate compounds by dots
     * @param value Value to set
     */
    public static void setByte(Object tag, String key, byte value) {
        Class nbtTagCompound = getClazz(nmsclasses, "NBTTagCompound");
        try {
            if (key.contains(".")) {
                String subkey = key.split("\\.")[0];
                Object t = nbtTagCompound.getDeclaredMethod("getCompound", String.class).invoke(tag, subkey);
                setByte(t, key.substring(key.indexOf('.') + 1), value);
                nbtTagCompound.getDeclaredMethod("set", String.class, getClazz(nmsclasses, "NBTBase")).invoke(tag, subkey, t);
            } else nbtTagCompound.getDeclaredMethod("setByte", String.class, byte.class).invoke(tag, key, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Set byte value in NBTParser's tag
     *
     * @param key   Key to set value in, separate compounds by dots
     * @param value Value to set
     */
    public void setByte(String key, byte value) {
        setByte(nbtTagCompound, key, value);
    }

    /**
     * Get byte value from given tag
     *
     * @param tag Tag to get value from
     * @param key Key to get value from, separate compounds by dots
     * @return Value
     */
    public static byte getByte(Object tag, String key) {
        Class nbtTagCompound = getClazz(nmsclasses, "NBTTagCompound");
        try {
            if (key.contains(".")) {
                String subkey = key.split("\\.")[0];
                return getByte(nbtTagCompound.getDeclaredMethod("getCompound", String.class).invoke(tag, subkey), key.substring(key.indexOf('.') + 1));
            } else return (byte) nbtTagCompound.getDeclaredMethod("getByte", String.class).invoke(tag, key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Get byte value from NBTParser's tag
     *
     * @param key Key to get value from, separate compounds by dots
     * @return Value
     */
    public byte getByte(String key) {
        return getByte(nbtTagCompound, key);
    }

    //BOOLEAN

    /**
     * Set boolean value for given tag
     *
     * @param tag   Tag to set value in
     * @param key   Key to set value in, separate compounds by dots
     * @param value Value to set
     */
    public static void setBoolean(Object tag, String key, boolean value) {
        Class nbtTagCompound = getClazz(nmsclasses, "NBTTagCompound");
        try {
            if (key.contains(".")) {
                String subkey = key.split("\\.")[0];
                Object t = nbtTagCompound.getDeclaredMethod("getCompound", String.class).invoke(tag, subkey);
                setBoolean(t, key.substring(key.indexOf('.') + 1), value);
                nbtTagCompound.getDeclaredMethod("set", String.class, getClazz(nmsclasses, "NBTBase")).invoke(tag, subkey, t);
            } else nbtTagCompound.getDeclaredMethod("setBoolean", String.class, boolean.class).invoke(tag, key, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Set boolean value in NBTParser's tag
     *
     * @param key   Key to set value in, separate compounds by dots
     * @param value Value to set
     */
    public void setBoolean(String key, boolean value) {
        setBoolean(nbtTagCompound, key, value);
    }

    /**
     * Get boolean value from given tag
     *
     * @param tag Tag to get value from
     * @param key Key to get value from, separate compounds by dots
     * @return Value
     */
    public static boolean getBoolean(Object tag, String key) {
        Class nbtTagCompound = getClazz(nmsclasses, "NBTTagCompound");
        try {
            if (key.contains(".")) {
                String subkey = key.split("\\.")[0];
                return getBoolean(nbtTagCompound.getDeclaredMethod("getCompound", String.class).invoke(tag, subkey), key.substring(key.indexOf('.') + 1));
            } else return (boolean) nbtTagCompound.getDeclaredMethod("getBoolean", String.class).invoke(tag, key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Get boolean value from NBTParser's tag
     *
     * @param key Key to get value from, separate compounds by dots
     * @return Value
     */
    public boolean getBoolean(String key) {
        return getBoolean(nbtTagCompound, key);
    }

    //DOUBLE

    /**
     * Set double value for given tag
     *
     * @param tag   Tag to set value in
     * @param key   Key to set value in, separate compounds by dots
     * @param value Value to set
     */
    public static void setDouble(Object tag, String key, double value) {
        Class nbtTagCompound = getClazz(nmsclasses, "NBTTagCompound");
        try {
            if (key.contains(".")) {
                String subkey = key.split("\\.")[0];
                Object t = nbtTagCompound.getDeclaredMethod("getCompound", String.class).invoke(tag, subkey);
                setDouble(t, key.substring(key.indexOf('.') + 1), value);
                nbtTagCompound.getDeclaredMethod("set", String.class, getClazz(nmsclasses, "NBTBase")).invoke(tag, subkey, t);
            } else nbtTagCompound.getDeclaredMethod("setDouble", String.class, double.class).invoke(tag, key, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Set double value in NBTParser's tag
     *
     * @param key   Key to set value in, separate compounds by dots
     * @param value Value to set
     */
    public void setDouble(String key, double value) {
        setDouble(nbtTagCompound, key, value);
    }

    /**
     * Get double value from given tag
     *
     * @param tag Tag to get value from
     * @param key Key to get value from, separate compounds by dots
     * @return Value
     */
    public static double getDouble(Object tag, String key) {
        Class nbtTagCompound = getClazz(nmsclasses, "NBTTagCompound");
        try {
            if (key.contains(".")) {
                String subkey = key.split("\\.")[0];
                return getDouble(nbtTagCompound.getDeclaredMethod("getCompound", String.class).invoke(tag, subkey), key.substring(key.indexOf('.') + 1));
            } else return (double) nbtTagCompound.getDeclaredMethod("getDouble", String.class).invoke(tag, key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Get double value from NBTParser's tag
     *
     * @param key Key to get value from, separate compounds by dots
     * @return Value
     */
    public double getDouble(String key) {
        return getDouble(nbtTagCompound, key);
    }

    //FLOAT

    /**
     * Set float value for given tag
     *
     * @param tag   Tag to set value in
     * @param key   Key to set value in, separate compounds by dots
     * @param value Value to set
     */
    public static void setFloat(Object tag, String key, float value) {
        Class nbtTagCompound = getClazz(nmsclasses, "NBTTagCompound");
        try {
            if (key.contains(".")) {
                String subkey = key.split("\\.")[0];
                Object t = nbtTagCompound.getDeclaredMethod("getCompound", String.class).invoke(tag, subkey);
                setFloat(t, key.substring(key.indexOf('.') + 1), value);
                nbtTagCompound.getDeclaredMethod("set", String.class, getClazz(nmsclasses, "NBTBase")).invoke(tag, subkey, t);
            } else nbtTagCompound.getDeclaredMethod("setFloat", String.class, float.class).invoke(tag, key, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Set float value in NBTParser's tag
     *
     * @param key   Key to set value in, separate compounds by dots
     * @param value Value to set
     */
    public void setFloat(String key, float value) {
        setFloat(nbtTagCompound, key, value);
    }

    /**
     * Get float value from given tag
     *
     * @param tag Tag to get value from
     * @param key Key to get value from, separate compounds by dots
     * @return Value
     */
    public static float getFloat(Object tag, String key) {
        Class nbtTagCompound = getClazz(nmsclasses, "NBTTagCompound");
        try {
            if (key.contains(".")) {
                String subkey = key.split("\\.")[0];
                return getFloat(nbtTagCompound.getDeclaredMethod("getCompound", String.class).invoke(tag, subkey), key.substring(key.indexOf('.') + 1));
            } else return (float) nbtTagCompound.getDeclaredMethod("getFloat", String.class).invoke(tag, key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Get float value from NBTParser's tag
     *
     * @param key Key to get value from, separate compounds by dots
     * @return Value
     */
    public float getFloat(String key) {
        return getFloat(nbtTagCompound, key);
    }

    //HAS KEY

    /**
     * Check if key is present in given tag
     *
     * @param tag Tag to get value from
     * @param key Key to get value from, separate compounds by dots
     * @return True if key is present, false otherwise
     */
    public static boolean hasKey(Object tag, String key) {
        Class nbtTagCompound = getClazz(nmsclasses, "NBTTagCompound");
        try {
            if (key.contains(".")) {
                String subkey = key.split("\\.")[0];
                return hasKey(nbtTagCompound.getDeclaredMethod("getCompound", String.class).invoke(tag, subkey), key.substring(key.indexOf('.') + 1));
            } else return (boolean) nbtTagCompound.getDeclaredMethod("hasKey", String.class).invoke(tag, key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Check if key is present in NBTParser's tag
     *
     * @param key Key to get value from, separate compounds by dots
     * @return True if key is present, false otherwise
     */
    public boolean hasKey(String key) {
        return hasKey(nbtTagCompound, key);
    }
}
