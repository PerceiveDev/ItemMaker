package com.perceivedev.itemmaker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;

import com.perceivedev.perceivecore.language.I18N;

public class ItemMaker extends JavaPlugin {

    private static ItemMaker instance;

    private Logger           logger;
    private I18N             language;

    @Override
    public void onEnable() {
        logger = getLogger();

        Path output = getDataFolder().toPath().resolve("language");

        if (Files.notExists(output)) {
            try {
                Files.createDirectories(output);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        I18N.copyDefaultFiles("language", output, false, getFile());

        language = new I18N(this, "language");

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

    /**
     * @return the I18N language instance
     */
    public I18N getLanguage() {
        return language;
    }

    /**
     * @return the instance
     */
    public static ItemMaker getInstance() {
        return instance;
    }

    /**
     * @param string
     * @return
     */
    public String tr(String key, Object... formattingObjects) {
        return language.tr(key, formattingObjects);
    }

}
