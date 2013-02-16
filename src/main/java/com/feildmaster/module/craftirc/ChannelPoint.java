package com.feildmaster.module.craftirc;

import com.ensifera.animosity.craftirc.CraftIRC;
import com.ensifera.animosity.craftirc.EndPoint;
import com.ensifera.animosity.craftirc.RelayedMessage;
import com.feildmaster.channelchat.channel.Channel;
import com.feildmaster.channelchat.channel.ChannelManager;
import com.feildmaster.channelchat.event.channel.ChannelCreateEvent;
import com.feildmaster.channelchat.event.channel.ChannelDeleteEvent;
import com.feildmaster.channelchat.event.channel.ChannelEvent;
import com.feildmaster.channelchat.event.channel.ChannelJoinEvent;
import com.feildmaster.channelchat.event.channel.ChannelLeaveEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

final class ChannelPoint implements EndPoint, Listener {
    static CraftIRC irc;
    private Channel channel;
    private final String channel_name;
    private final Patch plugin;

    ChannelPoint(Patch plugin, String name) {
        channel_name = name;
        this.plugin = plugin;
        init();
    }

    private void init() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        irc.registerEndPoint("cc-" + channel_name.toLowerCase(), this);
    }

    public Type getType() {
        return Type.MINECRAFT;
    }

    public void messageIn(final RelayedMessage msg) {
        Channel chan = getChannel();
        if (chan == null) {
            plugin.getLogger().log(Level.FINE, "ChannelChat channel \"{0}\" does not exist", channel_name);
            return;
        }
        chan.sendMessage(String.format(plugin.getConfig().getString("message-format"), msg.getField("sender"), msg.getMessage(), msg.getField("srcChannel")));
    }

    public boolean userMessageIn(String username, RelayedMessage msg) {
        return false;
    }

    public boolean adminMessageIn(RelayedMessage msg) {
        return false;
    }

    public List<String> listUsers() {
        Channel chan = getChannel();
        if (chan == null) {
            return null;
        }
        return new ArrayList<String>(getChannel().getMembers(null));
    }

    public List<String> listDisplayUsers() {
        return null;
    }

    private synchronized Channel getChannel() {
        if (channel == null) { // We keep trying until we find it! (It may be created later...)
            ChannelManager.getManager().getChannel(channel_name);
        }
        return channel;
    }

    private synchronized void set(final Channel channel) {
        if (this.channel == null || channel == null) {
            this.channel = channel;
        }
    }

    private void sendMessage(final String message) {
        sendMessage(channel_name, message);
    }

    private void sendMessage(final String sender, final String message) {
        RelayedMessage msg = irc.newMsg(this, null, "channel-chat");
        msg.setField("message", message);
        msg.setField("sender", sender);
        msg.setField("channel", channel_name);
        msg.post();
    }

    private boolean validChannel(final Channel chan) {
        if (chan != null && chan.getName().equals(channel_name)) {
            set(chan);
            return true;
        }
        return false;
    }

    private boolean validEvent(final ChannelEvent event) {
        if (event instanceof Cancellable && ((Cancellable) event).isCancelled()) {
            return false;
        }
        return validChannel(event.getChannel());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Channel chan = ChannelManager.getManager().getChannel(event);
        if (event.isCancelled() || !validChannel(chan)) {
            return;
        }
        sendMessage(event.getPlayer().getName(), String.format(event.getFormat(), event.getPlayer().getDisplayName(), event.getMessage()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChannelJoin(ChannelJoinEvent event) {
        if (!validEvent(event)) {
            return;
        }
        sendMessage(String.format("%s has joined", event.getPlayer().getDisplayName()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChannelLeave(ChannelLeaveEvent event) {
        if (!validEvent(event)) {
            return;
        }
        sendMessage(String.format("%s has left", event.getPlayer().getDisplayName()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChannelCreate(ChannelCreateEvent event) {
        validChannel(event.getChannel());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChannelRemove(ChannelDeleteEvent event) {
        if (validChannel(event.getChannel())) {
            set(null);
        }
    }
}
