package com.perceivedev.itemmaker;

import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;

public class ItemMaker extends JavaPlugin {

    private Logger logger;

    @Override
    public void onEnable() {
        logger = getLogger();

        getCommand("itemmaker").setExecutor(new ItemMakerCommand(this));

        logger.info(versionText() + " enabled");
    }

    @Override
    public void onDisable() {
        logger.info(versionText() + " disabled");
    }

    public String versionText() {
        return getName() + " v" + getDescription().getVersion();
    }

}
