package com.epicnicity322.lessdrops;

import com.epicnicity322.epicpluginlib.core.config.ConfigurationHolder;
import com.epicnicity322.epicpluginlib.core.config.ConfigurationLoader;
import com.epicnicity322.yamlhandler.Configuration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public final class LessDrops extends JavaPlugin implements Listener {

    private static final @NotNull SecureRandom random = new SecureRandom();
    private static final @NotNull ConfigurationHolder config = new ConfigurationHolder(Path.of("plugins", "LessDrops", "config.yml"), """
            # Nerf mob drops.
            # Every time a mob is killed, they have a chance of not dropping any loot.
            Nerf Drop Chance: 50.0 #percent
                        
            # Should XP be removed along with the drops?
            Remove XP Too: true
                        
            # Nerf fortune and looting drops.
            # The amount of drops from items that have this enchantment will be multiplied by this number.
            Fortune and Looting Multiplier: 0.5
                        
            # Entities ignored by drop and looting nerfs.
            Entities Ignored:
            - PLAYER
            - ENDER_DRAGON
                        
            # Nerf armors.
            # Every time a player takes damage, they have a chance of taking the damage as if they had no armor.
            Nerf Armor Chance: 20.0 #percent""");
    private static final @NotNull ConfigurationLoader loader = new ConfigurationLoader() {{
        registerConfiguration(config);
    }};
    private static @Nullable LessDrops instance = null;

    private final HashSet<EntityType> ignored = new HashSet<>();
    private double chanceDrops = 50.0;
    private double chanceArmor = 50.0;
    private double fortuneAndLootingMultiplier = 0.5;
    private boolean removeXPDropsToo = true;

    public LessDrops() {
        instance = this;
    }

    public static boolean reload() {
        if (instance == null) return false;
        HashMap<ConfigurationHolder, Exception> exceptions = loader.loadConfigurations();
        Logger logger = instance.getLogger();

        exceptions.forEach((conf, ex) -> {
            logger.severe("Something went wrong while loading config '" + conf.getPath().getFileName() + "':");
            ex.printStackTrace();
            logger.severe("Using default values.");
        });

        Configuration config = LessDrops.config.getConfiguration();

        instance.chanceDrops = config.getNumber("Nerf Drop Chance").orElse(50.0).doubleValue();
        instance.removeXPDropsToo = config.getBoolean("Remove XP Too").orElse(true);
        instance.ignored.clear();
        config.getCollection("Entities Ignored", obj -> {
            try {
                instance.ignored.add(EntityType.valueOf(obj.toString()));
            } catch (IllegalArgumentException e) {
                logger.warning("Entity '" + obj + "' specified in config is not valid!");
            }
            return null;
        });
        instance.chanceArmor = config.getNumber("Nerf Armor Chance").orElse(20.0).doubleValue();
        instance.fortuneAndLootingMultiplier = config.getNumber("Fortune and Looting Multiplier").orElse(0.5).doubleValue();

        return exceptions.isEmpty();
    }

    @Override
    public void onEnable() {
        reload();
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("lessdropsreload")).setExecutor(new ReloadCommand());
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (ignored.contains(event.getEntityType())) return;
        if (chanceDrops != 0.0 && (chanceDrops == 100.0 || chanceDrops >= random.nextDouble(0.0, 100.0))) {
            event.getDrops().clear();
            if (removeXPDropsToo) event.setDroppedExp(0);
            return;
        }

        Player player = event.getEntity().getKiller();
        if (player == null) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getEnchantmentLevel(Enchantment.LOOT_BONUS_MOBS) == 0) return;

        List<ItemStack> drops = event.getDrops();
        for (ItemStack drop : drops) {
            drop.setAmount((int) Math.ceil(drop.getAmount() * fortuneAndLootingMultiplier));
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) return;
        if (chanceArmor != 0.0 && (chanceArmor == 100.0 || chanceArmor >= random.nextDouble(0.0, 100.0))) {
            if (event.getDamage(EntityDamageEvent.DamageModifier.ARMOR) < 0) {
                event.setDamage(EntityDamageEvent.DamageModifier.ARMOR, 0);
            }
            if (event.getDamage(EntityDamageEvent.DamageModifier.MAGIC) < 0) {
                event.setDamage(EntityDamageEvent.DamageModifier.MAGIC, 0);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockDropItemEvent event) {
        Player player = event.getPlayer();
        if (player.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS) == 0) return;

        for (Item drop : event.getItems()) {
            ItemStack item = drop.getItemStack();
            item.setAmount((int) Math.ceil(item.getAmount() * fortuneAndLootingMultiplier));
            drop.setItemStack(item);
        }
    }
}
