package me.padej.anvilAnimation;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AnvilAnimation extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        // Plugin startup logic
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    public void onAnvilClick(PlayerInteractEvent event) {
        if (!(event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getType() == Material.ANVIL)) return;
        Player player = event.getPlayer();

        if ((player.getGameMode() == org.bukkit.GameMode.SURVIVAL || player.getGameMode() == org.bukkit.GameMode.ADVENTURE) && player.getLevel() < 10) {
            player.sendMessage("Недостаточно уровня. Требуется: 10");
            return;
        }

        Location anvilLocation = event.getClickedBlock().getLocation().add(0.5, 1.0, 0.5);
        anvilLocation.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, anvilLocation, 10, 0.25, 0, 0.25, 0.1);
        anvilLocation.getWorld().spawnParticle(Particle.CRIT, anvilLocation, 10, 0.25, 0, 0.25, 0.1);
        anvilLocation.getWorld().playSound(anvilLocation, Sound.BLOCK_ANVIL_PLACE, 0.8F, 0.7F);
        animateRedAndBlueWool(anvilLocation, player);
        event.setCancelled(true);
    }

    private void animateRedAndBlueWool(Location center, Player player) {
        Item redWool = center.getWorld().dropItem(center.clone(), new ItemStack(Material.RED_WOOL));
        Item blueWool = center.getWorld().dropItem(center.clone(), new ItemStack(Material.BLUE_WOOL));
        redWool.setGravity(false);
        blueWool.setGravity(false);
        redWool.setPickupDelay(Integer.MAX_VALUE);
        blueWool.setPickupDelay(Integer.MAX_VALUE);

        new BukkitRunnable() {
            double radius = 0.0;
            double angle = 0;
            double angleIncrement = 1.0;
            double speedMultiplier = 0.2;
            double height = 1.0;
            int ticks = 0;
            final int totalTicks = 20 * 10 + 60;
            Location finalLocation;

            @Override
            public void run() {
                if (ticks >= totalTicks) {
                    redWool.remove();
                    blueWool.remove();
                    center.getWorld().dropItem(finalLocation, new ItemStack(Material.PURPLE_WOOL));
                    center.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, finalLocation, 1, 0, 0, 0, 1);
                    center.getWorld().playSound(center, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1.2F);
                    cancel();
                    return;
                }

                if (ticks < 5) {
                    radius = 1.0 * ticks / 5.0; // Расходятся за 5 тиков до радиуса 1.0
                } else {
                    if (ticks % 3 == 0) {
                        angleIncrement += 0.5;
                        speedMultiplier += 0.01;
                    }
                    radius -= 1.0 / (totalTicks - 5); // Уменьшение радиуса после начальной фазы
                }

                angle += angleIncrement;
                height += 0.75 / totalTicks;

                double radians = Math.toRadians(angle);
                Vector redPosition = new Vector(Math.cos(radians) * radius, height, Math.sin(radians) * radius);
                Vector bluePosition = new Vector(Math.cos(radians + Math.PI) * radius, height, Math.sin(radians + Math.PI) * radius);

                Vector redVelocity = redPosition.subtract(redWool.getLocation().toVector().subtract(center.toVector())).multiply(speedMultiplier);
                Vector blueVelocity = bluePosition.subtract(blueWool.getLocation().toVector().subtract(center.toVector())).multiply(speedMultiplier);

                redWool.setVelocity(redVelocity);
                blueWool.setVelocity(blueVelocity);

                // Добавляем частицы ELECTRIC_SPARK за RED_WOOL и BLUE_WOOL
                center.getWorld().spawnParticle(Particle.CRIT_MAGIC, redWool.getLocation(), 1, 0.1, 0.1, 0.1, 0.02);
                center.getWorld().spawnParticle(Particle.CRIT_MAGIC, blueWool.getLocation(), 1, 0.1, 0.1, 0.1, 0.02);

                finalLocation = redWool.getLocation().add(blueWool.getLocation()).multiply(0.5);

                center.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, center.clone().add(0, 2, 0), 7, 2.5, 2.5, 2.5, 0.1);
                center.getWorld().spawnParticle(Particle.END_ROD, center.clone().add(0, 2, 0), 1, 2.5, 2.5, 2.5, 0.01);

                if (ticks == 60) {
                    if (player.getGameMode() == org.bukkit.GameMode.SURVIVAL || player.getGameMode() == org.bukkit.GameMode.ADVENTURE) {
                        player.setLevel(player.getLevel() - 10);
                    }
                    animateExperienceBottles(center, player, height, angleIncrement, speedMultiplier);
                }

                ticks++;
            }
        }.runTaskTimer(this, 0, 1);
    }

    private void animateExperienceBottles(Location center, Player player, double initialHeight, double initialAngleIncrement, double initialSpeedMultiplier) {
        List<Item> experienceBottles = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Item expBottle = player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.EXPERIENCE_BOTTLE));
            expBottle.setGravity(false);
            expBottle.setVelocity(expBottle.getVelocity().multiply(0.1F));
            expBottle.setPickupDelay(Integer.MAX_VALUE);
            experienceBottles.add(expBottle);
            center.getWorld().playSound(center, Sound.UI_TOAST_IN, 1, 0.7F);
        }

        new BukkitRunnable() {
            double radius = 1.0;
            double angle = 0;
            double angleIncrement = 0.5;
            double speedMultiplier = 0.05;
            double height = initialHeight;
            int ticks = 0;
            final int totalTicks = 20 * 10 - 60;

            @Override
            public void run() {
                if (ticks >= totalTicks) {
                    experienceBottles.forEach(Item::remove);
                    cancel();
                    return;
                }

                if (ticks % 3 == 0) {
                    angleIncrement += 0.1;
                    speedMultiplier += 0.005;
                } else if (ticks % 2 == 0) {
                    center.getWorld().playSound(center, Sound.UI_TOAST_IN, 0.7F, (float) (1.8 + speedMultiplier));
                }

                if (ticks == 15) { // Увеличиваем скорость спустя 15 тиков
                    angleIncrement = initialAngleIncrement;
                    speedMultiplier = initialSpeedMultiplier;
                    center.getWorld().playSound(center, Sound.UI_TOAST_IN, 1, 0.7F);
                }

                angle += angleIncrement;
                radius -= 1.0 / totalTicks;
                height += 0.75 / totalTicks;

                for (int i = 0; i < experienceBottles.size(); i++) {
                    double expAngle = angle + (360.0 / experienceBottles.size()) * i;
                    double expRadians = Math.toRadians(expAngle);
                    Vector expPosition = new Vector(Math.cos(expRadians) * radius, height, Math.sin(expRadians) * radius);
                    Vector expVelocity = expPosition.subtract(experienceBottles.get(i).getLocation().toVector().subtract(center.toVector())).multiply(speedMultiplier);
                    experienceBottles.get(i).setVelocity(expVelocity);
                }

                ticks++;
            }
        }.runTaskTimer(this, 60, 1);
    }
}