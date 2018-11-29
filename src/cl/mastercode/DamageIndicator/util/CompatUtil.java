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
package cl.mastercode.DamageIndicator.util;

import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;

/**
 * Class to manage compatibility with older minecraft versions.
 *
 * @author Beelzebu
 */
public final class CompatUtil {

    public static ItemStack RED_INK = null;

    public static void onEnable() {
        if (is113()) {
            RED_INK = new ItemStack(Material.ROSE_RED);
        } else {
            RED_INK = new ItemStack(Material.valueOf("INK_SACK"), 1, (short) 1);
        }
    }

    public static boolean is18() {
        try {
            ArmorStand.class.getMethod("setCollidable", boolean.class);
            return false;
        } catch (ReflectiveOperationException ex) {
            ex.printStackTrace();
            return true;
        }
    }

    public static boolean is113() {
        try {
            try {
                Class.forName("org.bukkit.Particle$DustOptions");
            } catch (ClassNotFoundException e) {
                return false;
            }
            Material.valueOf("ROSE_RED");
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
