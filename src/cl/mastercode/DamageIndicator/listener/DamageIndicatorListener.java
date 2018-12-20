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
package cl.mastercode.DamageIndicator.listener;

import cl.mastercode.DamageIndicator.DIMain;
import cl.mastercode.DamageIndicator.util.CompatUtil;
import cl.mastercode.DamageIndicator.util.EntityHider;
import cl.mastercode.DamageIndicator.util.EntityHider.Policy;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Animals;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.metadata.FixedMetadataValue;

/**
 * @author YitanTribal, Beelzebu
 */
public class DamageIndicatorListener implements Listener {

    private static final String DISABLED_DI = "DI-DISABLED-DI";
    private final DIMain plugin;
    @Getter
    private final Map<ArmorStand, Long> armorStands = new LinkedHashMap<>();
    private final Set<EntityType> disabledEntities = new HashSet<>();
    private final Set<CreatureSpawnEvent.SpawnReason> disabledSpawnReasons = new HashSet<>();
    private boolean enabled;
    private boolean enablePlayer;
    private boolean enableMonster;
    private boolean enableAnimal;
    private EntityHider hider;

    public DamageIndicatorListener(DIMain plugin) {
        this.plugin = plugin;
        enabled = plugin.getConfig().getBoolean("Damage Indicator.Enabled");
        enablePlayer = plugin.getConfig().getBoolean("Damage Indicator.Player");
        enableMonster = plugin.getConfig().getBoolean("Damage Indicator.Monster");
        enableAnimal = plugin.getConfig().getBoolean("Damage Indicator.Animals");
        if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
            hider = new EntityHider(plugin, Policy.BLACKLIST);
        }
        plugin.getConfig().getStringList("Damage Indicator.Disabled Entities").stream().map(entity -> {
            try {
                return EntityType.valueOf(entity.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }).filter(Objects::nonNull).forEach(disabledEntities::add);
        plugin.getConfig().getStringList("Damage Indicator.Disabled Reasons").stream().map(reason -> {
            try {
                return CreatureSpawnEvent.SpawnReason.valueOf(reason.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }).filter(Objects::nonNull).forEach(disabledSpawnReasons::add);
    }

    public void reload() {
        enabled = plugin.getConfig().getBoolean("Damage Indicator.Enabled");
        enablePlayer = plugin.getConfig().getBoolean("Damage Indicator.Player");
        enableMonster = plugin.getConfig().getBoolean("Damage Indicator.Monster");
        enableAnimal = plugin.getConfig().getBoolean("Damage Indicator.Animals");
        disabledEntities.clear();
        disabledSpawnReasons.clear();
        plugin.getConfig().getStringList("Damage Indicator.Disabled Entities").stream().map(entity -> {
            try {
                return EntityType.valueOf(entity.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }).filter(Objects::nonNull).forEach(disabledEntities::add);
        plugin.getConfig().getStringList("Damage Indicator.Disabled Reasons").stream().map(reason -> {
            try {
                return CreatureSpawnEvent.SpawnReason.valueOf(reason.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }).filter(Objects::nonNull).forEach(disabledSpawnReasons::add);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if (e.isCancelled()) {
            return;
        }
        if (!spawnArmorStand(e.getEntity())) {
            return;
        }
        if (disabledSpawnReasons.contains(e.getSpawnReason())) {
            e.getEntity().setMetadata(DISABLED_DI, new FixedMetadataValue(plugin, 1));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void oneEntitySpawn(EntitySpawnEvent e) {
        if (e.getEntity() instanceof ArmorStand && armorStands.containsKey(e.getEntity())) {
            e.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity.getType().equals(EntityType.ARMOR_STAND)) {
                ArmorStand as = (ArmorStand) entity;
                if (plugin.isDamageIndicator(as)) {
                    armorStands.remove(as);
                    as.remove();
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity.getType().equals(EntityType.ARMOR_STAND)) {
                ArmorStand as = (ArmorStand) entity;
                if (plugin.isDamageIndicator(as)) {
                    armorStands.remove(as);
                    as.remove();
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityRegainHealth(EntityRegainHealthEvent e) {
        if (!(e.getEntity() instanceof LivingEntity)) {
            return;
        }
        if (!e.isCancelled()) {
            handleArmorStand((LivingEntity) e.getEntity(), ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("Damage Indicator.Format.EntityRegain").replace("%health%", damageFormat(e.getAmount()))));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamageEvent(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof LivingEntity)) {
            return;
        }
        if (!e.isCancelled()) {
            handleArmorStand((LivingEntity) e.getEntity(), ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("Damage Indicator.Format.EntityDamage").replace("%damage%", damageFormat(e.getFinalDamage()))));
        }
    }

    private String damageFormat(double damage) {
        DecimalFormat df;
        try {
            df = new DecimalFormat(plugin.getConfig().getString("Damage Indicator.Format.Decimal", "#.##"));
        } catch (Exception ex) {
            df = new DecimalFormat("#.##");
        }
        return df.format(damage);
    }

    private void handleArmorStand(LivingEntity entity, String format) {
        if (!spawnArmorStand(entity)) {
            return;
        }
        armorStands.put(getDefaultArmorStand(entity.getLocation(), format), System.currentTimeMillis());
    }

    public ArmorStand getDefaultArmorStand(Location loc, String name) {
        ArmorStand as = (ArmorStand) loc.getWorld().spawnEntity(loc.clone().add(0, 255 - loc.getY(), 0), EntityType.ARMOR_STAND);
        as.setVisible(false);
        as.setCustomNameVisible(false);
        as.setSmall(true);
        as.setRemoveWhenFarAway(true);
        as.setMetadata("Mastercode-DamageIndicator", new FixedMetadataValue(plugin, 1));
        as.setGravity(false);
        if (!CompatUtil.is18()) {
            as.setCollidable(false);
            as.setInvulnerable(true);
        }
        as.setMarker(true);
        as.teleport(loc.add(0, plugin.getConfig().getDouble("Damage Indicator.Distance"), 0));
        as.setCustomName(name);
        as.setCustomNameVisible(true);
        if (hider != null) {
            Bukkit.getOnlinePlayers().stream().filter(op -> !plugin.getStorageProvider().showArmorStand(op)).forEach(op -> hider.hideEntity(op, as));
        }
        return as;
    }

    private boolean spawnArmorStand(Entity entity) {
        if (entity.hasMetadata("NPC")) {
            return false;
        }
        if (entity.hasMetadata(DISABLED_DI)) {
            return false;
        }
        if (!(entity instanceof LivingEntity)) {
            return false;
        }
        if (entity instanceof ArmorStand) {
            return false;
        }
        if (entity instanceof Player && !enablePlayer) {
            return false;
        }
        if ((entity instanceof Monster || entity instanceof Slime) && !enableMonster) {
            return false;
        }
        if (entity instanceof Animals && !enableAnimal) {
            return false;
        }
        if (!enabled) {
            return false;
        }
        return !disabledEntities.contains(entity.getType());
    }
}
