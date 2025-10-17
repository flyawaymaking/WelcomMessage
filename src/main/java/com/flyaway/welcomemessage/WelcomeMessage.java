package com.flyaway.welcomemessage;

import org.bukkit.plugin.java.JavaPlugin;

public class WelcomeMessage extends JavaPlugin {
    private MessageManager messageManager;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        // Инициализируем менеджеры
        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(this);

        // Регистрируем события
        getServer().getPluginManager().registerEvents(new PlayerListener(messageManager, configManager), this);

        // Команда для перезагрузки конфига
        getCommand("welcomemessage").setExecutor(new ReloadCommand(configManager));

        getLogger().info("WelcomeMessage включён!");
    }

    @Override
    public void onDisable() {
        getLogger().info("WelcomeMessage выключен!");
    }
}
