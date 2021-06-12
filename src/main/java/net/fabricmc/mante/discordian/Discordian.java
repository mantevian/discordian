package net.fabricmc.mante.discordian;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.mante.discordian.event.MessageListener;
import net.fabricmc.mante.discordian.event.ReadyEventListener;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.security.auth.login.LoginException;

public class Discordian implements DedicatedServerModInitializer {
    public static MinecraftServer server;
    public static Logger logger = LogManager.getLogger("Discord");
    public static JDA jda;
    public static JsonObject config;
    public static String channelID;
    public static TextChannel channel;
    public static boolean sendTellraw;

    @Override
    public void onInitializeServer() {
        config = DiscordConfig.readConfig();
        DiscordConfig.fixConfig();

        channelID = config.get("channel_id").getAsString();
        String token = config.get("token").getAsString();

        if (token.isEmpty()) {
            logger.error("Missing Discord token!");
            return;
        }

        JDABuilder builder = JDABuilder.createDefault(token)
                .addEventListeners(new ReadyEventListener(), new MessageListener())
                .enableIntents(GatewayIntent.GUILD_PRESENCES)
                .enableCache(CacheFlag.CLIENT_STATUS, CacheFlag.ACTIVITY, CacheFlag.EMOTE, CacheFlag.ROLE_TAGS, CacheFlag.MEMBER_OVERRIDES);
        try {
            jda = builder.build();
        } catch (LoginException e) {
            logger.error("Can't connect to Discord!");
            e.printStackTrace();
        }

        ServerLifecycleEvents.SERVER_STARTED.register(s -> {
            server = s;
            channel = jda.getTextChannelById(channelID);
            sendTellraw = config.get("send_tellraw").getAsBoolean();

            if (channel == null) {
                logger.error("Couldn't find the Discord channel!");
                return;
            }

            channel.sendMessage(config.get("start_message").getAsString()).queue();
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
            TextChannel channel = jda.getTextChannelById(channelID);
            if (channel == null)
                return;

            channel.sendMessage(config.get("stop_message").getAsString()).queue();
        });

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, serverResourceManager, success) -> {
            if (!success) {
                channel.sendMessage(config.get("reload_fail_message").getAsString()).queue();
                return;
            }

            channel = jda.getTextChannelById(channelID);
            sendTellraw = config.get("send_tellraw").getAsBoolean();
            if (channel == null) {
                logger.error("Couldn't find the Discord channel!");
                return;
            }

            config = DiscordConfig.readConfig();

            channel.sendMessage(config.get("reload_message").getAsString()).queue();
        });

        ServerTickEvents.END_SERVER_TICK.register(s -> {
            if (s.getTicks() % (20 * 20) == 0) {
                jda.getPresence().setActivity(Activity.playing("with " + s.getCurrentPlayerCount() + " player" + (s.getCurrentPlayerCount() > 1 ? "s" : "")));
            }
        });
    }

    public static ServerCommandSource discordCommandSource() {
        int opLevel = config.get("op_level").getAsInt();
        if (opLevel < 1)
            opLevel = 1;

        if (opLevel > 4)
            opLevel = 4;

        return new ServerCommandSource(
                new DiscordCommandOutput(),
                server.getCommandSource().getPosition(),
                server.getCommandSource().getRotation(),
                server.getCommandSource().getWorld(),
                opLevel,
                server.getCommandSource().getName(),
                server.getCommandSource().getDisplayName(),
                server.getCommandSource().getMinecraftServer(),
                server.getCommandSource().getEntity()
        );
    }
}
