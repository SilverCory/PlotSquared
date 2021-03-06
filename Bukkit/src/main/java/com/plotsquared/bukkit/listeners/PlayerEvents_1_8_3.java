package com.plotsquared.bukkit.listeners;

import java.util.Iterator;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;

import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.flag.FlagManager;
import com.intellectualcrafters.plot.object.Location;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotArea;
import com.plotsquared.bukkit.util.BukkitUtil;

public class PlayerEvents_1_8_3 implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBigBoom(final BlockExplodeEvent event) {
        final Block block = event.getBlock();
        Location loc = BukkitUtil.getLocation(block.getLocation());
        final String world = loc.getWorld();
        if (!PS.get().hasPlotArea(world)) {
            return;
        }
        PlotArea area = loc.getPlotArea();
        if (area == null) {
            final Iterator<Block> iter = event.blockList().iterator();
            while (iter.hasNext()) {
                loc = BukkitUtil.getLocation(iter.next().getLocation());
                if (loc.getPlotArea() != null) {
                    iter.remove();
                }
            }
            return;
        }
        Plot plot = area.getOwnedPlot(loc);
        if (plot == null) {
            event.setCancelled(true);
        }
        if (FlagManager.isPlotFlagTrue(plot, "explosion")) {
            final Iterator<Block> iter = event.blockList().iterator();
            while (iter.hasNext()) {
                final Block b = iter.next();
                if (!plot.equals(BukkitUtil.getLocation(b.getLocation()).getPlotAbs())) {
                    iter.remove();
                }
            }
            return;
        }
    }
}
