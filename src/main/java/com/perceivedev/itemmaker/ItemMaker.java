package com.perceivedev.itemmaker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;

import com.perceivedev.perceivecore.language.I18N;
import com.perceivedev.perceivecore.language.MessageProvider.Category;

public class ItemMaker extends JavaPlugin {

    private static ItemMaker instance;

    private Logger           logger;
    private I18N             language;

    @Override
    public void onEnable() {
        logger = getLogger();

        getCommand("itemmaker").setExecutor(new ItemMakerCommand(this));

        Path savePath = getDataFolder().toPath();

        if (Files.notExists(savePath)) {
            try {
                Files.createDirectories(savePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        saveResource("Messages.properties", false);

        Locale locale = Locale.ENGLISH;

        language = new I18N(locale, "", savePath, getClassLoader(), new Category("Messages"));

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

}
