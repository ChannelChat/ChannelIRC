package com.feildmaster.module.craftirc;

import com.ensifera.animosity.craftirc.CraftIRC;
import com.feildmaster.channelchat.Module;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

public class Patch extends Module {
    static CraftIRC handle = null;

    final Set<ChannelPoint> points = new HashSet();

    @Override
    public void onEnable() {
        if (handle == null) {
            Plugin plugin = getServer().getPluginManager().getPlugin("CraftIRC");
            if (plugin instanceof CraftIRC) {
                handle = (CraftIRC) plugin;
                ChannelPoint.irc = handle;
                // Temporary, until CraftIRC gets better handling
                AsyncPlayerChatEvent.getHandlerList().unregister(plugin);
            } else {
                getLogger().warning("Failed to set up CraftIRC!");
                return;
            }
        }
        setupConfig();
        setupPoints();
    }

    @Override
    public void onDisable() {
        Iterator<ChannelPoint> it = points.iterator();
        while (it.hasNext()) {
            final ChannelPoint point = it.next();
            HandlerList.unregisterAll(point);
        }
        points.clear();
    }

    void setupConfig() {
        if (!getConfig().fileExists() || !getConfig().checkDefaults()) {
            getConfig().saveDefaults();
        } else if (getConfig().isFileModified()) {
            getConfig().load();
        }
    }

    void setupPoints() {
        for (String channel : getConfig().getStringList("channels")) {
            points.add(new ChannelPoint(this, channel));
        }
    }
}
