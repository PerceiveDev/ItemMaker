/**
 * 
 */
package com.perceivedev.itemmaker;

import org.bukkit.entity.Player;

/**
 * @author Rayzr
 *
 */
@FunctionalInterface
public interface Subcommand {

    public boolean onCommand(Player player, String[] args);

}
