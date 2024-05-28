package net.fabricmc.mante.discordian;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.google.gson.JsonArray;
import eu.pb4.placeholders.api.TextParserUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.mante.discordian.event.MessageListener;
import net.fabricmc.mante.discordian.event.ReadyEventListener;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.security.auth.login.LoginException;
import java.util.Random;

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
    public static final Random RANDOM = new Random();


    @Override
    public void onInitializeServer() {
        configManager = new DiscordConfigManager();
        configManager.updateConfig();
        accountLinkManager = new AccountLinkManager();
        DiscordCommand.load();

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

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();

            DiscordUtil.sendJoinMessage(Text.translatable("multiplayer.player.joined", player.getDisplayName()).getString());

            if (Discordian.configManager.config.get("require_link_to_play").getAsBoolean()) {
                if (!accountLinkManager.isLinked(player.getUuidAsString())) {
                    player.sendMessage(
                            Text.literal("This server requires linking Minecraft accounts to Discord accounts to play. Please run ")
                                    .styled(s -> s.withColor(TextColor.fromFormatting(Formatting.WHITE)))
                                    .append(Text.literal("/discord link").styled(s -> s.withColor(TextColor.fromFormatting(Formatting.AQUA))))
                                    .append(Text.literal(" and follow the next instructions").styled(s -> s.withColor(TextColor.fromFormatting(Formatting.WHITE))))
                    );
                }
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (Discordian.configManager.config.get("require_link_to_play").getAsBoolean()) {
                for (var player : server.getPlayerManager().getPlayerList()) {
                    if (!accountLinkManager.isLinked(player.getUuidAsString())) {
                        player.changeGameMode(GameMode.SPECTATOR);
                        player.teleport(server.getOverworld(), 0.0, -100.0, 0.0, 0.0f, 0.0f);
                    }
                }
            }
        });

        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            String raw = StringUtils.normalizeSpace(message.getContent().getString()).replaceAll("@everyone", "`@everyone`").replaceAll("@here", "`@here`");

            if (DiscordUtil.isBlacklistedMessageMTD(raw))
                return;

            Text displayName = sender.getDisplayName();
            String name = "unknown";
            if (displayName != null) {
                name = displayName.getString();
            }
            String format = Discordian.configManager.config.get("format").getAsString()
                    .replaceAll("\\$\\{name}", name)
                    .replaceAll("\\$\\{message}", raw);

            Text text = TextParserUtils.formatText(format);

            Discordian.server.logChatMessage(text, params, "");

            if (Discordian.useWebhook && Discordian.accountLinkManager.isLinked(sender.getUuidAsString())) {
                WebhookMessageBuilder wmb = new WebhookMessageBuilder();

                Member member = Discordian.getMember(Discordian.accountLinkManager.uuidToId(sender.getUuidAsString()));
                if (member != null) {
                    wmb.setAvatarUrl(member.getEffectiveAvatarUrl());
                    wmb.setUsername(member.getEffectiveName());
                    wmb.setContent(raw);
                    Discordian.webhook.send(wmb.build());
                }
            } else
                Discordian.channel.sendMessage(text.getString()).queue();
        });
    }


    public static int load() {
        try {
            channel = jda.getTextChannelById(configManager.config.get("channel_id").getAsString());
        } catch (NumberFormatException e) {
            logger.error("Couldn't parse the provided channel ID.");
            return 0;
        }

        if (channel == null) {
            logger.error("Couldn't find the Discord channel. Make sure you've provided the correct ID.");
            return 0;
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
        return 1;
    }


    public static ServerCommandSource discordCommandSource(int opLevel) {
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
