package com.flyaway.welcomemessage;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.cacheddata.CachedMetaData;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class MessageManager {
    private final WelcomeMessage plugin;
    private LuckPerms luckPerms;

    // Паттерны для обработки форматирования
    private final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient:(#[A-Fa-f0-9]{6}):(#[A-Fa-f0-9]{6})>(.*?)</gradient>");
    private final Pattern HEX_PATTERN = Pattern.compile("<#([A-Fa-f0-9]{6})>");
    private final LegacyComponentSerializer textSerializer = LegacyComponentSerializer.legacySection();

    public MessageManager(WelcomeMessage plugin) {
        this.plugin = plugin;
        setupLuckPerms();
    }

    private void setupLuckPerms() {
        try {
            this.luckPerms = LuckPermsProvider.get();
            plugin.getLogger().info("LuckPerms найден, префиксы будут загружаться из него");
        } catch (IllegalStateException e) {
            plugin.getLogger().warning("LuckPerms не найден, будут использоваться стандартные префиксы");
            this.luckPerms = null;
        }
    }

    // Проверка на скрытый вход
    public boolean shouldSilentJoin(Player player) {
        return player.hasPermission("essentials.silentjoin");
    }

    public String getPlayerPrefix(Player player) {
        if (luckPerms == null) {
            return "";
        }

        try {
            User user = luckPerms.getPlayerAdapter(Player.class).getUser(player);
            CachedMetaData metaData = user.getCachedData().getMetaData();
            String prefix = metaData.getPrefix();
            return prefix != null ? prefix : "";

        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось получить префикс для игрока " + player.getName() + ": " + e.getMessage());
            return "";
        }
    }

    public String getPlayerSuffix(Player player) {
        if (luckPerms == null) {
            return "";
        }

        try {
            User user = luckPerms.getPlayerAdapter(Player.class).getUser(player);
            CachedMetaData metaData = user.getCachedData().getMetaData();
            String suffix = metaData.getSuffix();
            return suffix != null ? suffix : "";

        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось получить суффикс для игрока " + player.getName() + ": " + e.getMessage());
            return "";
        }
    }

    public Component createFormattedMessage(Player player, String prefix, String suffix, String rawMessage, boolean isFirstJoin) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            return Component.empty();
        }

        // Получаем формат сообщения из конфига
        String messageFormat = plugin.getConfig().getString("message-format", "%prefix%%player%%suffix% %message%");

        // Заменяем плейсхолдеры
        String formattedMessage = messageFormat
                .replace("%prefix%", prefix)
                .replace("%player%", player.getName())
                .replace("%suffix%", suffix)
                .replace("%message%", rawMessage);

        // Обрабатываем всё сообщение через форматирование
        return processAdvancedFormatting(formattedMessage);
    }

    // Улучшенная обработка форматирования
    private Component processGradients(String text) {
        Matcher gradientMatcher = GRADIENT_PATTERN.matcher(text);

        if (!gradientMatcher.find()) {
            // Если градиентов нет, возвращаем обычное форматирование
            return processBasicFormatting(text);
        }

        gradientMatcher.reset();

        Component result = Component.empty();
        int lastEnd = 0;

        while (gradientMatcher.find()) {
            // Добавляем текст до градиента
            if (gradientMatcher.start() > lastEnd) {
                String before = text.substring(lastEnd, gradientMatcher.start());
                result = result.append(processBasicFormatting(before));
            }

            // Обрабатываем градиент
            String startHex = gradientMatcher.group(1);
            String endHex = gradientMatcher.group(2);
            String content = gradientMatcher.group(3);

            Component gradientComponent = createGradientWithFormatting(content, startHex, endHex);
            result = result.append(gradientComponent);

            lastEnd = gradientMatcher.end();
        }

        // Добавляем оставшийся текст после последнего градиента
        if (lastEnd < text.length()) {
            String after = text.substring(lastEnd);
            result = result.append(processBasicFormatting(after));
        }

        return result;
    }

    private Component createGradientWithFormatting(String text, String startHex, String endHex) {
        TextColor startColor = TextColor.fromHexString(startHex);
        TextColor endColor = TextColor.fromHexString(endHex);

        if (startColor == null) startColor = TextColor.color(0xFFFFFF);
        if (endColor == null) endColor = TextColor.color(0xFFFFFF);

        // Сначала обрабатываем форматирование внутри градиента
        Component formattedContent = processBasicFormatting(text);

        // Если текст не содержит форматирования, создаем простой градиент
        if (!hasFormatting(formattedContent)) {
            return createSimpleGradient(text, startColor, endColor);
        }

        // Если есть форматирование, применяем градиент к каждому символу с сохранением стилей
        return applyGradientToFormattedComponent(formattedContent, startColor, endColor);
    }

    private boolean hasFormatting(Component component) {
        if (component.style().hasDecoration(TextDecoration.BOLD) ||
            component.style().hasDecoration(TextDecoration.ITALIC) ||
            component.style().hasDecoration(TextDecoration.UNDERLINED) ||
            component.style().hasDecoration(TextDecoration.STRIKETHROUGH) ||
            component.style().hasDecoration(TextDecoration.OBFUSCATED)) {
            return true;
        }

        // Рекурсивно проверяем дочерние компоненты
        for (Component child : component.children()) {
            if (hasFormatting(child)) {
                return true;
            }
        }

        return false;
    }

    private Component applyGradientToFormattedComponent(Component component, TextColor startColor, TextColor endColor) {
        String plainText = LegacyComponentSerializer.legacySection().serialize(component);
        int length = plainText.length();

        if (length == 0) return component;

        Component result = Component.empty();

        for (int i = 0; i < length; i++) {
            float ratio = (float) i / (float) Math.max(1, length - 1);
            TextColor color = interpolateColor(startColor, endColor, ratio);

            // Создаем компонент для одного символа с сохранением стилей исходного компонента
            Component charComponent = Component.text(String.valueOf(plainText.charAt(i)))
                    .color(color)
                    .style(component.style());

            result = result.append(charComponent);
        }

        return result;
    }

    private Component createSimpleGradient(String text, TextColor startColor, TextColor endColor) {
        Component result = Component.empty();
        int length = text.length();

        if (length == 0) return result;

        for (int i = 0; i < length; i++) {
            float ratio = (float) i / (float) Math.max(1, length - 1);
            TextColor color = interpolateColor(startColor, endColor, ratio);
            result = result.append(Component.text(text.charAt(i)).color(color));
        }

        return result;
    }

    private Component processBasicFormatting(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        // Обрабатываем теги форматирования без градиентов
        String processed = processAllFormattingTags(text);

        // Применяем цвета
        return colorize(processed);
    }

    private Component processAdvancedFormatting(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Component.empty();
        }

        // Теперь processGradients возвращает Component, а не String
        return processGradients(text);
    }

    // Обработка всех тегов форматирования
    private String processAllFormattingTags(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Простая замена тегов на Minecraft коды форматирования
        String processed = text;

        // Обрабатываем одиночные теги (не парные)
        processed = processed.replace("<bold>", "§l")
                           .replace("</bold>", "§r")
                           .replace("<italic>", "§o")
                           .replace("</italic>", "§r")
                           .replace("<underlined>", "§n")
                           .replace("</underlined>", "§r")
                           .replace("<strikethrough>", "§m")
                           .replace("</strikethrough>", "§r")
                           .replace("<obfuscated>", "§k")
                           .replace("</obfuscated>", "§r")
                           .replace("<reset>", "§r")
                           .replace("</reset>", "§r");

        // Обрабатываем цвета
        processed = processed.replace("<black>", "§0")
                           .replace("</black>", "§r")
                           .replace("<dark_blue>", "§1")
                           .replace("</dark_blue>", "§r")
                           .replace("<dark_green>", "§2")
                           .replace("</dark_green>", "§r")
                           .replace("<dark_aqua>", "§3")
                           .replace("</dark_aqua>", "§r")
                           .replace("<dark_red>", "§4")
                           .replace("</dark_red>", "§r")
                           .replace("<dark_purple>", "§5")
                           .replace("</dark_purple>", "§r")
                           .replace("<gold>", "§6")
                           .replace("</gold>", "§r")
                           .replace("<gray>", "§7")
                           .replace("</gray>", "§r")
                           .replace("<dark_gray>", "§8")
                           .replace("</dark_gray>", "§r")
                           .replace("<blue>", "§9")
                           .replace("</blue>", "§r")
                           .replace("<green>", "§a")
                           .replace("</green>", "§r")
                           .replace("<aqua>", "§b")
                           .replace("</aqua>", "§r")
                           .replace("<red>", "§c")
                           .replace("</red>", "§r")
                           .replace("<light_purple>", "§d")
                           .replace("</light_purple>", "§r")
                           .replace("<yellow>", "§e")
                           .replace("</yellow>", "§r")
                           .replace("<white>", "§f")
                           .replace("</white>", "§r");

        return processed;
    }

    // Создание градиентного текста
    private Component createGradient(String text, String startHex, String endHex) {
        TextColor startColor = TextColor.fromHexString(startHex);
        TextColor endColor = TextColor.fromHexString(endHex);

        if (startColor == null) startColor = TextColor.color(0xFFFFFF);
        if (endColor == null) endColor = TextColor.color(0xFFFFFF);

        Component result = Component.empty();
        int length = text.length();

        if (length == 0) return result;

        for (int i = 0; i < length; i++) {
            float ratio = (float) i / (float) Math.max(1, length - 1);
            TextColor color = interpolateColor(startColor, endColor, ratio);
            result = result.append(Component.text(text.charAt(i)).color(color));
        }

        return result;
    }

    // Интерполяция цвета
    private TextColor interpolateColor(TextColor start, TextColor end, float ratio) {
        int red = (int) (start.red() + (end.red() - start.red()) * ratio);
        int green = (int) (start.green() + (end.green() - start.green()) * ratio);
        int blue = (int) (start.blue() + (end.blue() - start.blue()) * ratio);

        // Ограничиваем значения 0-255
        red = Math.max(0, Math.min(255, red));
        green = Math.max(0, Math.min(255, green));
        blue = Math.max(0, Math.min(255, blue));

        return TextColor.color(red, green, blue);
    }

    // Обработка стандартных цветов и HEX
    private Component colorize(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        text = text.replace('&', '§');

        // Обрабатываем HEX цвета
        Matcher hexMatcher = HEX_PATTERN.matcher(text);
        StringBuffer hexResult = new StringBuffer();

        while (hexMatcher.find()) {
            String hexColor = hexMatcher.group(1);
            hexMatcher.appendReplacement(hexResult, "§x§" + hexColor.charAt(0) + "§" + hexColor.charAt(1) +
                    "§" + hexColor.charAt(2) + "§" + hexColor.charAt(3) +
                    "§" + hexColor.charAt(4) + "§" + hexColor.charAt(5));
        }
        hexMatcher.appendTail(hexResult);

        // Конвертируем legacy цвета в Component
        return textSerializer.deserialize(hexResult.toString());
    }
}
