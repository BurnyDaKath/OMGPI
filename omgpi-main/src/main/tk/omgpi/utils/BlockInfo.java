package tk.omgpi.utils;

import org.apache.commons.io.FileUtils;
import org.bukkit.Location;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Saves and loads blocks. Can use files.
 */
public class BlockInfo {
    /**
     * Location relative to which blocks are set or saved.
     */
    public Location anchor;
    /**
     * Blocks in NBT format, unparsed. Coordinate tags x, y, z are relative to anchor.
     */
    public List<String> blocks;

    /**
     * Create empty block info.
     *
     * @param anchor Location relative to which blocks are set or saved.
     */
    public BlockInfo(Location anchor) {
        this(anchor, new LinkedList<>());
    }

    /**
     * Create empty block info.
     *
     * @param anchor Location relative to which blocks are set or saved.
     * @param blocks Blocks in NBT format, unparsed. Coordinate tags x, y, z are relative to anchor.
     */
    public BlockInfo(Location anchor, List<String> blocks) {
        this.anchor = anchor;
        this.blocks = blocks;
    }

    /**
     * Load a file into block list.
     *
     * @param f File to load
     */
    public void loadFromFile(File f) {
        try {
            blocks = FileUtils.readLines(f, Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Save block list into a file.
     *
     * @param f File to save to
     */
    public void saveToFile(File f) {
        try {
            FileUtils.writeLines(f, blocks);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Put blocks in a cuboid into block list.
     *
     * @param cuboid Cuboid to get blocks from. Anchor's world is used.
     */
    public void getFromCoordinates(double[] cuboid) {
        Integer[] coords = Arrays.stream(cuboid).boxed().map(Double::intValue).collect(Collectors.toList()).toArray(new Integer[0]);
        for (int x = coords[0]; x < coords[3]; x++)
            for (int y = coords[1]; y < coords[4]; y++)
                for (int z = coords[2]; z < coords[5]; z++) {
                    NBTParser nbt = NBTParser.getTagCompound(anchor.getWorld().getBlockAt(x, y, z));
                    if (!nbt.getString("id").contains("structure_void")) {
                        nbt.setInt("x", nbt.getInt("x") - anchor.getBlockX());
                        nbt.setInt("y", nbt.getInt("y") - anchor.getBlockY());
                        nbt.setInt("z", nbt.getInt("z") - anchor.getBlockZ());
                        blocks.add(nbt + "");
                    }
                }
    }

    /**
     * Set blocks according to block list and anchor.
     */
    public void setBlocks() {
        blocks.forEach(nbt -> {
            NBTParser prsr = new NBTParser(nbt);
            prsr.setTagCompound(anchor.clone().add(prsr.getInt("x"), prsr.getInt("y"), prsr.getInt("z")).getBlock());
        });
    }
}
