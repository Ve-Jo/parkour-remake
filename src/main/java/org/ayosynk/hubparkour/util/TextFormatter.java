package org.ayosynk.hubparkour.util;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;

public class TextFormatter {
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacySection();

    public Component format(String input, Player player, Map<String, String> placeholders) {
        if (input == null || input.isBlank()) {
            return Component.empty();
        }
        String resolved = applyPlaceholders(input, player, placeholders);
        return miniMessage.deserialize(resolved, toResolvers(placeholders));
    }

    public Component format(String input, Map<String, String> placeholders) {
        if (input == null || input.isBlank()) {
            return Component.empty();
        }
        return miniMessage.deserialize(applyPlaceholders(input, null, placeholders), toResolvers(placeholders));
    }

    public Component format(String input) {
        if (input == null || input.isBlank()) {
            return Component.empty();
        }
        return miniMessage.deserialize(applyPlaceholders(input, null, Map.of()));
    }

    public String formatLegacy(String input, Player player, Map<String, String> placeholders) {
        if (input == null || input.isBlank()) {
            return "";
        }
        Component component = format(input, player, placeholders);
        return legacySerializer.serialize(component);
    }

    public String resolveString(String input, Player player, Map<String, String> placeholders) {
        if (input == null || input.isBlank()) {
            return "";
        }
        return applyPlaceholders(input, player, placeholders);
    }

    private String applyPlaceholders(String input, Player player, Map<String, String> placeholders) {
        String resolved = input;
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                resolved = resolved.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }
        if (player != null && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            resolved = PlaceholderAPI.setPlaceholders(player, resolved);
        }
        return resolved;
    }

    private TagResolver toResolvers(Map<String, String> placeholders) {
        if (placeholders == null || placeholders.isEmpty()) {
            return TagResolver.empty();
        }
        TagResolver.Builder builder = TagResolver.builder();
        placeholders.forEach((key, value) -> builder.resolver(Placeholder.parsed(key, value)));
        return builder.build();
    }
}
