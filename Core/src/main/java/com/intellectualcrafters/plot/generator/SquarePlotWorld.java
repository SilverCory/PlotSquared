package com.intellectualcrafters.plot.generator;

import com.intellectualcrafters.configuration.ConfigurationSection;
import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.object.PlotId;

public abstract class SquarePlotWorld extends GridPlotWorld {
    
    public SquarePlotWorld(String worldname, String id, IndependentPlotGenerator generator, PlotId min, PlotId max) {
        super(worldname, id, generator, min, max);
    }

    public int PLOT_WIDTH = 42;
    public int ROAD_WIDTH = 7;
    public int ROAD_OFFSET_X = 0;
    public int ROAD_OFFSET_Z = 0;
    
    @Override
    public void loadConfiguration(final ConfigurationSection config) {
        if (!config.contains("plot.height")) {
            PS.debug(" - &cConfiguration is null? (" + config.getCurrentPath() + ")");
        }
        PLOT_WIDTH = config.getInt("plot.size");
        ROAD_WIDTH = config.getInt("road.width");
        ROAD_OFFSET_X = config.getInt("road.offset.x");
        ROAD_OFFSET_Z = config.getInt("road.offset.z");
        SIZE = (short) (PLOT_WIDTH + ROAD_WIDTH);
    }
}
