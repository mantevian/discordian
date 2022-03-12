package net.fabricmc.mante.discordian;

import club.minnced.discord.webhook.WebhookClient;
import com.google.gson.JsonArray;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.mante.discordian.event.MessageListener;
import net.fabricmc.mante.discordian.event.ReadyEventListener;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.MathHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.security.auth.login.LoginException;

public class Discordian implements DedicatedServerModInitializer {
    public static MinecraftServer server;
    public static Logger logger = LogManager.getLogger("Discord");
    public static JDA jda;
    public static DiscordConfigManager configManager;
    public static TextChannel channel;
    public static Guild guild;
    public static AccountLinkManager accountLinkManager;
    public static WebhookClient webhook;
    public static boolean useWebhook;

    @Override
    public void onInitializeServer() {
        configManager = new DiscordConfigManager();
        configManager.updateConfig();
        accountLinkManager = new AccountLinkManager();
        DiscordLinkCommand.load();

        useWebhook = false;

        String token = configManager.config.get("token").getAsString();

        if (token.isEmpty()) {
            logger.error("Missing Discord token.");
            return;
        }

        JDABuilder builder = JDABuilder
                .createDefault(token)
                .addEventListeners(new ReadyEventListener(), new MessageListener())
                .enableIntents(GatewayIntent.GUILD_PRESENCES)
                .enableCache(CacheFlag.CLIENT_STATUS, CacheFlag.ACTIVITY, CacheFlag.EMOTE, CacheFlag.ROLE_TAGS, CacheFlag.MEMBER_OVERRIDES);
        try {
            jda = builder.build();
        } catch (LoginException e) {
            logger.error("Can't connect to Discord. Make sure you've provided the correct token.");
            e.printStackTrace();
        }

        ServerLifecycleEvents.SERVER_STARTED.register(s -> {
            server = s;
            load();
            channel.sendMessage(configManager.config.get("start_message").getAsString()).queue();
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(s ->
                channel.sendMessage(configManager.config.get("stop_message").getAsString()).queue());

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, serverResourceManager, success) -> {
            if (!success)
                channel.sendMessage(configManager.config.get("reload_fail_message").getAsString()).queue();
            else {
                load();
                channel.sendMessage(configManager.config.get("reload_message").getAsString()).queue();
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(s -> {
            JsonArray strings = configManager.config.get("statuses").getAsJsonArray();

            for (int i = 0; i < strings.size(); i++) {
                String str = strings.get(i).getAsString();

                str = str.replaceAll("\\{players}", String.valueOf(s.getCurrentPlayerCount()));
                str = str.replaceAll("\\{tps}", String.valueOf(Math.round(MathHelper.clamp(1000 / DiscordUtil.avgTick(), 0, 20) * 10) / 10d));

                if (s.getTicks() % (20 * 20 * strings.size()) == 20 * 20 * i)
                    jda.getPresence().setActivity(Activity.playing(str));
            }
        });
    }

    private void load() {
        try {
            channel = jda.getTextChannelById(configManager.config.get("channel_id").getAsString());
        }
        catch (NumberFormatException e) {
            logger.error("Couldn't parse the provided channel ID.");
            return;
        }

        if (channel == null) {
            logger.error("Couldn't find the Discord channel. Make sure you've provided the correct ID.");
            return;
        }
        guild = channel.getGuild();

        if (configManager.config.get("use_webhook").getAsBoolean()) {
            try {
                webhook = WebhookClient.withUrl(configManager.config.get("webhook_url").getAsString());
                useWebhook = true;
            } catch (Exception e) {
                logger.error("Couldn't load the webhook. Make sure you have created the webhook in #" + channel.getName() + " and provided correct details. Switching to no webhook mode. Details: " + e);
                useWebhook = false;
            }
        }

        configManager.updateConfig();
    }

    public static ServerCommandSource discordCommandSource() {
        int opLevel = configManager.config.get("op_level").getAsInt();
        opLevel = MathHelper.clamp(opLevel, 1, 4);

        return new ServerCommandSource(
                new DiscordCommandOutput(),
                server.getCommandSource().getPosition(),
                server.getCommandSource().getRotation(),
                server.getCommandSource().getWorld(),
                opLevel,
                server.getCommandSource().getName(),
                server.getCommandSource().getDisplayName(),
                server.getCommandSource().getServer(),
                server.getCommandSource().getEntity()
        );
    }

    public static Member getMember(String id) {
        User user = jda.retrieveUserById(id).complete();
        return guild == null ? null : guild.retrieveMember(user).complete();
    }
}
