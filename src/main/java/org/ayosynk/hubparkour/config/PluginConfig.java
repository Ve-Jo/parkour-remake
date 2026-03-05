package org.ayosynk.hubparkour.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.List;

public class PluginConfig {
    private final FileConfiguration config;

    public PluginConfig(FileConfiguration config) {
        this.config = config;
    }

    public DatabaseConfig getDatabaseConfig() {
        ConfigurationSection section = config.getConfigurationSection("database");
        if (section == null) {
            return new DatabaseConfig(false, "localhost", 3306, "minecraft_hub", "minecraft", "", 10, false, true);
        }
        return new DatabaseConfig(
                section.getBoolean("enabled", true),
                section.getString("host", "localhost"),
                section.getInt("port", 3306),
                section.getString("name", "minecraft_hub"),
                section.getString("user", "minecraft"),
                section.getString("password", ""),
                section.getInt("pool-size", 10),
                section.getBoolean("use-ssl", false),
                section.getBoolean("create-database", true)
        );
    }

    public boolean isAllowFly() {
        return config.getBoolean("parkour.allow-fly", false);
    }

    public int getTimeoutSeconds() {
        return Math.max(0, config.getInt("parkour.timeout-seconds", 0));
    }

    public boolean isActionBarEnabled() {
        return config.getBoolean("parkour.action-bar-enabled", true);
    }

    public long getActionBarIntervalTicks() {
        return Math.max(1L, config.getLong("parkour.action-bar-interval-ticks", 1L));
    }

    public FormattingConfig getFormatting() {
        return new FormattingConfig(config.getConfigurationSection("formatting"));
    }

    public LeaderboardConfig getLeaderboardConfig() {
        return new LeaderboardConfig(config.getConfigurationSection("leaderboards"));
    }

    public HologramConfig getHologramConfig() {
        return new HologramConfig(config.getConfigurationSection("holograms"));
    }

    public VelocityConfig getVelocityConfig() {
        return new VelocityConfig(config.getConfigurationSection("velocity"));
    }

    public static class FormattingConfig {
        private final ConfigurationSection section;

        public FormattingConfig(ConfigurationSection section) {
            this.section = section;
        }

        public String startPlate() {
            return get("start-plate", "<green>⚑</green> <white>%parkour_name%</white>");
        }

        public String endPlate() {
            return get("end-plate", "<red>⚑</red> <white>%parkour_name%</white>");
        }

        public String checkpointPlate() {
            return get("checkpoint-plate", "<blue>⚑</blue> <gray>(%checkpoint%/%checkpoint_total%) <white>%parkour_name%</white>");
        }

        public String timerFormat() {
            return get("timer", "%m%:%s%:%ms%");
        }

        public String startMessage() {
            return get("start-message", "");
        }

        public String cancelMessage() {
            return get("cancel-message", "");
        }

        public String endMessage() {
            return get("end-message", "");
        }

        public String resetMessage() {
            return get("reset-message", "");
        }

        public String tpMessage() {
            return get("tp-message", "");
        }

        public String checkpointMessage() {
            return get("checkpoint-message", "");
        }

        public String checkpointSkipMessage() {
            return get("checkpoint-skip-message", "");
        }

        public String actionBar() {
            String value = get("action-bar", "");
            if (value == null || value.isBlank()) {
                return "";
            }
            if (!value.contains("%record%")) {
                return value + "  <dark_gray>|</dark_gray>  <color:#ffc157>Record:</color> <yellow>%record%</yellow>";
            }
            return value;
        }

        private String get(String key, String fallback) {
            return section == null ? fallback : section.getString(key, fallback);
        }
    }

    public static class LeaderboardConfig {
        private final ConfigurationSection section;

        public LeaderboardConfig(ConfigurationSection section) {
            this.section = section;
        }

        public boolean isEnabled() {
            return getBoolean("enabled", true);
        }

        public int getUpdateIntervalTicks() {
            return Math.max(20, getInt("update-interval-ticks", 100));
        }

        public int getQueryIntervalTicks() {
            return Math.max(40, getInt("query-interval-ticks", 200));
        }

        public int getBroadcastTop() {
            return Math.max(1, getInt("broadcast-top", 5));
        }

        public LeaderboardFormatting formatting() {
            return new LeaderboardFormatting(section == null ? null : section.getConfigurationSection("formatting"));
        }

        public DisplayItem displayItem() {
            return new DisplayItem(section == null ? null : section.getConfigurationSection("display-item"));
        }

        private int getInt(String key, int fallback) {
            return section == null ? fallback : section.getInt(key, fallback);
        }

        private boolean getBoolean(String key, boolean fallback) {
            return section == null ? fallback : section.getBoolean(key, fallback);
        }

        public static class LeaderboardFormatting {
            private final ConfigurationSection section;

            public LeaderboardFormatting(ConfigurationSection section) {
                this.section = section;
            }

            public String title() {
                String value = get("title", "<gradient:#6efcff:#5a7dff><bold>Лучшие результаты</bold></gradient> <gray>•</gray> <white>%parkour_name%</white>");
                if ("<red>Лучшие результаты: %parkour_name%".equals(value)) {
                    return "<gradient:#6efcff:#5a7dff><bold>Лучшие результаты</bold></gradient> <gray>•</gray> <white>%parkour_name%</white>";
                }
                return value;
            }

            public int maximumDisplayed() {
                return Math.max(1, getInt("maximum-displayed", 10));
            }

            public boolean personalBestEnabled() {
                return getBoolean("personal-best-enabled", false);
            }

            public String defaultLineStyle() {
                return get("default-line-style", "#%position% - %player_name%: %timer%");
            }

            public String personalBestStyle() {
                return get("personal-best-style", "");
            }

            public String emptyLineStyle() {
                return get("empty-line-style", "-");
            }

            public List<String> lines() {
                return section == null ? Collections.emptyList() : section.getStringList("lines");
            }

            private String get(String key, String fallback) {
                return section == null ? fallback : section.getString(key, fallback);
            }

            private int getInt(String key, int fallback) {
                return section == null ? fallback : section.getInt(key, fallback);
            }

            private boolean getBoolean(String key, boolean fallback) {
                return section == null ? fallback : section.getBoolean(key, fallback);
            }
        }

        public static class DisplayItem {
            private final ConfigurationSection section;

            public DisplayItem(ConfigurationSection section) {
                this.section = section;
            }

            public boolean isEnabled() {
                return section != null && section.getBoolean("enabled", true);
            }

            public String getItemId() {
                return section == null ? "minecraft:stone" : section.getString("item", "minecraft:stone");
            }

            public boolean enchantGlint() {
                return section != null && section.getBoolean("enchant-glint", true);
            }

            public boolean spinEnabled() {
                return section != null && section.getBoolean("spin-enabled", true);
            }

            public int spinIntervalTicks() {
                return section == null ? 1 : Math.max(1, section.getInt("spin-interval-ticks", 1));
            }
        }
    }

    public static class HologramConfig {
        private final ConfigurationSection section;

        public HologramConfig(ConfigurationSection section) {
            this.section = section;
        }

        public boolean isEnabled() {
            return section == null || section.getBoolean("enabled", true);
        }

        public int updateIntervalTicks() {
            return section == null ? 100 : Math.max(20, section.getInt("update-interval-ticks", 100));
        }
    }

    public static class VelocityConfig {
        private final ConfigurationSection section;

        public VelocityConfig(ConfigurationSection section) {
            this.section = section;
        }

        public boolean isEnabled() {
            return section != null && section.getBoolean("enabled", true);
        }

        public String channel() {
            return section == null ? "hubparkour:records" : section.getString("channel", "hubparkour:records");
        }

        public String broadcastFormat() {
            return section == null ? "" : section.getString("broadcast-format", "");
        }

        public int globalBroadcastTop() {
            if (section == null) {
                return 0;
            }
            return Math.max(0, section.getInt("global-broadcast-top", 0));
        }

        public String globalBroadcastFormat() {
            return section == null ? "" : section.getString("global-broadcast-format", "");
        }
    }
}
