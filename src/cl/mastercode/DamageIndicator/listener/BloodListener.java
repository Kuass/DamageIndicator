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
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import lombok.Getter;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Animals;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

/**
 * @author Beelzebu
 */
public class BloodListener implements Listener {

    private static final String BLOOD_NAME = "di-blood";
    private static final String DISABLED_BLOOD = "DI-DISABLED-BLOOD";
    private final DIMain plugin;
    @Getter
    private final Map<Item, Long> bloodItems = new LinkedHashMap<>();
    private final Set<EntityType> disabledEntities = new HashSet<>();
    private final Set<CreatureSpawnEvent.SpawnReason> disabledSpawnReasons = new HashSet<>();
    private final Random random = new Random();
    private final boolean enablePlayer;
    private final boolean enableMonster;
    private final boolean enableAnimal;
    private Method playEffect;

    public BloodListener(DIMain plugin) {
        this.plugin = plugin;
        enablePlayer = plugin.getConfig().getBoolean("Blood.Player");
        enableMonster = plugin.getConfig().getBoolean("Blood.Monster");
        enableAnimal = plugin.getConfig().getBoolean("Blood.Animals");
        if (!CompatUtil.is113()) {
            try {
                playEffect = World.Spigot.class.getMethod("playEffect", Location.class, Effect.class, int.class, int.class, float.class, float.class, float.class, float.class, int.class, int.class);
            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
            }
        } else {
            playEffect = null;
        }
    }

    public void reload() {
        disabledEntities.clear();
        disabledSpawnReasons.clear();
        plugin.getConfig().getStringList("Blood.Disabled Entities").stream().map(entity -> {
            try {
                return EntityType.valueOf(entity.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }).filter(Objects::nonNull).forEach(disabledEntities::add);
        plugin.getConfig().getStringList("Blood.Disabled Reasons").stream().map(reason -> {
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
        if (!showBlood(e.getEntity())) {
            return;
        }
        if (disabledSpawnReasons.contains(e.getSpawnReason())) {
            e.getEntity().setMetadata(DISABLED_BLOOD, new FixedMetadataValue(plugin, 1));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamage(EntityDamageEvent e) {
        if (e.isCancelled()) {
            return;
        }
        Entity entity = e.getEntity();
        if (!showBlood(entity)) {
            return;
        }
        if (CompatUtil.is113()) {
            e.getEntity().getWorld().spawnParticle(Particle.REDSTONE, ((LivingEntity) e.getEntity()).getEyeLocation(), 7, .5, 1, .5, new Particle.DustOptions(Color.RED, 3f));
        } else if (CompatUtil.is18()) {
            try {
                if (playEffect != null) {
                    playEffect.invoke(e.getEntity().getWorld().spigot(), ((LivingEntity) e.getEntity()).getEyeLocation(), Effect.valueOf("COLOURED_DUST"), 0, 0, 0.4f, 0.3f, 0.4f, 0, 8, 16);
                }
            } catch (ReflectiveOperationException e1) {
                e1.printStackTrace();
            }
        } else {
            for (int i = 0; i < 5; i++) {
                e.getEntity().getWorld().spawnParticle(Particle.REDSTONE, ((LivingEntity) e.getEntity()).getEyeLocation().clone().add(random.nextDouble(), random.nextDouble(), random.nextDouble()), 0, 255, 0, 0, 1);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemPickup(EntityPickupItemEvent e) {
        if (bloodItems.containsKey(e.getItem())) {
            e.setCancelled(true);
            return;
        }
        if (e.getItem().getPickupDelay() == Integer.MAX_VALUE && e.getItem().getItemStack().getItemMeta().hasDisplayName() && e.getItem().getItemStack().getItemMeta().getDisplayName().startsWith(BLOOD_NAME)) {
            e.getItem().remove();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemPickup(PlayerPickupItemEvent e) {
        if (bloodItems.containsKey(e.getItem())) {
            e.setCancelled(true);
            return;
        }
        if (e.getItem().getPickupDelay() == Integer.MAX_VALUE && e.getItem().getItemStack().getItemMeta().hasDisplayName() && e.getItem().getItemStack().getItemMeta().getDisplayName().startsWith(BLOOD_NAME)) {
            e.getItem().remove();
        }
    }

    @EventHandler
    public void onInventoryPickup(InventoryPickupItemEvent e) {
        if (bloodItems.containsKey(e.getItem())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        if (!showBlood(e.getEntity())) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            e.getEntity().getWorld().playEffect(e.getEntity().getLocation(), Effect.STEP_SOUND, Material.REDSTONE_BLOCK);
        }
        e.getEntity().getWorld().playEffect(e.getEntity().getLocation(), Effect.STEP_SOUND, Material.REDSTONE_WIRE, 2);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        if (!enablePlayer) {
            return;
        }
        for (int i = 0; i < 14; i++) {
            ItemStack is = new ItemStack(CompatUtil.RED_INK);
            ItemMeta meta = is.getItemMeta();
            meta.setDisplayName("di-blood" + i);
            is.setItemMeta(meta);
            Item item = e.getEntity().getWorld().dropItemNaturally(e.getEntity().getLocation(), is);
            item.setPickupDelay(Integer.MAX_VALUE);
            item.setVelocity(new Vector(random.nextDouble() * 0.1, 0.4, random.nextDouble() * 0.1));
            bloodItems.put(item, System.currentTimeMillis());
        }
    }

    private boolean showBlood(Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return false;
        }
        if (entity.hasMetadata("NPC")) {
            return false;
        }
        if (entity.hasMetadata(DISABLED_BLOOD)) {
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
        return !disabledEntities.contains(entity.getType());
    }
}
