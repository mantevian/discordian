package net.fabricmc.mante.discordian;

import com.google.gson.JsonElement;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;

public class DiscordUtil {
    public static void sendAdvancement(String text, String description) {
        if (Discordian.config.get("send_advancements").getAsBoolean())
            sendEmbedWithFooter(text, description, Color.CYAN);
    }

    public static void sendJoinMessage(String text) {
        if (Discordian.config.get("send_player_joins").getAsBoolean())
            sendEmbed(text, Color.GREEN);
    }

    public static void sendLeaveMessage(String text) {
        if (Discordian.config.get("send_player_leaves").getAsBoolean())
            sendEmbed(text, Color.YELLOW);
    }

    public static void sendDeathMessage(String text) {
        if (Discordian.config.get("send_player_deaths").getAsBoolean())
            sendEmbed(text, Color.RED);
    }

    private static void sendEmbed(String text, Color color) {
        EmbedBuilder builder = new EmbedBuilder()
                .setDescription(text)
                .setColor(color);

        Discordian.channel.sendMessage(builder.build()).queue();
    }

    private static void sendEmbedWithFooter(String text, String footer, Color color) {
        EmbedBuilder builder = new EmbedBuilder()
                .setDescription(text)
                .setColor(color)
                .setFooter(footer);

        Discordian.channel.sendMessage(builder.build()).queue();
    }

    private static boolean configArrayContainsSubstring(String key, String value) {
        for (JsonElement element : Discordian.config.get(key).getAsJsonArray()) {
            if (value.contains(element.getAsString()))
                return true;
        }

        return false;
    }

    public static boolean isBlacklistedMessageMTD(String text) {
        return configArrayContainsSubstring("blacklisted_minecraft_to_discord", text);
    }

    public static boolean isBlacklistedMessageDTM(String text) {
        return configArrayContainsSubstring("blacklisted_discord_to_minecraft", text);
    }

    public static int getDecColor(String color) {
        switch (color) {
            case "black" -> color = "#000001";
            case "dark_blue" -> color = "#0000AA";
            case "dark_green" -> color = "#00AA00";
            case "dark_aqua" -> color = "#00AAAA";
            case "dark_red" -> color = "#AA0000";
            case "dark_purple" -> color = "#AA00AA";
            case "gold" -> color = "#FFAA00";
            case "gray" -> color = "#AAAAAA";
            case "dark_gray" -> color = "#555555";
            case "blue" -> color = "#5555FF";
            case "green" -> color = "#55FF55";
            case "aqua" -> color = "#55FFFF";
            case "red" -> color = "#FF5555";
            case "light_purple" -> color = "#FF55FF";
            case "yellow" -> color = "#FFFF55";
            case "white" -> color = "#FFFFFF";
        }

        return Integer.parseInt(color.substring(1, 7), 16);
    }

    public static double avgTick() {
        long[] ticks = Discordian.server.lastTickLengths;
        double avgTick = 0;
        for (long l : ticks)
            avgTick += l;
        avgTick /= ticks.length;
        avgTick /= 1000000;

        return avgTick;
    }
}
