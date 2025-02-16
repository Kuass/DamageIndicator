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
import cl.mastercode.DamageIndicator.util.ConfigUtil;
import cl.mastercode.DamageIndicator.util.EntityHider;
import cl.mastercode.DamageIndicator.util.EntityHider.Policy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.*;
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

import java.text.DecimalFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author YitanTribal, Beelzebu
 */
public class DamageIndicatorListener implements Listener {
    private static final String DISABLED_DI = "DI-DISABLED-DI";
    private final DIMain plugin;
    private final Map<ArmorStand, Long> armorStands = new LinkedHashMap<>();
    private final Set<EntityType> disabledEntities = new HashSet<>();
    private final Set<CreatureSpawnEvent.SpawnReason> disabledSpawnReasons = new HashSet<>();
    private final Set<EntityDamageEvent.DamageCause> disabledDamageCauses = new HashSet<>();
    private final FixedMetadataValue armorStandMeta;
    private boolean enabled = true;
    private boolean enablePlayer = true;
    private boolean enableMonster = true;
    private boolean enableAnimal = true;
    private boolean sneaking = false;
    private EntityHider hider;

    public DamageIndicatorListener(DIMain plugin) {
        this.plugin = plugin;
        armorStandMeta = new FixedMetadataValue(plugin, 0);
        reload();
    }

    public void reload() {
        disabledEntities.clear();
        disabledSpawnReasons.clear();
        enabled = plugin.getConfig().getBoolean("Damage Indicator.Enabled");
        enablePlayer = plugin.getConfig().getBoolean("Damage Indicator.Player");
        enableMonster = plugin.getConfig().getBoolean("Damage Indicator.Monster");
        enableAnimal = plugin.getConfig().getBoolean("Damage Indicator.Animals");
        sneaking = plugin.getConfig().getBoolean("Damage Indicator.Sneaking");
        if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
            hider = new EntityHider(plugin, Policy.BLACKLIST);
        }
        plugin.getConfig().getStringList("Damage Indicator.Disabled Entities").stream().map(entity -> {
            try {
                return EntityType.valueOf(entity.toUpperCase());
            } catch (IllegalArgumentException e) {
                Logger.getLogger(DIMain.class.getName()).log(Level.WARNING, entity.toUpperCase() + " is not a valid EntityType.");
                return null;
            }
        }).filter(Objects::nonNull).forEach(disabledEntities::add);
        plugin.getConfig().getStringList("Damage Indicator.Disabled Spawn Reasons").stream().map(reason -> {
            try {
                return CreatureSpawnEvent.SpawnReason.valueOf(reason.toUpperCase());
            } catch (IllegalArgumentException e) {
                Logger.getLogger(DIMain.class.getName()).log(Level.WARNING, reason.toUpperCase() + " is not a valid SpawnReason.");
                return null;
            }
        }).filter(Objects::nonNull).forEach(disabledSpawnReasons::add);
        plugin.getConfig().getStringList("Damage Indicator.Disabled Damage Causes").stream().map(cause -> {
            try {
                return EntityDamageEvent.DamageCause.valueOf(cause);
            } catch (IllegalArgumentException e) {
                Logger.getLogger(DIMain.class.getName()).log(Level.WARNING, cause.toUpperCase() + " is not a valid DamageCause.");
                return null;
            }
        }).filter(Objects::nonNull).forEach(disabledDamageCauses::add);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if (e.isCancelled()) {
            if (plugin.isDamageIndicator(e.getEntity())) {
                e.setCancelled(false);
            }
            return;
        }
        if (!isSpawnArmorStand(e.getEntity(), null, .1)) {
            return;
        }
        if (disabledSpawnReasons.contains(e.getSpawnReason())) {
            e.getEntity().setMetadata(DISABLED_DI, new FixedMetadataValue(plugin, 1));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void oneEntitySpawn(EntitySpawnEvent e) {
        if (e.isCancelled() && e.getEntity() instanceof ArmorStand) {
            if (plugin.isDamageIndicator(e.getEntity())) {
                e.setCancelled(false);
            }
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
        if (e.getEntity() instanceof Player) {
            Player player = (Player) e.getEntity();
            if (player.isSneaking() && !sneaking) {
                return;
            }
        }
        if (((LivingEntity) e.getEntity()).getHealth() == CompatUtil.getMaxHealth((LivingEntity) e.getEntity())) {
            return;
        }
        if (!e.isCancelled()) {
            handleArmorStand((LivingEntity) e.getEntity(), ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("Damage Indicator.Format.EntityRegain").replace("%health%", damageFormat(e.getAmount()))), null, e.getAmount());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamageEvent(EntityDamageEvent e) {
        if (e.isCancelled()) {
            return;
        }
        if (!(e.getEntity() instanceof LivingEntity)) {
            return;
        }
        handleArmorStand((LivingEntity) e.getEntity(), ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("Damage Indicator.Format.EntityDamage").replace("%damage%", damageFormat(e.getFinalDamage()))), e.getCause(), e.getFinalDamage());
    }

    private String damageFormat(double damage) {
        DecimalFormat df;
        try {
            df = new DecimalFormat(Objects.requireNonNull(plugin.getConfig().getString("Damage Indicator.Format.Decimal", "#.##")));
        } catch (Exception ex) {
            df = new DecimalFormat("#.##");
        }
        return df.format(damage);
    }

    private void handleArmorStand(LivingEntity entity, String format, EntityDamageEvent.DamageCause damageCause, double damage) {
        if (isSpawnArmorStand(entity, damageCause, damage)) {
            spawnArmorStand(entity.getLocation(), format);
        }
    }

    public ArmorStand spawnArmorStand(Location loc, String name) {
        ArmorStand armorStand = CompatUtil.buildArmorStand(loc, plugin.getConfig().getDouble("Damage Indicator.Distance"), armorStandMeta, name);
        if (hider != null) {
            Bukkit.getOnlinePlayers().stream().filter(op -> plugin.getStorageProvider().showArmorStand(op)).forEach(op -> hider.hideEntity(op, armorStand));
        }
        armorStands.put(armorStand, System.currentTimeMillis());
        return armorStand;
    }

    private boolean isSpawnArmorStand(Entity entity, EntityDamageEvent.DamageCause damageCause, double damage) {
        return ConfigUtil.isShowIndicator(entity, damageCause, damage, DISABLED_DI, enabled, enablePlayer, sneaking, enableMonster, enableAnimal, disabledEntities, disabledDamageCauses);
    }

    public Map<ArmorStand, Long> getArmorStands() {
        return armorStands;
    }
}
