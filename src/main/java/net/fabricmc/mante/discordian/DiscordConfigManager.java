package net.fabricmc.mante.discordian;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class DiscordConfigManager {
    private final int VERSION = 5;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public JsonObject config;

    public DiscordConfigManager() {
        config = readConfig();
    }

    public void fixConfig() {
        config = readConfig();

        switch (config.get("version").getAsInt()) {
            case 1, 2 -> {
                config.remove("reload_message");
                config.remove("reload_fail_message");
            }
            case 3 -> {
                config.remove("op_level");
            }
            case 4 -> {
                config.remove("op_role_id");
            }
        }

        config.addProperty("version", VERSION);

        for (Map.Entry<String, JsonElement> element : defaultConfig().entrySet())
            if (!config.has(element.getKey()))
                config.add(element.getKey(), element.getValue());

        writeConfig();
    }

    public JsonObject defaultConfig() {
        JsonArray statuses = new JsonArray();
        statuses.add("at {tps} TPS");
        statuses.add("with {players} player(s)");

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("version", VERSION);
        jsonObject.addProperty("token", "");
        jsonObject.addProperty("channel_id", "");
        jsonObject.addProperty("use_webhook", false);
        jsonObject.addProperty("webhook_url", "");
        jsonObject.addProperty("channel_id", "");
        jsonObject.addProperty("start_message", "# :white_check_mark: **Server has started!**");
        jsonObject.addProperty("stop_message", "# :stop_sign: **Server has stopped!**");
        jsonObject.addProperty("send_advancements", true);
        jsonObject.addProperty("send_player_joins", true);
        jsonObject.addProperty("send_player_leaves", true);
        jsonObject.addProperty("send_player_deaths", true);
        jsonObject.addProperty("send_tellraw", true);
        jsonObject.addProperty("send_replies", true);
        jsonObject.add("discord_operators", new JsonObject());
        jsonObject.add("blacklisted_minecraft_to_discord", new JsonArray());
        jsonObject.add("blacklisted_discord_to_minecraft", new JsonArray());
        jsonObject.add("statuses", statuses);
        jsonObject.add("linked_accounts", new JsonObject());
        jsonObject.addProperty("format", "<${name}> ${message}");
        jsonObject.addProperty("require_link_to_play", false);
        return jsonObject;
    }

    public JsonObject readConfig() {
        try {
            Path path = Paths.get("config", "mantevian", "discordian.json");
            if (Files.notExists(path)) {
                Files.createDirectories(path.getParent());
                Files.createFile(path);
                Files.writeString(path, gson.toJson(defaultConfig()));
            }
            String read = Files.readString(path);
            return JsonParser.parseString(read).getAsJsonObject();
        } catch (IOException e) {
            Discordian.logger.error("Can't load configurations for Discord Mod!");
            e.printStackTrace();
            return new JsonObject();
        }
    }

    public void writeConfig() {
        try {
            Path path = Paths.get("config", "mantevian", "discordian.json");
            if (Files.notExists(path)) {
                Files.createDirectories(path.getParent());
                Files.createFile(path);
                Files.writeString(path, gson.toJson(defaultConfig()));
            } else {
                Files.writeString(path, gson.toJson(Discordian.configManager.config));
            }
        } catch (IOException e) {
            Discordian.logger.error("Can't save configurations for Discord Mod!");
            e.printStackTrace();
        }
    }

    public void updateConfig() {
        fixConfig();
        config = readConfig();
    }

    public void addLinkedAccount(String uuid, String id) {
        config.get("linked_accounts").getAsJsonObject().addProperty(uuid, id);
        writeConfig();
    }

    public void removeLinkedAccount(String uuid) {
        config.get("linked_accounts").getAsJsonObject().remove(uuid);
        writeConfig();
    }
}
