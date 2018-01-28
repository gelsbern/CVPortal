package org.cubeville.cvportal.warps;

import java.io.File;
import java.io.IOException;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import org.cubeville.cvipc.CVIPC;
import org.cubeville.cvipc.IPCInterface;

public class WarpManager implements IPCInterface
{
    Map<String, Warp> warps;
    CVIPC ipc;
    File configFile;
    Configuration config;
    
    public WarpManager(Configuration config, File configFile, CVIPC ipc) {
        warps = new HashMap<>();
        this.ipc = ipc;
        this.config = config;
        this.configFile = configFile;

        System.out.println("Loading warp manager config: " + config);
        if(config != null) {
            Collection<String> warpNames = config.getKeys();
            for(String warpName: warpNames) {
                System.out.println("Loading warp " + warpName);
                Configuration warp = (Configuration) config.get(warpName);
                String server = warp.getString("server");
                String world = warp.getString("world");
                Double x = warp.getDouble("x");
                Double y = warp.getDouble("y");
                Double z = warp.getDouble("z");
                Float yaw = warp.getFloat("yaw");
                Float pitch = warp.getFloat("pitch");
                String command = warp.getString("command");
                warps.put(warpName, new Warp(server, world, x, y, z, yaw, pitch, command));
            }
        }

        ipc.registerInterface("setwarp", this);
    }

    public Map<String, String> getWarpCommands() {
        Map<String, String> ret = new HashMap<>();
        for(String warp: warps.keySet()) {
            String command = warps.get(warp).getCommand();
            if(command != null && (!command.equals(""))) ret.put(command, warp);
        }
        return ret;
    }

    public Set<String> getWarpNames() {
        return warps.keySet();
    }
    
    public void save() {
        Configuration cmap = new Configuration();
        cmap.set("warps", config);
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(cmap, configFile);
        } catch(IOException e) {}
    }
    
    public void process(String server, String channel, String message) {
        int idx = message.indexOf("|");
        if(idx == -1) return;

        String name = message.substring(0, idx);

        StringTokenizer tk = new StringTokenizer(message.substring(idx + 1), ",");
        String world = tk.nextToken();
        double x = Double.valueOf(tk.nextToken());
        double y = Double.valueOf(tk.nextToken());
        double z = Double.valueOf(tk.nextToken());
        double pitch = Double.valueOf(tk.nextToken());
        double yaw = Double.valueOf(tk.nextToken());
        
        Warp warp = new Warp(server, world, x, y, z, yaw, pitch, null);
        warps.put(name, warp);

        config.set(name, warp.getConfiguration());
        save();
    }

    public void teleport(ProxiedPlayer player, String warpName) {
        System.out.println("Teleport player " + player + " to warp " + warpName);
        Warp warp = warps.get(warpName);
        if(warp == null) {
            player.sendMessage("§cUnknown warp " + warpName);
            return;
        }
        ipc.sendMessage(warp.getServer(), "xwportal|" + player.getUniqueId() + "|coord:" + warp.getLocationString() + "|" + warp.getServer());
    }
}
