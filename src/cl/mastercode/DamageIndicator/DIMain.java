/*
 * Copyright 2018 YitanTribal & Beelzebu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cl.mastercode.DamageIndicator;

import cl.mastercode.DamageIndicator.command.CommandHandler;
import cl.mastercode.DamageIndicator.listener.BloodListener;
import cl.mastercode.DamageIndicator.listener.DamageIndicatorListener;
import cl.mastercode.DamageIndicator.storage.SimpleStorageProvider;
import cl.mastercode.DamageIndicator.storage.StorageProvider;
import cl.mastercode.DamageIndicator.util.CompatUtil;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author YitanTribal, Beelzebu
 */
public class DIMain extends JavaPlugin {

    private final File configFile = new File(getDataFolder(), "config.yml");
    private DamageIndicatorListener damageIndicatorListener;
    private BloodListener bloodListener;
    @Getter
    @Setter
    private StorageProvider storageProvider = null;

    public void reload() {
        if (damageIndicatorListener != null) {
            damageIndicatorListener.getArmorStands().forEach((armor, time) -> armor.remove());
            damageIndicatorListener.getArmorStands().clear();
            damageIndicatorListener.reload();
        }
        if (bloodListener != null) {
            bloodListener.getBloodItems().forEach((item, time) -> item.remove());
            bloodListener.getBloodItems().clear();
            bloodListener.reload();
        }
        if (!configFile.exists()) {
            saveResource("config.yml", false);
        }
        if (storageProvider == null) {
            storageProvider = new SimpleStorageProvider();
        }
        reloadConfig();
    }

    @Override
    public void onEnable() {
        reload();
        updateConfig();
        if (getConfig().getBoolean("Damage Indicator.Enabled")) {
            Bukkit.getPluginManager().registerEvents(damageIndicatorListener = new DamageIndicatorListener(this), this);
            damageIndicatorListener.reload();
        }
        if (getConfig().getBoolean("Blood.Enabled")) {
            Bukkit.getPluginManager().registerEvents(bloodListener = new BloodListener(this), this);
            bloodListener.reload();
        }
        getCommand("damageindicator").setExecutor(new CommandHandler(this));
        startTasks();
        CompatUtil.onEnable();
    }

    @Override
    public void onDisable() {
        damageIndicatorListener.getArmorStands().forEach((armor, time) -> armor.remove());
        bloodListener.getBloodItems().forEach((item, time) -> item.remove());
    }

    private void updateConfig() {
        if (getConfig().getInt("version", 1) < 2) {
            List<String> lines = Arrays.asList(
                    "# DamageIndicator Reborn, Minecraft plugin to show the damage taken by a entity",
                    "# Source Code: https://github.com/Beelzebu/DamageIndicator",
                    "# Issue Tracker: https://github.com/Beelzebu/DamageIndicator/issues",
                    "",
                    "# Config version, don't edit",
                    "version: 2",
                    "",
                    "# Damage Indicator options, here you can define in what type of entities we",
                    "# should show the damage indicators and completly disable this feature.",
                    "Damage Indicator:",
                    "  Enabled: true",
                    "  Player: " + getConfig().getBoolean("UseAt.Player", true),
                    "  Monster: " + getConfig().getBoolean("UseAt.Monster", true),
                    "  Animals: " + getConfig().getBoolean("UseAt.Animals", true),
                    "  # Use %health% for the regain health you get",
                    "  # Use %damage% for the damage you get",
                    "  Format:",
                    "    EntityRegain: '" + getConfig().getString("Format.EntityRegain", "&7+&a%health%") + "'",
                    "    EntityDamage: '" + getConfig().getString("Format.EntityDamage", "&7-&c%damage%") + "'",
                    "    # Here you define the decimal format for the damage and health",
                    "    # See https://docs.oracle.com/javase/8/docs/api/java/text/DecimalFormat.html",
                    "    # for more information.",
                    "    Decimal: '" + getConfig().getString("Format.Decimal", "#.##") + "'",
                    "",
                    "# Blood here you can completly disable this feature.",
                    "Blood:",
                    "  Enabled: true"
            );
            try {
                Files.write(configFile.toPath(), lines);
                reloadConfig();
                Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&eDamageIndicator config updated to v2."));
            } catch (IOException ex) {
                Logger.getLogger(DIMain.class.getName()).log(Level.WARNING, "Can't save config v2", ex);
            }
        }
        if (getConfig().getInt("version", 2) < 3) {
            List<String> lines = Arrays.asList(
                    "# DamageIndicator Reborn, Minecraft plugin to show the damage taken by a entity",
                    "# Source Code: https://github.com/Beelzebu/DamageIndicator",
                    "# Issue Tracker: https://github.com/Beelzebu/DamageIndicator/issues",
                    "",
                    "# Config version, don't edit",
                    "version: 3",
                    "",
                    "# Damage Indicator options, here you can define in what type of entities we",
                    "# should show the damage indicators and completely disable this feature.",
                    "Damage Indicator:",
                    "  Enabled: " + getConfig().getBoolean("Damage Indicator.Enabled"),
                    "  Player: " + getConfig().getBoolean("Damage Indicator.Player"),
                    "  Monster: " + getConfig().getBoolean("Damage Indicator.Monster"),
                    "  Animals: " + getConfig().getBoolean("Damage Indicator.Animals"),
                    "  # Use %health% for the regain health you get",
                    "  # Use %damage% for the damage you get",
                    "  Format:",
                    "    EntityRegain: '" + getConfig().getString("Damage Indicator.Format.EntityRegain") + "'",
                    "    EntityDamage: '" + getConfig().getString("Damage Indicator.Format.EntityDamage") + "'",
                    "    # Here you define the decimal format for the damage and health",
                    "    # See https://docs.oracle.com/javase/8/docs/api/java/text/DecimalFormat.html",
                    "    # for more information.",
                    "    Decimal: '" + getConfig().getString("Damage Indicator.Format.Decimal") + "'",
                    "  # Distance in Y axis from the entity where we should spawn the damage indicator",
                    "  # A distance of 2 is recommended for 1.8 servers",
                    "  Distance: 1.6",
                    "  # List of disabled entity types that shouldn't spawn damage indicators",
                    "  # See https://hub.spigotmc.org/javadocs/spigot/org/bukkit/entity/EntityType.html",
                    "  # for all possible entities.",
                    "  Disabled Entities:",
                    "  - GHAST",
                    "  - ZOMBIE",
                    "  - IRON_GOLEM",
                    "  # List of spawn reasons that we must listen and mark entities so they won't spawn",
                    "  # damage indicators.",
                    "  # See https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/event/entity/CreatureSpawnEvent.SpawnReason.html",
                    "  # for all possible spawn reasons.",
                    "  Disabled Reasons:",
                    "  - SPAWNER",
                    "",
                    "# Blood options, here you can configure this feature.",
                    "Blood:",
                    "  Enabled: " + getConfig().getBoolean("Blood.Enabled"),
                    "  Player: true",
                    "  Monster: true",
                    "  Animals: true",
                    "  # List of disabled entity types that shouldn't bleed.",
                    "  # See https://hub.spigotmc.org/javadocs/spigot/org/bukkit/entity/EntityType.html",
                    "  # for all possible entities.",
                    "  Disabled Entities:",
                    "  - ZOMBIE",
                    "  - SKELETON",
                    "  # List of spawn reasons that we must listen and mark entities so they won't bleed.",
                    "  # See https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/event/entity/CreatureSpawnEvent.SpawnReason.html",
                    "  # for all possible spawn reasons.",
                    "  Disabled Reasons:",
                    "  - SPAWNER",
                    "",
                    "Messages:",
                    "  Damage Indicator:",
                    "    Enabled: '" + getConfig().getString("Messages.Damage Indicator.Enabled") + "'",
                    "    Disabled: '" + getConfig().getString("Messages.Damage Indicator.Disabled") + "'"
            );
            try {
                Files.write(configFile.toPath(), lines);
                reloadConfig();
                Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&eDamageIndicator config updated to v3."));
            } catch (IOException ex) {
                Logger.getLogger(DIMain.class.getName()).log(Level.WARNING, "Can't save config v3", ex);
            }
        }
    }

    private void startTasks() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            Iterator<Map.Entry<ArmorStand, Long>> asit = damageIndicatorListener.getArmorStands().entrySet().iterator();
            while (asit.hasNext()) {
                Map.Entry<ArmorStand, Long> ent = asit.next();
                if (ent.getValue() + 1500 <= System.currentTimeMillis()) {
                    ent.getKey().remove();
                    asit.remove();
                } else {
                    ent.getKey().teleport(ent.getKey().getLocation().clone().add(0.0, 0.07, 0.0));
                }
            }
            Iterator<Map.Entry<Item, Long>> bit = bloodListener.getBloodItems().entrySet().iterator();
            while (bit.hasNext()) {
                Map.Entry<Item, Long> ent = bit.next();
                if (ent.getValue() + 2000 <= System.currentTimeMillis()) {
                    ent.getKey().remove();
                    bit.remove();
                }
            }
        }, 0, 1);
    }

    public boolean isDamageIndicator(Entity entity) {
        return isDamageIndicator(entity, true);
    }

    public boolean isDamageIndicator(Entity entity, boolean strict) {
        if (!(entity instanceof ArmorStand)) {
            return false;
        }
        ArmorStand as = (ArmorStand) entity;
        return as.hasMetadata("Mastercode-DamageIndicator") || !strict && (as.isMarker() && !as.isVisible() && as.isCustomNameVisible() && !as.hasGravity());
    }
}
