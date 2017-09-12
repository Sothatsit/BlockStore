package net.sothatsit.blockstore;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.logging.Logger;

public class BlockStoreConfig {

    private PreloadStrategy preloadStrategy = PreloadStrategy.CLOSE;
    private double unloadTime = 60;

    public PreloadStrategy getPreloadStrategy() {
        return preloadStrategy;
    }

    public double getUnloadTime() {
        return unloadTime;
    }

    public long getUnloadTimeMS() {
        return (long) (unloadTime * 1000);
    }

    public void reload() {
        getLogger().info("Reloading config...");

        BlockStore plugin = BlockStore.getInstance();

        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        FileConfiguration config = plugin.getConfig();

        this.preloadStrategy = loadPreloadStrategy(config);
        this.preloadStrategy.initialise();

        this.unloadTime = loadUnloadTime(config);

        getLogger().info("Config reloaded.");
    }

    private PreloadStrategy loadPreloadStrategy(ConfigurationSection config) {
        if(!config.isSet("preload")) {
            error("'preload' not set."
                    + " Should be set to 'all', 'close' or 'none'."
                    + " Defaulting to 'close'.");

            return PreloadStrategy.CLOSE;
        }

        if(!config.isString("preload")) {
            error("'preload' not text."
                    + " Should be set to 'all', 'close' or 'none'."
                    + " Defaulting to 'close'.");

            return PreloadStrategy.CLOSE;
        }

        String preload = config.getString("preload");
        PreloadStrategy strategy = PreloadStrategy.getStrategy(preload);

        if(strategy == null) {
            error("unknown preload value '" + preload + "'."
                    + " Should be set to 'all', 'close' or 'none'."
                    + " Defaulting to 'close'.");

            return PreloadStrategy.CLOSE;
        }

        info("Running using preload strategy: " + strategy);

        return strategy;
    }

    private double loadUnloadTime(ConfigurationSection config) {
        if(!config.isSet("unload-time")) {
            error("'unload-time' not set, should be a number in seconds."
                    + " Defaulting to 60 seconds.");

            return 60;
        }

        if(!config.isInt("unload-time") && !config.isDouble("unload-time")) {
            error("'unload-time' must be a number in seconds."
                    + " Defaulting to 60 seconds.");

            return 60;
        }

        double unloadTime = config.getDouble("unload-time");

        if(unloadTime <= 0) {
            error("'unload-time' must be greater than 0, it is a time in seconds."
                    + " Defaulting to 60 seconds.");

            return 60;
        }

        info("Unloading unused chunk stores after " + unloadTime + " seconds");

        return unloadTime;
    }

    private Logger getLogger() {
        return BlockStore.getInstance().getLogger();
    }

    private void info(String info) {
        getLogger().info("  " + info);
    }

    private void error(String error) {
        getLogger().severe("  Error in config.yml: " + error);
    }

}
