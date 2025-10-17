package com.flyaway.welcomemessage;

import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    private final WelcomeMessage plugin;
    private FileConfiguration config;

    // Сообщения из конфига
    private String welcomeMessage;
    private String firstTimeMessage;
    private String quitMessage;

    public ConfigManager(WelcomeMessage plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        loadMessages();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        loadMessages();
    }

    private void loadMessages() {
        welcomeMessage = config.getString("welcome-message", "&fзашёл на сервер!");
        firstTimeMessage = config.getString("first-time-message", "&fзашёл на сервер впервые!");
        quitMessage = config.getString("quit-message", "&fвышел с сервера.");
    }

    // Методы для получения сообщений (без имени игрока, так как оно будет добавляться отдельно)
    public String getWelcomeMessage(String playerName) {
        return welcomeMessage;
    }

    public String getFirstTimeMessage(String playerName) {
        return firstTimeMessage;
    }

    public String getQuitMessage(String playerName) {
        return quitMessage;
    }
}
