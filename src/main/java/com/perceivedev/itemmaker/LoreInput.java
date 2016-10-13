/**
 * 
 */
package com.perceivedev.itemmaker;

import static com.perceivedev.perceivecore.util.TextUtils.colorize;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;

/**
 * @author Rayzr
 *
 */
public class LoreInput extends StringPrompt {

    private ItemMaker    plugin;

    private List<String> lore      = new ArrayList<String>();
    private String       lastInput = null;

    public LoreInput(ItemMaker plugin) {
        this.plugin = plugin;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.bukkit.conversations.Prompt#getPromptText(org.bukkit.conversations.
     * ConversationContext)
     */
    @Override
    public String getPromptText(ConversationContext context) {
        return lastInput == null ? plugin.tr("prompt.first") : plugin.tr("prompt.lore", lastInput);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.bukkit.conversations.Prompt#acceptInput(org.bukkit.conversations.
     * ConversationContext, java.lang.String)
     */
    @Override
    public Prompt acceptInput(ConversationContext context, String input) {

        if (input.equalsIgnoreCase("done")) {
            context.setSessionData("lore", lore);
            return null;
        } else {
            lastInput = colorize(input.trim());
            if (lastInput.equals("\\n")) {
                lore.add("");
                lastInput = plugin.tr("prompt.lore.empty");
            } else {
                lore.add(ChatColor.RESET + lastInput);
            }
        }

        return this;
    }

}
