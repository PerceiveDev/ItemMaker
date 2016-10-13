/**
 * 
 */
package com.perceivedev.itemmaker;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
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

    private ConversationFactory         cf;

    /**
     * @param plugin the plugin this command is associated with
     */
    public ItemMakerCommand(ItemMaker plugin) {
        this.plugin = plugin;
        cf = new ConversationFactory(plugin)
                .thatExcludesNonPlayersWithMessage(ChatColor.RED + "Only players can do this!")
                .withPrefix(ctx -> TextUtils.colorize(plugin.tr("prefix") + " "))
                .withModality(true)
                .withLocalEcho(false)
                .addConversationAbandonedListener(this::finishLore);

        subCommands.put("help", this::listCommands);
        subCommands.put("name", this::setName);
        subCommands.put("lore", this::setLore);
        subCommands.put("attribute", this::setAttribute);
        subCommands.put("removeattr", this::removeAttribute);
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
        msg(player, plugin.tr("subcmd usage for " + sub));
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

    /**
     * A reference to Bash :P
     * 
     * @param args
     * @return
     */
    public String[] shift(String... args) {
        return (args.length < 1) ? args : Arrays.copyOfRange(args, 1, args.length);
    }

    // ...////---------------------------------------//
    // ...///---------- BEGIN SUB COMMANDS ---------///
    // ...//---------------------------------------////

    // Set item name
    public boolean setName(Player player, String[] args) {

        if (args.length < 1) {
            msg(player, plugin.tr("missing args"));
            return false;
        }

        ItemStack item = getHand(player);
        if (!checkItem(item)) {
            msg(player, plugin.tr("no item"));
            return false;
        }

        String name = TextUtils.colorize("&r" + ArrayUtils.concat(args, " "));
        ItemUtils.setName(item, name);

        msg(player, plugin.tr("name set", name));

        return true;

    }

    // Set item unbreakable
    public boolean setUnbreakable(Player player, String[] args) {

        if (args.length < 1) {
            msg(player, plugin.tr("missing args"));
            return false;
        }

        ItemStack item = getHand(player);
        if (!checkItem(item)) {
            msg(player, plugin.tr("no item"));
            return false;
        }

        NBTTagCompound tag = ItemNBTUtil.getTag(item);

        boolean input = parseInput(args[0]);

        if (input) {
            tag.setBoolean("Unbreakable", input);
        } else if (tag.hasKeyOfType("Unbreakable", NBTTagByte.class)) {
            tag.remove("Unbreakable");
        }

        item = ItemNBTUtil.setNBTTag(tag, item);

        player.getInventory().setItemInMainHand(item);

        msg(player, plugin.tr("value set", "Unbreakable", input));

        return true;

    }

    // Set item attributes
    public boolean setAttribute(Player player, String[] args) {

        if (args.length < 3) {
            msg(player, plugin.tr("missing args"));
            return false;
        }

        String slot = null;

        if (args.length > 3) {
            slot = args[3];
        }

        ItemStack item = getHand(player);
        if (!checkItem(item)) {
            msg(player, plugin.tr("no item"));
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

        item = ItemNBTUtil.setNBTTag(tag, item);

        player.getInventory().setItemInMainHand(item);

        return true;

    }

    // Remove item attributes
    public boolean removeAttribute(Player player, String[] args) {

        if (args.length < 1) {
            msg(player, "&7You need to provide an &battribute name&7!");
            return false;
        }

        ItemStack item = getHand(player);
        if (!checkItem(item)) {
            msg(player, plugin.tr("no item"));
            return false;
        }

        String attributeName = args[0];
        if (attributeName.indexOf(".") < 0) {
            attributeName = "generic." + attributeName;
        }

        NBTTagCompound tag = ItemNBTUtil.getTag(item);

        NBTTagList modifierList;

        if (!tag.hasKey("AttributeModifiers")) {
            msg(player, "&7This item has no attributes!");
            return false;
        }

        modifierList = (NBTTagList) tag.get("AttributeModifiers");

        // TODO: Waiting for @i_al_istannen....
        // modifierList.remove(some stuff)

        tag.set("AttributeModifiers", modifierList);

        item = ItemNBTUtil.setNBTTag(tag, item);

        player.getInventory().setItemInMainHand(item);

        return true;

    }

    public boolean setLore(Player player, String[] args) {

        ItemStack item = getHand(player);
        if (!checkItem(item)) {
            msg(player, plugin.tr("no item"));
            return false;
        }

        Conversation conv = cf.withFirstPrompt(new LoreInput()).buildConversation(player);
        conv.getContext().setSessionData("item", item);
        conv.begin();

        return true;

    }

    @SuppressWarnings("unchecked")
    public void finishLore(ConversationAbandonedEvent e) {

        if (!e.gracefulExit()) {
            // Something went wrong :P
            return;
        }

        ConversationContext ctx = e.getContext();
        if (!(ctx.getForWhom() instanceof Player)) {
            // What the heck? They should be a player...
            return;
        }

        Player p = (Player) ctx.getForWhom();

        if (!getHand(p).equals(ctx.getSessionData("item"))) {
            msg(p, "&7You must be holding the same item as you started with!");
            return;
        }

        p.getInventory().setItemInMainHand(ItemUtils.setLore(getHand(p), (List<String>) ctx.getSessionData("lore")));
        msg(p, "&7Lore set");

    }

}
