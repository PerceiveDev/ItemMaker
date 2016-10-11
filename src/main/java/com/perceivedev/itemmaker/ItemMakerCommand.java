/**
 * 
 */
package com.perceivedev.itemmaker;

import java.util.Arrays;
import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.perceivedev.perceivecore.util.TextUtils;

/**
 * @author Rayzr
 *
 */
public class ItemMakerCommand implements CommandExecutor {

    private HashMap<String, Subcommand> subCommands = new HashMap<>();

    @SuppressWarnings("unused")
    private ItemMaker                   plugin;

    /**
     * @param plugin the plugin this command is associated with
     */
    public ItemMakerCommand(ItemMaker plugin) {
        this.plugin = plugin;
        subCommands.put("name", this::setName);
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
            showUsage(sub);
        }

        return true;
    }

    /**
     * @param player who to show the help too
     */
    private void showHelp(Player player) {
        msg(player, "&7You are running &bItemMaker v1.0", "&7Do &b/help &7to see all available subcommands");
    }

    /***
     * @param sub the sub-command to show
     */
    private void showUsage(String sub) {
        
    }

    private void msg(Player player, String... msgs) {
        Arrays.stream(msgs).forEach(msg -> player.sendMessage(TextUtils.colorize(msg)));
    }

    public boolean setName(Player player, String[] args) {
        return true;
    }

    public boolean listCommands(Player player, String[] args) {
        // TODO: List all sub-commands
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

}
