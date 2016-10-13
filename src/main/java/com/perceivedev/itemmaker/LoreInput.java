/**
 * 
 */
package com.perceivedev.itemmaker;

import static com.perceivedev.perceivecore.util.TextUtils.colorize;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;

import com.perceivedev.perceivecore.util.TextUtils;

/**
 * @author Rayzr
 *
 */
public class LoreInput extends StringPrompt {

    private final String promptFirst = "&7Enter the lines of lore that you want. Use &b\\n &7for a blank line, and &bDONE &7when you have finished.";
    private final String promptLore  = "&7Lore added: &r";

    private List<String> lore        = new ArrayList<String>();
    private String       lastInput   = null;

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.bukkit.conversations.Prompt#getPromptText(org.bukkit.conversations.
     * ConversationContext)
     */
    @Override
    public String getPromptText(ConversationContext context) {
        return lastInput == null ? colorize(promptFirst) : colorize(promptLore + lastInput);
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
            lastInput = TextUtils.colorize(input);
            lore.add(lastInput);
        }

        return this;
    }

}
