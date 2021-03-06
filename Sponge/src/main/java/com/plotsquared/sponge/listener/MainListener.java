package com.plotsquared.sponge.listener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.explosive.Explosive;
import org.spongepowered.api.entity.explosive.PrimedTNT;
import org.spongepowered.api.entity.living.Ambient;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.animal.Animal;
import org.spongepowered.api.entity.living.monster.Monster;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.vehicle.Boat;
import org.spongepowered.api.entity.vehicle.minecart.Minecart;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.action.InteractEvent;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.NotifyNeighborBlockEvent;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.entity.BreedEntityEvent;
import org.spongepowered.api.event.entity.DisplaceEntityEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.message.MessageEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.event.world.ExplosionEvent.Detonate;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.extent.Extent;

import com.flowpowered.math.vector.Vector3d;
import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.config.Settings;
import com.intellectualcrafters.plot.database.DBFunc;
import com.intellectualcrafters.plot.flag.Flag;
import com.intellectualcrafters.plot.flag.FlagManager;
import com.intellectualcrafters.plot.object.Location;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotArea;
import com.intellectualcrafters.plot.object.PlotBlock;
import com.intellectualcrafters.plot.object.PlotId;
import com.intellectualcrafters.plot.object.PlotManager;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.object.StringWrapper;
import com.intellectualcrafters.plot.util.ExpireManager;
import com.intellectualcrafters.plot.util.MainUtil;
import com.intellectualcrafters.plot.util.MathMan;
import com.intellectualcrafters.plot.util.Permissions;
import com.intellectualcrafters.plot.util.StringMan;
import com.intellectualcrafters.plot.util.TaskManager;
import com.intellectualcrafters.plot.util.UUIDHandler;
import com.plotsquared.listener.PlotListener;
import com.plotsquared.sponge.SpongeMain;
import com.plotsquared.sponge.object.SpongePlayer;
import com.plotsquared.sponge.util.SpongeUtil;

public class MainListener {
    
    /*
     * TODO:
     *  - Anything marked with a TODO below
     *  - BlockPhysicsEvent
     *  - BlockFormEvent
     *  - BlockFadeEvent
     *  - BlockFromToEvent
     *  - BlockDamageEvent
     *  - Structure (tree etc)
     *  - ChunkPreGenerateEvent
     *  - PlayerIgniteBlockEvent
     *  - PlayerBucketEmptyEvent
     *  - PlayerBucketFillEvent
     *  - VehicleCreateEvent
     *  - HangingPlaceEvent
     *  - HangingBreakEvent
     *  - EntityChangeBlockEvent
     *  - PVP
     *  - block dispense
     *  - PVE
     *  - VehicleDestroy
     *  - Projectile
     *  - enderman harvest
     */

    @Listener
    public void onCommand(final SendCommandEvent event) {
        switch (event.getCommand().toLowerCase()) {
            case "plotme": {
                Player source = SpongeUtil.<Player> getCause(event.getCause(), Player.class);
                if (source == null) {
                    return;
                }
                if (Settings.USE_PLOTME_ALIAS) {
                    SpongeMain.THIS.getGame().getCommandManager().process(source, ("plots " + event.getArguments()).trim());
                } else {
                    source.sendMessage(SpongeUtil.text(C.NOT_USING_PLOTME.s()));
                }
                event.setCancelled(true);
            }
        }
    }
    
    @Listener
    public void onChat(final MessageEvent event) {
        // TODO
        Player player = SpongeUtil.<Player> getCause(event.getCause(), Player.class);
        if (player == null) {
            return;
        }
        final String world = player.getWorld().getName();
        if (!PS.get().hasPlotArea(world)) {
            return;
        }
        final PlotArea plotworld = PS.get().getPlotAreaByString(world);
        final PlotPlayer plr = SpongeUtil.getPlayer(player);
        if (!plotworld.PLOT_CHAT && ((plr.getMeta("chat") == null) || !(Boolean) plr.getMeta("chat"))) {
            return;
        }
        final Location loc = SpongeUtil.getLocation(player);
        final Plot plot = loc.getPlot();
        if (plot == null) {
            return;
        }
        final Text message = event.getMessage().orElse(Text.EMPTY);
        
        // TODO use display name rather than username
        //  - Getting displayname currently causes NPE, so wait until sponge fixes that
        
        final String sender = player.getName();
        final PlotId id = plot.getId();
        final String newMessage = StringMan.replaceAll(C.PLOT_CHAT_FORMAT.s(), "%plot_id%", id.x + ";" + id.y, "%sender%", sender);
        final Text forcedMessage = event.getMessage().orElse(Text.EMPTY);
        //        String forcedMessage = StringMan.replaceAll(C.PLOT_CHAT_FORCED.s(), "%plot_id%", id.x + ";" + id.y, "%sender%", sender);
        for (Entry<String, PlotPlayer> entry : UUIDHandler.getPlayers().entrySet()) {
            PlotPlayer user = entry.getValue();
            String toSend;
            if (plot.equals(user.getLocation().getPlot())) {
                toSend = newMessage;
            } else if (Permissions.hasPermission(user, C.PERMISSION_COMMANDS_CHAT)) {
                ((SpongePlayer) user).player.sendMessage(forcedMessage);
                continue;
            } else {
                continue;
            }
            final String[] split = (toSend + " ").split("%msg%");
            final List<Text> components = new ArrayList<>();
            Text prefix = null;
            for (final String part : split) {
                if (prefix != null) {
                    components.add(prefix);
                } else {
                    prefix = message;
                }
                components.add(Text.of(part));
            }
            ((SpongePlayer) user).player.sendMessage(Text.join(components));
        }
        event.setMessage(null);
    }
    
    @Listener
    public void onBreedEntity(final BreedEntityEvent.Breed event) {
        final Location loc = SpongeUtil.getLocation(event.getTargetEntity());
        final String world = loc.getWorld();
        final PlotArea plotworld = PS.get().getPlotAreaByString(world);
        if (plotworld == null) {
            return;
        }
        final Plot plot = loc.getPlot();
        if (plot == null) {
            if (loc.isPlotRoad()) {
                event.setCancelled(true);
            }
            return;
        }
        if (!plotworld.SPAWN_BREEDING) {
            event.setCancelled(true);
        }
    }

    public void onSpawnEntity(SpawnEntityEvent event) throws Exception {
        World world = event.getTargetWorld();
        final PlotArea plotworld = PS.get().getPlotAreaByString(world.getName());
        if (plotworld == null) {
            return;
        }
        event.filterEntities(new Predicate<Entity>() {
            @Override
            public boolean test(Entity entity) {
                if (entity instanceof Player) {
                    return true;
                }
                final Location loc = SpongeUtil.getLocation(entity);
                final Plot plot = loc.getPlot();
                if (plot == null) {
                    if (loc.isPlotRoad()) {
                        return false;
                    }
                    return true;
                }
                //        Player player = this.<Player> getCause(event.getCause());
                // TODO selectively cancel depending on spawn reason
                // - Not sure if possible to get spawn reason (since there are no callbacks)
                //        if (player != null && !plotworld.SPAWN_EGGS) {
                //            return false;
                //            return true;
                //        }
                
                if (entity.getType() == EntityTypes.ITEM) {
                    if (FlagManager.isPlotFlagFalse(plot, "item-drop")) {
                        return false;
                    }
                    return true;
                }
                int[] mobs = null;
                if (entity instanceof Living) {
                    if (!plotworld.MOB_SPAWNING) {
                        return false;
                    }
                    final Flag mobCap = FlagManager.getPlotFlagRaw(plot, "mob-cap");
                    if (mobCap != null) {
                        final Integer cap = (Integer) mobCap.getValue();
                        if (cap == 0) {
                            return false;
                        }
                        if (mobs == null) {
                            mobs = plot.countEntities();
                        }
                        if (mobs[3] >= cap) {
                            return false;
                        }
                    }
                    if ((entity instanceof Ambient) || (entity instanceof Animal)) {
                        final Flag animalFlag = FlagManager.getPlotFlagRaw(plot, "animal-cap");
                        if (animalFlag != null) {
                            final int cap = ((Integer) animalFlag.getValue());
                            if (cap == 0) {
                                return false;
                            }
                            if (mobs == null) {
                                mobs = plot.countEntities();
                            }
                            if (mobs[1] >= cap) {
                                return false;
                            }
                        }
                    }
                    if (entity instanceof Monster) {
                        final Flag monsterFlag = FlagManager.getPlotFlagRaw(plot, "hostile-cap");
                        if (monsterFlag != null) {
                            final int cap = ((Integer) monsterFlag.getValue());
                            if (cap == 0) {
                                return false;
                            }
                            if (mobs == null) {
                                mobs = plot.countEntities();
                            }
                            if (mobs[2] >= cap) {
                                return false;
                            }
                        }
                    }
                    return true;
                }
                if ((entity instanceof Minecart) || (entity instanceof Boat)) {
                    final Flag vehicleFlag = FlagManager.getPlotFlagRaw(plot, "vehicle-cap");
                    if (vehicleFlag != null) {
                        final int cap = ((Integer) vehicleFlag.getValue());
                        if (cap == 0) {
                            return false;
                        }
                        if (mobs == null) {
                            mobs = plot.countEntities();
                        }
                        if (mobs[4] >= cap) {
                            return false;
                        }
                    }
                }
                final Flag entityCap = FlagManager.getPlotFlagRaw(plot, "entity-cap");
                if (entityCap != null) {
                    final Integer cap = (Integer) entityCap.getValue();
                    if (cap == 0) {
                        return false;
                    }
                    if (mobs == null) {
                        mobs = plot.countEntities();
                    }
                    if (mobs[0] >= cap) {
                        return false;
                    }
                }
                if (entity instanceof PrimedTNT) {
                    Vector3d pos = entity.getLocation().getPosition();
                    entity.setRotation(new Vector3d(MathMan.roundInt(pos.getX()), MathMan.roundInt(pos.getY()), MathMan.roundInt(pos.getZ())));
                }
                return true;
            }
        });
    }

    public void onNotifyNeighborBlock(NotifyNeighborBlockEvent event) throws Exception {
        final AtomicBoolean cancelled = new AtomicBoolean(false);
        SpongeUtil.printCause("physics", event.getCause());
        //        PlotArea area = plotloc.getPlotArea();
        //        event.filterDirections(new Predicate<Direction>() {
        //            
        //            @Override
        //            public boolean test(Direction dir) {
        //                if (cancelled.get()) {
        //                    return true;
        //                }
        //                org.spongepowered.api.world.Location<World> loc = relatives.get(dir);
        //                com.intellectualcrafters.plot.object.Location plotloc = SpongeUtil.getLocation(loc.getExtent().getName(), loc);
        //                if (area == null) {
        //                    return true;
        //                }
        //                plot = area.get
        //                Plot plot = plotloc.getPlot();
        //                if (plot == null) {
        //                    if (MainUtil.isPlotAreaAbs(plotloc)) {
        //                        cancelled.set(true);
        //                        return false;
        //                    }
        //                    cancelled.set(true);
        //                    return true;
        //                }
        //                org.spongepowered.api.world.Location<World> relative = loc.getRelative(dir);
        //                com.intellectualcrafters.plot.object.Location relLoc = SpongeUtil.getLocation(relative.getExtent().getName(), relative);
        //                if (plot.equals(MainUtil.getPlot(relLoc))) {
        //                    return true;
        //                }
        //                return false;
        //            }
        //        });
    }

    @Listener
    public void onInteract(InteractEvent event) throws Exception {
        final Player player = SpongeUtil.<Player> getCause(event.getCause(), Player.class);
        if (player == null) {
            event.setCancelled(true);
            return;
        }
        Optional<Vector3d> target = event.getInteractionPoint();
        if (!target.isPresent()) {
            return;
        }
        Location loc = SpongeUtil.getLocation(player.getWorld().getName(), target.get());
        org.spongepowered.api.world.Location l = SpongeUtil.getLocation(loc);
        Plot plot = loc.getPlot();
        PlotPlayer pp = SpongeUtil.getPlayer(player);
        if (plot == null) {
            if (loc.getPlotAbs() == null) {
                return;
            }
            if (!Permissions.hasPermission(pp, C.PERMISSION_ADMIN_INTERACT_ROAD)) {
                event.setCancelled(true);
                return;
            }
            return;
        }
        if (!plot.hasOwner()) {
            if (Permissions.hasPermission(pp, C.PERMISSION_ADMIN_INTERACT_UNOWNED)) {
                return;
            }
            MainUtil.sendMessage(pp, C.NO_PERMISSION_EVENT, C.PERMISSION_ADMIN_INTERACT_UNOWNED);
            event.setCancelled(true);
            return;
        }
        if (plot.isAdded(pp.getUUID()) || Permissions.hasPermission(pp, C.PERMISSION_ADMIN_INTERACT_OTHER)) {
            return;
        } else {
            final Flag flag = FlagManager.getPlotFlagRaw(plot, "use");
            if ((flag != null) && ((HashSet<PlotBlock>) flag.getValue()).contains(SpongeUtil.getPlotBlock(l.getBlock()))) {
                return;
            }
            MainUtil.sendMessage(pp, C.NO_PERMISSION_EVENT, C.PERMISSION_ADMIN_INTERACT_OTHER);
            event.setCancelled(true);
            return;
        }
    }
    
    @Listener
    public void onExplosion(ExplosionEvent e) throws Exception {
        if (e instanceof ExplosionEvent.Detonate) {
            ExplosionEvent.Detonate event = (Detonate) e;
            final World world = event.getTargetWorld();
            final String worldname = world.getName();
            if (!PS.get().hasPlotArea(worldname)) {
                return;
            }
            Optional<Explosive> source = event.getExplosion().getSourceExplosive();
            if (!source.isPresent()) {
                event.setCancelled(true);
                return;
            }
            Explosive tnt = source.get();
            Location origin = SpongeUtil.getLocation(worldname, tnt.getRotation());
            Plot originPlot = origin.getPlot();
            Location current = SpongeUtil.getLocation(tnt);
            final Plot currentPlot = current.getPlot();
            if (!Objects.equals(originPlot, currentPlot)) {
                event.setCancelled(true);
                return;
            }
            if (originPlot == null && current.getPlotAbs() == null) {
                return;
            }
            if (!FlagManager.isPlotFlagTrue(currentPlot, "explosion")) {
                event.setCancelled(true);
                return;
            }
            event.filter(new Predicate<org.spongepowered.api.world.Location<World>>() {
                @Override
                public boolean test(org.spongepowered.api.world.Location<World> loc) {
                    return currentPlot.equals(SpongeUtil.getLocation(loc.getExtent().getName(), loc).getPlot());
                }
            });
            event.filterEntities(new Predicate<Entity>() {
                @Override
                public boolean test(Entity entity) {
                    return currentPlot.equals(SpongeUtil.getLocation(entity).getPlot());
                }
            });
        }
    }
    
    public void onChangeBlock(ChangeBlockEvent event) {
        final World world = event.getTargetWorld();
        final String worldname = world.getName();
        if (!PS.get().hasPlotArea(worldname)) {
            return;
        }
        List<Transaction<BlockSnapshot>> transactions = event.getTransactions();
        Transaction<BlockSnapshot> first = transactions.get(0);
        Location loc = SpongeUtil.getLocation(worldname, first.getOriginal().getPosition());
        Plot plot = loc.getPlot();
        if (plot == null) {
            if (loc.getPlotAbs() == null) {
                return;
            }
            event.setCancelled(true);
            return;
        }
        event.filter(new Predicate<org.spongepowered.api.world.Location<World>>() {
            
            @Override
            public boolean test(org.spongepowered.api.world.Location<World> loc) {
                if (SpongeUtil.getLocation(worldname, loc).isPlotRoad()) {
                    return false;
                }
                return true;
            }
        });
    }

    @Listener
    public void onBlockBreak(final ChangeBlockEvent.Decay event) {
        onChangeBlock(event);
    }
    
    @Listener
    public void onBlockBreak(final ChangeBlockEvent.Grow event) {
        onChangeBlock(event);
    }
    
    @Listener
    public void onBlockBreak(final ChangeBlockEvent.Modify event) {
        onChangeBlock(event);
    }
    
    @Listener
    public void onBlockBreak(final ChangeBlockEvent.Break event) {
        Player player = SpongeUtil.<Player> getCause(event.getCause(), Player.class);
        if (player == null) {
            event.setCancelled(true);
            return;
        }
        final PlotPlayer pp = SpongeUtil.getPlayer(player);
        final World world = event.getTargetWorld();
        final String worldname = world.getName();
        if (!PS.get().hasPlotArea(worldname)) {
            return;
        }
        List<Transaction<BlockSnapshot>> transactions = event.getTransactions();
        Transaction<BlockSnapshot> first = transactions.get(0);
        BlockSnapshot pos = first.getOriginal();
        Location loc = SpongeUtil.getLocation(worldname, pos.getPosition());
        Plot plot = loc.getPlot();
        if (plot == null) {
            if (loc.getPlotAbs() == null) {
                return;
            }
            if (!Permissions.hasPermission(pp, C.PERMISSION_ADMIN_DESTROY_ROAD)) {
                event.setCancelled(true);
                return;
            }
        } else if (transactions.size() == 1) {
            if (!plot.hasOwner()) {
                if (Permissions.hasPermission(pp, C.PERMISSION_ADMIN_DESTROY_UNOWNED)) {
                    return;
                }
                MainUtil.sendMessage(pp, C.NO_PERMISSION_EVENT, C.PERMISSION_ADMIN_DESTROY_UNOWNED);
                event.setCancelled(true);
                return;
            }
            if (plot.isAdded(pp.getUUID()) || Permissions.hasPermission(pp, C.PERMISSION_ADMIN_DESTROY_OTHER)) {
                return;
            } else {
                MainUtil.sendMessage(pp, C.NO_PERMISSION_EVENT, C.PERMISSION_ADMIN_DESTROY_OTHER);
                final Flag destroy = FlagManager.getPlotFlagRaw(plot, "break");
                final BlockState state = pos.getState();
                if ((destroy == null) || !((HashSet<PlotBlock>) destroy.getValue()).contains(SpongeUtil.getPlotBlock(state))) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        event.filter(new Predicate<org.spongepowered.api.world.Location<World>>() {
            
            @Override
            public boolean test(org.spongepowered.api.world.Location<World> l) {
                Location loc = SpongeUtil.getLocation(worldname, l);
                Plot plot = loc.getPlot();
                if (plot == null) {
                    if (loc.getPlotAbs() == null) {
                        return true;
                    }
                    if (!Permissions.hasPermission(pp, C.PERMISSION_ADMIN_DESTROY_ROAD)) {
                        return false;
                    }
                    return true;
                }
                if (!plot.hasOwner()) {
                    if (Permissions.hasPermission(pp, C.PERMISSION_ADMIN_DESTROY_UNOWNED)) {
                        return true;
                    }
                    MainUtil.sendMessage(pp, C.NO_PERMISSION_EVENT, C.PERMISSION_ADMIN_DESTROY_UNOWNED);
                    return false;
                }
                if (plot.isAdded(pp.getUUID()) || Permissions.hasPermission(pp, C.PERMISSION_ADMIN_DESTROY_OTHER)) {
                    return true;
                } else {
                    final Flag destroy = FlagManager.getPlotFlagRaw(plot, "break");
                    final BlockState state = l.getBlock();
                    if ((destroy != null) && ((HashSet<PlotBlock>) destroy.getValue()).contains(SpongeUtil.getPlotBlock(state))) {
                        return true;
                    }
                    MainUtil.sendMessage(pp, C.NO_PERMISSION_EVENT, C.PERMISSION_ADMIN_DESTROY_OTHER);
                    return false;
                }
            }
        });
    }
    
    @Listener
    public void onBlockPlace(final ChangeBlockEvent.Place event) {
        Player player = SpongeUtil.<Player> getCause(event.getCause(), Player.class);
        if (player == null) {
            event.setCancelled(true);
            return;
        }
        final PlotPlayer pp = SpongeUtil.getPlayer(player);
        final World world = event.getTargetWorld();
        final String worldname = world.getName();
        if (!PS.get().hasPlotArea(worldname)) {
            return;
        }
        List<Transaction<BlockSnapshot>> transactions = event.getTransactions();
        Transaction<BlockSnapshot> first = transactions.get(0);
        BlockSnapshot pos = first.getOriginal();
        Location loc = SpongeUtil.getLocation(worldname, pos.getPosition());
        Plot plot = loc.getPlot();
        if (plot == null) {
            if (loc.getPlotAbs() == null) {
                return;
            }
            if (!Permissions.hasPermission(pp, C.PERMISSION_ADMIN_BUILD_ROAD)) {
                event.setCancelled(true);
                return;
            }
        } else if (transactions.size() == 1) {
            if (!plot.hasOwner()) {
                if (Permissions.hasPermission(pp, C.PERMISSION_ADMIN_BUILD_UNOWNED)) {
                    return;
                }
                MainUtil.sendMessage(pp, C.NO_PERMISSION_EVENT, C.PERMISSION_ADMIN_BUILD_UNOWNED);
                event.setCancelled(true);
                return;
            }
            if (plot.isAdded(pp.getUUID()) || Permissions.hasPermission(pp, C.PERMISSION_ADMIN_BUILD_OTHER)) {
                return;
            } else {
                MainUtil.sendMessage(pp, C.NO_PERMISSION_EVENT, C.PERMISSION_ADMIN_BUILD_OTHER);
                final Flag BUILD = FlagManager.getPlotFlagRaw(plot, C.FLAG_PLACE.s());
                final BlockState state = pos.getState();
                if ((BUILD == null) || !((HashSet<PlotBlock>) BUILD.getValue()).contains(SpongeUtil.getPlotBlock(state))) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        event.filter(new Predicate<org.spongepowered.api.world.Location<World>>() {
            
            @Override
            public boolean test(org.spongepowered.api.world.Location<World> l) {
                Location loc = SpongeUtil.getLocation(worldname, l);
                Plot plot = loc.getPlot();
                if (plot == null) {
                    if (loc.getPlotAbs() == null) {
                        return true;
                    }
                    if (!Permissions.hasPermission(pp, C.PERMISSION_ADMIN_BUILD_ROAD)) {
                        return false;
                    }
                    return true;
                }
                if (!plot.hasOwner()) {
                    if (Permissions.hasPermission(pp, C.PERMISSION_ADMIN_BUILD_UNOWNED)) {
                        return true;
                    }
                    MainUtil.sendMessage(pp, C.NO_PERMISSION_EVENT, C.PERMISSION_ADMIN_BUILD_UNOWNED);
                    return false;
                }
                if (plot.isAdded(pp.getUUID()) || Permissions.hasPermission(pp, C.PERMISSION_ADMIN_BUILD_OTHER)) {
                    return true;
                } else {
                    final Flag build = FlagManager.getPlotFlagRaw(plot, C.FLAG_PLACE.s());
                    final BlockState state = l.getBlock();
                    if ((build != null) && ((HashSet<PlotBlock>) build.getValue()).contains(SpongeUtil.getPlotBlock(state))) {
                        return true;
                    }
                    MainUtil.sendMessage(pp, C.NO_PERMISSION_EVENT, C.PERMISSION_ADMIN_BUILD_OTHER);
                    return false;
                }
            }
        });
    }
    
    @Listener
    public void onConnect(final ClientConnectionEvent.Login event) {
        GameProfile profile = event.getProfile();
        if (profile == null) {
            return;
        }
        if (profile.getName().equals("PlotSquared") || profile.getUniqueId().equals(DBFunc.everyone) || DBFunc.everyone.equals(UUIDHandler.getUUID(profile.getName(), null))) {
            event.setCancelled(true);
        }
    }
    
    @Listener
    public void onJoin(final ClientConnectionEvent.Join event) {
        final Player player = event.getTargetEntity();
        SpongeUtil.removePlayer(player.getName());
        final PlotPlayer pp = SpongeUtil.getPlayer(player);
        final String username = pp.getName();
        final StringWrapper name = new StringWrapper(username);
        final UUID uuid = pp.getUUID();
        UUIDHandler.add(name, uuid);
        ExpireManager.dates.put(uuid, System.currentTimeMillis());
        if ((PS.get().update != null) && pp.hasPermission("plots.admin")) {
            TaskManager.runTaskLater(new Runnable() {
                @Override
                public void run() {
                    MainUtil.sendMessage(pp, "&6An update for PlotSquared is available: &7/plot update");
                }
            }, 20);
        }
        final Location loc = SpongeUtil.getLocation(player);
        final Plot plot = loc.getPlot();
        if (plot == null) {
            return;
        }
        if (Settings.TELEPORT_ON_LOGIN) {
            pp.teleport(loc);
            MainUtil.sendMessage(pp, C.TELEPORTED_TO_ROAD);
        }
        PlotListener.plotEntry(pp, plot);
    }
    
    @Listener
    public void onQuit(final ClientConnectionEvent.Disconnect event) {
        final Player player = event.getTargetEntity();
        final PlotPlayer pp = SpongeUtil.getPlayer(player);
        pp.unregister();
    }
    
    @Listener
    public void onMove(final DisplaceEntityEvent.TargetPlayer event) {
        final org.spongepowered.api.world.Location<World> from = event.getFromTransform().getLocation();
        org.spongepowered.api.world.Location<World> to = event.getToTransform().getLocation();
        int x2;
        if (MathMan.roundInt(from.getX()) != (x2 = MathMan.roundInt(to.getX()))) {
            final Player player = event.getTargetEntity();
            final PlotPlayer pp = SpongeUtil.getPlayer(player);
            final Extent extent = to.getExtent();
            pp.setMeta("location", SpongeUtil.getLocation(player));
            final World world = (World) extent;
            final String worldname = ((World) extent).getName();
            final PlotArea plotworld = PS.get().getPlotAreaByString(worldname);
            if (plotworld == null) {
                return;
            }
            final PlotManager plotManager = plotworld.getPlotManager();
            final PlotId id = plotManager.getPlotId(plotworld, x2, 0, MathMan.roundInt(to.getZ()));
            final Plot lastPlot = (Plot) pp.getMeta("lastplot");
            if (id == null) {
                if (lastPlot == null) {
                    return;
                }
                if (!PlotListener.plotExit(pp, lastPlot)) {
                    MainUtil.sendMessage(pp, C.NO_PERMISSION_EVENT, C.PERMISSION_ADMIN_EXIT_DENIED);
                    if (lastPlot.equals(SpongeUtil.getLocation(worldname, from).getPlot())) {
                        event.setCancelled(true);
                    } else {
                        event.setToTransform(new Transform<>(world.getSpawnLocation()));
                    }
                    return;
                }
            } else if ((lastPlot != null) && id.equals(lastPlot.getId())) {
                return;
            } else {
                final Plot plot = PS.get().getPlot(PS.get().getPlotAreaByString(worldname), id);
                if (!PlotListener.plotEntry(pp, plot)) {
                    MainUtil.sendMessage(pp, C.NO_PERMISSION_EVENT, C.PERMISSION_ADMIN_ENTRY_DENIED);
                    if (!plot.getBasePlot(false).equals(SpongeUtil.getLocation(worldname, from).getPlot())) {
                        event.setCancelled(true);
                    } else {
                        event.setToTransform(new Transform<>(world.getSpawnLocation()));
                    }
                    return;
                }
            }
            final Integer border = plotworld.getBorder();
            if (x2 > border) {
                final Vector3d pos = to.getPosition();
                to = to.setPosition(new Vector3d(border - 4, pos.getY(), pos.getZ()));
                event.setToTransform(new Transform<>(to));
                MainUtil.sendMessage(pp, C.BORDER);
            } else if (x2 < -border) {
                final Vector3d pos = to.getPosition();
                to = to.setPosition(new Vector3d(-border + 4, pos.getY(), pos.getZ()));
                event.setToTransform(new Transform<>(to));
                MainUtil.sendMessage(pp, C.BORDER);
            }
            return;
        }
        int z2;
        if (MathMan.roundInt(from.getZ()) != (z2 = MathMan.roundInt(to.getZ()))) {
            final Player player = event.getTargetEntity();
            final PlotPlayer pp = SpongeUtil.getPlayer(player);
            final Extent extent = to.getExtent();
            pp.setMeta("location", SpongeUtil.getLocation(player));
            final World world = (World) extent;
            final String worldname = ((World) extent).getName();
            final PlotArea plotworld = PS.get().getPlotAreaByString(worldname);
            if (plotworld == null) {
                return;
            }
            final PlotManager plotManager = plotworld.getPlotManager();
            final PlotId id = plotManager.getPlotId(plotworld, x2, 0, z2);
            final Plot lastPlot = pp.getMeta("lastplot");
            if (id == null) {
                if (lastPlot == null) {
                    return;
                }
                if (!PlotListener.plotExit(pp, lastPlot)) {
                    MainUtil.sendMessage(pp, C.NO_PERMISSION_EVENT, C.PERMISSION_ADMIN_EXIT_DENIED);
                    if (lastPlot.equals(SpongeUtil.getLocation(worldname, from).getPlot())) {
                        event.setCancelled(true);
                    } else {
                        event.setToTransform(new Transform<>(world.getSpawnLocation()));
                    }
                    return;
                }
            } else if ((lastPlot != null) && id.equals(lastPlot.getId())) {
                return;
            } else {
                final Plot plot = PS.get().getPlot(PS.get().getPlotAreaByString(worldname), id);
                if (!PlotListener.plotEntry(pp, plot)) {
                    MainUtil.sendMessage(pp, C.NO_PERMISSION_EVENT, C.PERMISSION_ADMIN_ENTRY_DENIED);
                    if (!plot.equals(SpongeUtil.getLocation(worldname, from).getPlot())) {
                        event.setCancelled(true);
                    } else {
                        event.setToTransform(new Transform<>(world.getSpawnLocation()));
                    }
                    return;
                }
            }
            final Integer border = plotworld.getBorder();
            if (z2 > border) {
                final Vector3d pos = to.getPosition();
                to = to.setPosition(new Vector3d(pos.getX(), pos.getY(), border - 4));
                event.setToTransform(new Transform<>(to));
                MainUtil.sendMessage(pp, C.BORDER);
            } else if (z2 < -border) {
                final Vector3d pos = to.getPosition();
                to = to.setPosition(new Vector3d(pos.getX(), pos.getY(), -border + 4));
                event.setToTransform(new Transform<>(to));
                MainUtil.sendMessage(pp, C.BORDER);
            }
        }
    }
}