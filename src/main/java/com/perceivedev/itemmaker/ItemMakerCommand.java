/**
 * 
 */
package com.perceivedev.itemmaker;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.perceivedev.perceivecore.nbt.ItemNBTUtil;
import com.perceivedev.perceivecore.nbt.NBTWrappers.NBTTagByte;
import com.perceivedev.perceivecore.nbt.NBTWrappers.NBTTagCompound;
import com.perceivedev.perceivecore.nbt.NBTWrappers.NBTTagList;
import com.perceivedev.perceivecore.util.ArrayUtils;
import com.perceivedev.perceivecore.util.ItemUtils;
import com.perceivedev.perceivecore.util.TextUtils;

/**
 * @author Rayzr
 *
 */
public class ItemMakerCommand implements CommandExecutor {

    private HashMap<String, Subcommand> subCommands = new HashMap<>();

    private ItemMaker                   plugin;

    private Random                      rand        = new Random();

    /**
     * @param plugin the plugin this command is associated with
     */
    public ItemMakerCommand(ItemMaker plugin) {
        this.plugin = plugin;
        subCommands.put("help", this::listCommands);
        subCommands.put("name", this::setName);
        subCommands.put("attribute", this::setAttribute);
        subCommands.put("unbreakable", this::setUnbreakable);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.
     * CommandSender, org.bukkit.command.Command, java.lang.String,
     * java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You have to be a player to use this command!");
            return true;
        }

        Player p = (Player) sender;

        if (args.length < 1) {
            showHelp(p);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (!subCommands.containsKey(sub)) {
            listCommands(p, new String[0]);
            return true;
        }

        Subcommand cmd = subCommands.get(sub);
        if (!cmd.onCommand(p, Arrays.copyOfRange(args, 1, args.length))) {
            showUsage(p, sub);
            return true;
        }

        return true;

    }

    /**
     * @param player who to show the help too
     */
    private void showHelp(Player player) {
        msg(player, "&7You are running &bItemMaker v1.0", "&7Do &b/itemmaker help &7to see all available subcommands");
    }

    /***
     * @param sub the sub-command to show
     */
    private void showUsage(Player player, String sub) {
        msg(player, plugin.getLanguage().tr("subcmd usage for " + sub));
    }

    private void msg(Player player, String... msgs) {
        Arrays.stream(msgs).forEach(msg -> player.sendMessage(TextUtils.colorize(msg)));
    }

    public boolean listCommands(Player player, String[] args) {
        subCommands.keySet().stream().forEach(label -> showUsage(player, label));
        return true;
    }

    /**
     * Parses a string for various forms of "yes", including: yes, y, true, 1,
     * and 1b. The last one is due to the format of NBT for those who are used
     * to using the built in Minecraft commands and setting NBT values.
     * 
     * @param input the user input
     * @return Whether or not the input represents "yes". Basically, the input
     *         in boolean form.
     */
    public boolean parseInput(String input) {
        input = input.toLowerCase();
        return input.equals("yes") || input.equals("y") || input.equals("true") || input.equals("1") || input.equals("1b");
    }

    private ItemStack getHand(Player player) {
        return player.getInventory().getItemInMainHand();
    }

    private boolean checkItem(ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }

    // ...////---------------------------------------//
    // ...///---------- BEGIN SUB COMMANDS ---------///
    // ...//---------------------------------------////

    // Set item name
    public boolean setName(Player player, String[] args) {

        if (args.length < 1) {
            msg(player, "&7You need to provide a name!");
            return false;
        }

        ItemStack item = getHand(player);
        if (!checkItem(item)) {
            msg(player, "&7You need to be holding an item!");
            return false;
        }

        String name = TextUtils.colorize("&r" + ArrayUtils.concat(args, " "));
        ItemUtils.setName(item, name);

        msg(player, "&7Item name set to \"&r" + name + "&7\"");

        return true;

    }

    // Set item unbreakable
    public boolean setUnbreakable(Player player, String[] args) {

        if (args.length < 1) {
            msg(player, "&7You need to specify whether or not to make it &bunbreakable&7!");
            return false;
        }

        ItemStack item = getHand(player);
        if (!checkItem(item)) {
            msg(player, "&7You need to be holding an item!");
            return false;
        }

        NBTTagCompound tag = ItemNBTUtil.getTag(item);

        boolean input = parseInput(args[0]);

        if (input) {
            System.out.println("Set unbreakable to true");
            tag.setBoolean("Unbreakable", input);
        } else if (tag.hasKeyOfType("Unbreakable", NBTTagByte.class)) {
            System.out.println("Removed unbreakable");
            tag.remove("Unbreakable");
        }

        item = ItemNBTUtil.setNBTTag(tag, item);

        player.getInventory().setItemInMainHand(item);

        msg(player, "&7Unbreakable set to &b" + input);

        return true;

    }

    // Set item attributes
    public boolean setAttribute(Player player, String[] args) {

        if (args.length < 3) {
            msg(player, "&7You need to provide an &battribute name&7, an &boperation (0, 1 or 2)&7 and a &bvalue&7!");
            return false;
        }

        String slot = null;

        if (args.length > 3) {
            slot = args[3];
        }

        ItemStack item = getHand(player);
        if (!checkItem(item)) {
            msg(player, "&7You need to be holding an item!");
            return false;
        }

        String attributeName = args[0];
        if (attributeName.indexOf(".") < 0) {
            attributeName = "generic." + attributeName;
        }

        int operation = 0;
        try {
            operation = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            operation = -1;
        }

        if (operation < 0 || operation > 2) {
            msg(player, "&7Please enter a valid number for the operation! (0, 1 or 2)");
            return false;
        }

        int amount = -1;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            msg(player, "&7Please enter a valid number for the amount!");
            return false;
        }

        NBTTagCompound tag = ItemNBTUtil.getTag(item);

        NBTTagList modifierList = new NBTTagList();

        if (tag.hasKey("AttributeModifiers")) {
            modifierList = (NBTTagList) tag.get("AttributeModifiers");
        }

        NBTTagCompound attr = new NBTTagCompound();
        attr.setString("AttributeName", attributeName);
        attr.setString("Name", attributeName);
        attr.setInt("Amount", amount);
        attr.setInt("Operation", 0);
        attr.setInt("UUIDMost", rand.nextInt(999999999));
        attr.setInt("UUIDLeast", rand.nextInt(999999999));
        if (slot != null) {
            attr.setString("Slot", slot);
        }

        modifierList.add(attr);

        tag.set("AttributeModifiers", modifierList);

        System.out.println(tag.toString());

        item = ItemNBTUtil.setNBTTag(tag, item);

        player.getInventory().setItemInMainHand(item);

        return true;

    }

    /**
     * A reference to Bash :P
     * 
     * @param args
     * @return
     */
    public String[] shift(String... args) {
        return (args.length < 1) ? args : Arrays.copyOfRange(args, 1, args.length);
    }

}
