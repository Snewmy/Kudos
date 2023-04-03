package de.urbance;

import Commands.Kudmin;
import Commands.Kudo;
import Commands.Kudos;
import Events.OnPlayerJoin;
import Utils.*;
import Utils.SQL.SQL;
import Utils.SQL.SQLGetter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.sql.SQLException;

public final class Main extends JavaPlugin implements Listener {
    public final CooldownManager cooldownManager = new CooldownManager();
    public String prefix;
    public FileConfiguration localeConfig;
    public Utils.SQL.SQL SQL;
    public SQLGetter data;
    public FileConfiguration config;
    public FileConfiguration mysqlConfig;
    public FileConfiguration guiConfig;
    public boolean isConnected;

    @Override
    public void onEnable() {
        this.config = getConfig();
        this.prefix = config.getString("general.prefix");

        getLogger().info("Successfully launched. Suggestions? Questions? Report a Bug? Visit my discord server! https://discord.gg/hDqPms3MbH");

        setupSQL();
        setupConfigs();
        updateChecker();
        registerListenerAndCommands();
        useSQL();
        setupMetrics();
    }

    @Override
    public void onDisable() {
        SQL.disconnect();
    }

    public void useSQL() {
        if (!config.getBoolean("general.use-SQL")) {
            getLogger().warning("In the config.yml \"useSQL\" is set to false. Currently there is only the possibility to store data via MySQL. Value is set back to true.");
            config.set("general.use-SQL", true);
            saveConfig();
        }
    }
    public void registerListenerAndCommands() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new OnPlayerJoin(), this);
        if (!isConnected) {
            return;
        }
        pluginManager.registerEvents(new GUI(), this);
        getCommand("kudos").setExecutor(new Kudos());
        getCommand("kudos").setTabCompleter(new Kudos());
        getCommand("kudo").setExecutor(new Kudo());
        getCommand("kudo").setTabCompleter(new Kudo());
        getCommand("kudmin").setExecutor(new Kudmin());
        getCommand("kudmin").setTabCompleter(new Kudmin());
        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new KudosExpansion().register();
        }
    }

    public void setupSQL(){
        this.SQL = new SQL();
        this.data = new SQLGetter(this);

        try {
            SQL.connect();
        } catch (ClassNotFoundException | SQLException e) {
            getLogger().info("Database not connected");
            this.isConnected = false;
            if (config.getBoolean("general.debug-mode"))
                e.printStackTrace();
        }

        if (SQL.isConnected()) {
            getLogger().info("Database is connected");
            data.createTable();
            this.isConnected = true;
        }
        if (isConnected)
            keepAliveDatabaseConnection();
    }

    public void setupConfigs() {
        // setup config.yml
        getConfig().options().copyDefaults(true);
        saveConfig();
        this.config = getConfig();

        // setup mysql.yml
        FileManager mysqlManager = new FileManager("mysql.yml", this);
        this.mysqlConfig = mysqlManager.getConfig();
        mysqlConfig.options().copyDefaults(true);
        mysqlManager.save();

        // setup messages.yml
        FileManager localeManager = new FileManager("messages.yml", this);
        this.localeConfig = localeManager.getConfig();
        localeConfig.options().copyDefaults(true);
        localeManager.save();

        // setup gui.yml
        FileManager guiManager = new FileManager("gui.yml", this);
        this.guiConfig = guiManager.getConfig();
        guiConfig.options().copyDefaults(true);
        guiManager.save();
    }

    private void setupMetrics() {
        new Metrics(this, 16627);
    }

    public void updateChecker() {
        if (!config.getBoolean("general.update-notification")){
            return;
        }
        new UpdateChecker(this, 106036).getVersion(version -> {
            if (this.getDescription().getVersion().equals(version)) {
                getLogger().info("There is not a new update available.");
            } else {
                getLogger().info("There is a new update available.");
            }
        });
    }

    /*
    Added on v1.7.3.
    Fixed connection timeout from database temporarily.
    It's planned to replace it with DataSources and HikariCP.
     */
    private void keepAliveDatabaseConnection() {
        BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(this, () -> data.keepAlive(), 0L, 1200);
    }
}
