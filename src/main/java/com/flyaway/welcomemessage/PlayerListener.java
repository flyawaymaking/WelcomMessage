package com.flyaway.welcomemessage;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class PlayerListener implements Listener {
    private final MessageManager messageManager;
    private final ConfigManager configManager;

    public PlayerListener(MessageManager messageManager, ConfigManager configManager) {
        this.messageManager = messageManager;
        this.configManager = configManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Отключаем стандартное сообщение
        event.joinMessage(null);

        // Проверяем, нужно ли отображать сообщение о входе
        if (messageManager.shouldSilentJoin(player)) {
            return;
        }

        // Получаем префикс и суффикс игрока
        String playerPrefix = messageManager.getPlayerPrefix(player);
        String playerSuffix = messageManager.getPlayerSuffix(player);

        Component message;

        if (!player.hasPlayedBefore()) {
            // Первый вход
            String rawMessage = configManager.getFirstTimeMessage(player.getName());
            message = messageManager.createFormattedMessage(player, playerPrefix, playerSuffix, rawMessage, true);
        } else {
            // Обычный вход
            String rawMessage = configManager.getWelcomeMessage(player.getName());
            message = messageManager.createFormattedMessage(player, playerPrefix, playerSuffix, rawMessage, false);
        }

        // Отправляем сообщение всем игрокам
        if (message != null) {
            Bukkit.broadcast(message);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Отключаем стандартное сообщение
        event.quitMessage(null);

        // Проверяем, нужно ли отображать сообщение о выходе
        if (messageManager.shouldSilentJoin(player)) {
            return;
        }

        // Получаем префикс и суффикс игрока
        String playerPrefix = messageManager.getPlayerPrefix(player);
        String playerSuffix = messageManager.getPlayerSuffix(player);

        // Создаем сообщение о выходе
        String rawMessage = configManager.getQuitMessage(player.getName());
        Component message = messageManager.createFormattedMessage(player, playerPrefix, playerSuffix, rawMessage, false);

        // Отправляем сообщение всем игрокам
        if (message != null) {
            Bukkit.broadcast(message);
        }
    }
}
