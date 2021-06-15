package net.fabricmc.mante.discordian;

import com.google.gson.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class DiscordConfig {
    public static final int VERSION = 1;
    public static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void fixConfig() {
        Discordian.config.addProperty("version", VERSION);
        writeConfig();

        for (Map.Entry<String, JsonElement> element : defaultConfig().entrySet()) {
            if (!readConfig().has(element.getKey())) {
                Discordian.config.add(element.getKey(), element.getValue());
                writeConfig();
            }
        }
    }

    public static JsonObject defaultConfig() {
        JsonArray statuses = new JsonArray();
        statuses.add("at {tps} TPS");
        statuses.add("with {players} player(s)");

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("version", VERSION);
        jsonObject.addProperty("token", "");
        jsonObject.addProperty("channel_id", "");
        jsonObject.addProperty("start_message", ":white_check_mark: **Server has started!**");
        jsonObject.addProperty("stop_message", ":stop_sign: **Server has stopped!**");
        jsonObject.addProperty("reload_message", ":recycle: **Server has been reloaded!**");
        jsonObject.addProperty("reload_fail_message", ":warning: **A reload attempt has failed!**");
        jsonObject.addProperty("send_advancements", true);
        jsonObject.addProperty("send_player_joins", true);
        jsonObject.addProperty("send_player_leaves", true);
        jsonObject.addProperty("send_player_deaths", true);
        jsonObject.addProperty("send_tellraw", true);
        jsonObject.addProperty("send_replies", true);
        jsonObject.addProperty("op_role_id", "");
        jsonObject.addProperty("op_level", 4);
        jsonObject.add("blacklisted_minecraft_to_discord", new JsonArray());
        jsonObject.add("blacklisted_discord_to_minecraft", new JsonArray());
        jsonObject.add("statuses", statuses);
        return jsonObject;
    }

    public static JsonObject readConfig() {
        try {
            Path path = Paths.get("config", "mante", "discordian.json");
            if (Files.notExists(path)) {
                Files.createDirectories(path.getParent());
                Files.createFile(path);
                Files.writeString(path, gson.toJson(defaultConfig()));
            }
            String read = Files.readString(path);
            JsonParser jsonParser = new JsonParser();
            return jsonParser.parse(read).getAsJsonObject();
        } catch (IOException e) {
            Discordian.logger.error("Can't load configurations for Discord Mod!");
            e.printStackTrace();
            return new JsonObject();
        }
    }

    public static void writeConfig() {
        try {
            Path path = Paths.get("config", "mante", "discordian.json");
            if (Files.notExists(path)) {
                Files.createDirectories(path.getParent());
                Files.createFile(path);
                Files.writeString(path, gson.toJson(defaultConfig()));
            } else {
                Files.writeString(path, gson.toJson(Discordian.config));
            }
        } catch (IOException e) {
            Discordian.logger.error("Can't save configurations for Discord Mod!");
            e.printStackTrace();
        }
    }
}
