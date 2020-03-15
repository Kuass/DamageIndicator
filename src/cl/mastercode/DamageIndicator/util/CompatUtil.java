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

import cl.mastercode.DamageIndicator.DIMain;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Class to manage compatibility with older minecraft versions.
 *
 * @author Beelzebu
 */
public final class CompatUtil {

    public static ItemStack RED_INK = null;
    public static int MINOR_VERSION = 8;

    public static void onEnable() {
        MINOR_VERSION = getMinorVersion();
        if (MINOR_VERSION >= 13) {
            if (MINOR_VERSION == 13) {
                RED_INK = new ItemStack(Material.valueOf("ROSE_RED"));
            } else {
                RED_INK = new ItemStack(Material.RED_DYE);
            }
        } else {
            RED_INK = new ItemStack(Material.valueOf("INK_SACK"), 1, (short) 1);
        }
    }

    public static boolean isCanSetCollidable() {
        return MINOR_VERSION > 8;
    }

    public static boolean is113orHigher() {
        return getMinorVersion() > 13;
    }

    private static int getMinorVersion() {
        String ver = Bukkit.getServer().getClass().getPackage().getName().substring(Bukkit.getServer().getClass().getPackage().getName().lastIndexOf(".") + 1);
        int verInt = -1;
        try {
            verInt = Integer.parseInt(ver.split("_")[1]);
        } catch (IllegalArgumentException e) {
            Bukkit.getScheduler().runTask(DIMain.getPlugin(DIMain.class), () -> {
                DIMain.getPlugin(DIMain.class).getLogger().warning("An error occurred getting server version, please contact developer.");
                DIMain.getPlugin(DIMain.class).getLogger().warning("Detected version " + ver);
                Bukkit.getPluginManager().disablePlugin(DIMain.getPlugin(DIMain.class));
            });
        }
        return verInt;
    }
}
