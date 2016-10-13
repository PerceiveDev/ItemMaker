/**
 * 
 */
package com.perceivedev.itemmaker;

import static com.perceivedev.perceivecore.util.ArrayUtils.concat;
import static com.perceivedev.perceivecore.util.TextUtils.colorize;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.perceivedev.perceivecore.nbt.ItemNBTUtil;
import com.perceivedev.perceivecore.nbt.NBTWrappers.NBTTagByte;
import com.perceivedev.perceivecore.nbt.NBTWrappers.NBTTagCompound;
import com.perceivedev.perceivecore.nbt.NBTWrappers.NBTTagList;
import com.perceivedev.perceivecore.nbt.NBTWrappers.NBTTagString;
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
        // @formatter:off
        cf = new ConversationFactory(plugin)
                .thatExcludesNonPlayersWithMessage(plugin.tr("only.players"))
                .withPrefix(ctx -> plugin.tr("prefix") + " ")
                .withModality(true)
                .withLocalEcho(false)
                .addConversationAbandonedListener(this::finishLore);
        // @formatter:on

        subCommands.put("help", this::listCommands);
        subCommands.put("name", this::setName);
        subCommands.put("lore", this::setLore);
        subCommands.put("attribute", this::setAttribute);
        subCommands.put("removeattr", this::removeAttribute);
        subCommands.put("unbreakable", this::setUnbreakable);
        subCommands.put("flags", this::hideFlags);
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
            sender.sendMessage(plugin.tr("only.players"));
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
        msg(player, plugin.tr("version.info", plugin.versionText()), plugin.tr("do.help"));
    }

    /***
     * @param sub the sub-command to show
     */
    private void showUsage(Player player, String sub) {
        msg(player, plugin.tr("subcmd.usage.for." + sub));
    }

    private void msg(Player player, String... msgs) {
        Arrays.stream(msgs).forEach(msg -> player.sendMessage(colorize(msg)));
    }

    public boolean listCommands(Player player, String[] args) {
        subCommands.keySet().stream().sorted().forEach(label -> showUsage(player, label));
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

    ////////////////////////////////////////
    // ---------------------------------- //
    // ------- BEGIN SUB COMMANDS ------- //
    // ---------------------------------- //
    ////////////////////////////////////////

    // ---------------------------------- //
    // -------- Set Name command -------- //
    // ---------------------------------- //
    public boolean setName(Player player, String[] args) {

        if (args.length < 1) {
            msg(player, plugin.tr("missing.args"));
            return false;
        }

        ItemStack item = getHand(player);
        if (!checkItem(item)) {
            msg(player, plugin.tr("no.item"));
            return false;
        }

        String name = colorize("&r" + concat(args, " "));
        ItemUtils.setName(item, name);

        msg(player, plugin.tr("name.set", name));

        return true;

    }

    // ---------------------------------- //
    // --- Toggle Unbreakable command --- //
    // ---------------------------------- //
    public boolean setUnbreakable(Player player, String[] args) {

        if (args.length < 1) {
            msg(player, plugin.tr("missing.args"));
            return false;
        }

        ItemStack item = getHand(player);
        if (!checkItem(item)) {
            msg(player, plugin.tr("no.item"));
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

        msg(player, plugin.tr("value.set", "Unbreakable", input));

        return true;

    }

    // ---------------------------------- //
    // ----- Add Attributes command ----- //
    // ---------------------------------- //
    public boolean setAttribute(Player player, String[] args) {

        if (args.length < 3) {
            msg(player, plugin.tr("missing.args"));
            return false;
        }

        String slot = null;

        if (args.length > 3) {
            slot = args[3];
        }

        ItemStack item = getHand(player);
        if (!checkItem(item)) {
            msg(player, plugin.tr("no.item"));
            return false;
        }

        String attributeName = args[0].indexOf(".") < 0 ? "generic." + args[0] : args[0];

        int operation = 0;
        try {
            operation = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            operation = -1;
        }

        if (operation < 0 || operation > 2) {
            msg(player, plugin.tr("invalid.param", "operation", "number (0, 1 or 2)"));
            return false;
        }

        int amount = -1;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            msg(player, plugin.tr("invalid.param", "amount", "number"));
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

        msg(player, plugin.tr("attribute.set", attributeName, amount, operation));

        return true;

    }

    // ---------------------------------- //
    // --- Remove Attributes commands --- //
    // ---------------------------------- //
    public boolean removeAttribute(Player player, String[] args) {

        if (args.length < 1) {
            msg(player, plugin.tr("missing.args"));
            return false;
        }

        ItemStack item = getHand(player);
        if (!checkItem(item)) {
            msg(player, plugin.tr("no.item"));
            return false;
        }

        String attributeName = args[0].indexOf(".") < 0 ? "generic." + args[0] : args[0];

        NBTTagCompound tag = ItemNBTUtil.getTag(item);

        NBTTagList modifierList;

        if (!tag.hasKey("AttributeModifiers")) {
            msg(player, plugin.tr("no.attributes"));
            return false;
        }

        modifierList = (NBTTagList) tag.get("AttributeModifiers");

        // @i_al_istannen: I found a way to deal with the stupidity of the
        // differences between our formatter profiles! Yay!
        // @formatter:off
        Optional<NBTTagCompound> tag2 = modifierList
                .getList()
                .stream()
                .filter(o -> o instanceof NBTTagCompound)
                .map(o -> (NBTTagCompound) o)
                .filter(o -> o.hasKeyOfType("AttributeName", NBTTagString.class) && o.getString("AttributeName").equals(attributeName))
                .findFirst();
        // @formatter:on

        if (!tag2.isPresent()) {
            msg(player, plugin.tr("attribute.not.present", attributeName));
            return false;
        }

        if (!modifierList.remove(tag2.get())) {
            msg(player, plugin.tr("error", "ItemMakerCommand#removeAttribute failed to remove tag from modifierList!"));
            return false;
        }

        tag.set("AttributeModifiers", modifierList);

        item = ItemNBTUtil.setNBTTag(tag, item);

        player.getInventory().setItemInMainHand(item);

        msg(player, plugin.tr("attribute.removed", attributeName));

        return true;

    }

    // ---------------------------------- //
    // ---------- Lore command ---------- //
    // ---------------------------------- //
    // I know this method is really ugly, //
    // but I don't know any better way to //
    // implement setting, changing, *and* //
    // removing in one command. YAY!! @w@ //
    // ---------------------------------- //
    // After writing the above note I did //
    // end up adding `add` functionality, //
    // so now it is even more complicated //
    // than it was. Why do I do this?!?!? //
    // ---------------------------------- //
    public boolean setLore(Player player, String[] args) {

        ItemStack item = getHand(player);
        if (!checkItem(item)) {
            msg(player, plugin.tr("no.item"));
            return false;
        }

        if (args.length >= 2) {
            if (args[0].equalsIgnoreCase("add")) {
                args = shift(args);
                String line = colorize(concat(args, " "));
                if (ItemUtils.getLore(item) == null) {
                    ItemUtils.setLore(item, Arrays.asList(ChatColor.RESET + line));
                } else {
                    ItemUtils.addLore(item, Arrays.asList(ChatColor.RESET + line));
                }
                msg(player, plugin.tr("lore.line.added", line));
                return true;
            }
            int lineNum = -1;
            try {
                lineNum = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                lineNum = -1;
            }

            if (lineNum < 1) {
                msg(player, plugin.tr("invalid.param", "operation", "number >0"));
                return false;
            }

            ItemMeta im = item.getItemMeta();
            if (!im.hasLore()) {
                msg(player, plugin.tr("no.lore"));
                return true;
            }

            List<String> lore = im.getLore();
            if (lineNum > lore.size()) {
                msg(player, plugin.tr("no.lore.line", lineNum, lore.size()));
                return false;
            }

            args = shift(args);

            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("remove")) {
                    lore.remove(lineNum - 1);
                    msg(player, plugin.tr("lore.line.removed", lineNum));
                    return true;
                }
            }

            String line = colorize(concat(args, " ").trim());

            if (lore.equals("\\n")) {
                lore.set(lineNum - 1, "");
            } else {
                lore.set(lineNum - 1, ChatColor.RESET + line);
                line = plugin.tr("prompt.lore.empty");
            }

            im.setLore(lore);
            item.setItemMeta(im);
            msg(player, plugin.tr("lore.line.set", lineNum, line));
        } else {
            Conversation conv = cf.withFirstPrompt(new LoreInput(plugin)).buildConversation(player);
            conv.getContext().setSessionData("item", item);
            conv.begin();
        }

        return true;

    }

    public boolean hideFlags(Player player, String... args) {

        if (args.length < 2) {
            if (args.length > 0 && args[0].equalsIgnoreCase("list")) {
                msg(player, plugin.tr("flags.list", Arrays.stream(ItemFlag.values()).map(flag -> flag.toString()).sorted().collect(Collectors.joining(", "))));
                return true;
            }
            msg(player, plugin.tr("missing.args"));
            return false;
        }

        ItemStack item = getHand(player);
        if (!checkItem(item)) {
            msg(player, plugin.tr("no.item"));
            return false;
        }
        ItemMeta im = item.getItemMeta();

        String arg = args[0].toLowerCase();
        args = shift(args);

        List<ItemFlag> flags;
        if (args.length == 1 && args[0].equalsIgnoreCase("all")) {
            flags = Arrays.asList(ItemFlag.values());
        } else {
            flags = Arrays.asList(args).stream().filter(str -> {
                try {
                    return ItemFlag.valueOf(TextUtils.enumFormat(str)) != null;
                } catch (Exception e) {
                    return false;
                }
            }).map(str -> ItemFlag.valueOf(TextUtils.enumFormat(str))).collect(Collectors.toList());
        }

        String action = "";
        if (arg.equals("add")) {
            action = "added";
            ItemFlag[] actual = flags.stream().filter(flag -> im.hasItemFlag(flag)).collect(Collectors.toList()).toArray(new ItemFlag[0]);
            System.out.println("Adding, actual.length = " + actual.length);
            im.addItemFlags(actual);
        } else if (arg.equals("remove")) {
            action = "removed";
            ItemFlag[] actual = flags.stream().filter(flag -> im.hasItemFlag(flag)).collect(Collectors.toList()).toArray(new ItemFlag[0]);
            System.out.println("Adding, actual.length = " + actual.length);
            im.removeItemFlags(actual);
        } else {
            return false;
        }

        msg(player, plugin.tr("flags", action, flags.stream().map(flag -> flag.toString()).sorted().collect(Collectors.joining(", "))));

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
            msg(p, plugin.tr("needs.same.item"));
            return;
        }

        p.getInventory().setItemInMainHand(ItemUtils.setLore(getHand(p), (List<String>) ctx.getSessionData("lore")));
        msg(p, plugin.tr("lore.set"));

    }

}