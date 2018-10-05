/**
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

/**
 * @author Beelzebu
 */
@RequiredArgsConstructor
public class BloodListener implements Listener {

    private static final String BLOOD_NAME = "di-blood";
    @Getter
    private final Map<Item, Long> bloodItems = new LinkedHashMap<>();

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamage(EntityDamageEvent e) {
        if (e.isCancelled() || !(e.getEntity() instanceof LivingEntity) || e.getEntity().getType().equals(EntityType.ARMOR_STAND)) {
            return;
        }
        e.getEntity().getWorld().spawnParticle(Particle.REDSTONE, ((LivingEntity) e.getEntity()).getEyeLocation(), 7, .5, 1, .5, new Particle.DustOptions(Color.RED, 3f));
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

    @EventHandler
    public void onInventoryPickup(InventoryPickupItemEvent e) {
        if (bloodItems.containsKey(e.getItem())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        for (int i = 0; i < 3; i++) {
            e.getEntity().getWorld().playEffect(e.getEntity().getLocation(), Effect.STEP_SOUND, Material.REDSTONE_BLOCK);
        }
        e.getEntity().getWorld().playEffect(e.getEntity().getLocation(), Effect.STEP_SOUND, Material.REDSTONE_WIRE, 2);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        for (int i = 0; i < 14; i++) {
            ItemStack is = new ItemStack(Material.ROSE_RED);
            ItemMeta meta = is.getItemMeta();
            meta.setDisplayName("di-blood" + i);
            is.setItemMeta(meta);
            Item item = e.getEntity().getWorld().dropItemNaturally(e.getEntity().getLocation(), is);
            item.setPickupDelay(Integer.MAX_VALUE);
            item.setVelocity(new Vector(new Random().nextDouble() * 0.1, 0.4, new Random().nextDouble() * 0.1));
            bloodItems.put(item, System.currentTimeMillis());
        }
    }
}
