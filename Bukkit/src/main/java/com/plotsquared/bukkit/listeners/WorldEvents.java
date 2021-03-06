package com.plotsquared.bukkit.listeners;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.generator.ChunkGenerator;

import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.generator.GeneratorWrapper;
import com.plotsquared.bukkit.generator.BukkitPlotGenerator;

public class WorldEvents implements Listener {
    
    public static String lastWorld = null;
    
    public static String getName(final World world) {
        if ((lastWorld != null) && !lastWorld.equals("CheckingPlotSquaredGenerator")) {
            return lastWorld;
        } else {
            return world.getName();
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public static void onWorldInit(final WorldInitEvent event) {
        final World world = event.getWorld();
        final String name = getName(world);
        final ChunkGenerator gen = world.getGenerator();
        if (gen instanceof GeneratorWrapper) {
            PS.get().loadWorld(name, (GeneratorWrapper<?>) gen);
        } else {
            if (PS.get().config.contains("worlds." + name)) {
                PS.get().loadWorld(name, new BukkitPlotGenerator(name, gen));
            }
        }
        lastWorld = null;
    }
}
