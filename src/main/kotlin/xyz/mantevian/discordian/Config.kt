package xyz.mantevian.discordian

import com.google.gson.GsonBuilder
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler
import dev.isxander.yacl3.config.v2.api.SerialEntry
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.util.Identifier

class Config {
	companion object {
		val HANDLER: ConfigClassHandler<Config> =
			ConfigClassHandler.createBuilder(Config::class.java)
				.id(Identifier.of("mantevian", "discordian"))
				.serializer {
					GsonConfigSerializerBuilder.create(it)
						.setPath(FabricLoader.getInstance().configDir.resolve("mantevian/discordian.json5"))
						.appendGsonBuilder(GsonBuilder::setPrettyPrinting)
						.setJson5(true)
						.build()
				}
				.build()

		fun inst(): Config = HANDLER.instance()
	}

	@SerialEntry(value = "token", comment = "Token for the Discord bot, found in its Discord Developers page")
	val token = ""

	@SerialEntry(value = "channel_id", comment = "Discord channel ID for communication between Discord and Minecraft")
	val channelId = ""

	@SerialEntry(
		value = "use_webhook",
		comment = "Whether a webhook should be used to send fancy messages for players with linked accounts"
	)
	var useWebhook = false

	@SerialEntry(
		value = "webhook_url",
		comment = "URL to a Discord Webhook to use, if use_webhook is enabled. This can be found in the settings of a Discord channel that's going to be used for communication with Minecraft"
	)
	val webhookUrl = ""

	@SerialEntry(
		value = "start_message",
		comment = "Message that gets sent to Discord when the server is started. Use {world} to insert the name of the running world"
	)
	val startMessage = "# :white_check_mark: Server has started on world `{world}`!"

	@SerialEntry(value = "stop_message", comment = "Message that gets sent to Discord when the server is stopped")
	val stopMessage = "# :stop_sign: Server has stopped!"

	@SerialEntry(value = "send_advancements", comment = "Whether to send advancements to Discord")
	val sendAdvancements = true

	@SerialEntry(
		value = "send_player_connections",
		comment = "Whether to send player join and leave messages to Discord"
	)
	val sendPlayerConnections = true

	@SerialEntry(value = "send_player_deaths", comment = "Whether to send player death messages to Discord")
	val sendPlayerDeaths = true

	@SerialEntry(
		value = "send_tellraw",
		comment = "Whether to send messages from /tellraw to Discord (only shows messages that are visible to every player currently online)"
	)
	val sendTellraw = true

	@SerialEntry(
		value = "send_replies",
		comment = "Whether to send Discord messages that are replies to another message to Minecraft"
	)
	val sendReplies = true

	@SerialEntry(
		value = "discord_operators",
		comment = "List of Discord users that have in-game operator permissions when running commands from Discord. To promote a user, it will look like this: \"1234567890\": 4 where 1234567890 is the Discord user ID and 4 is the OP level (read about OP levels on the Minecraft wiki)"
	)
	val discordOperators: MutableMap<String, Int> = mutableMapOf()

	@SerialEntry(
		value = "linked_accounts",
		comment = "List of Minecraft players that are linked to Discord accounts. It's recommended to link through using /discord link, only change this if you want to link someone manually. This field is a map of Minecraft player UUID to Discord user ID"
	)
	val linkedAccounts: MutableMap<String, String> = mutableMapOf()

	@SerialEntry(
		value = "format",
		comment = "The format for transmitting messages from Minecraft to Discord. Use {name} for player name and {message} for their message"
	)
	val format = "<{name}> {message}"

	@SerialEntry(
		value = "statuses",
		comment = "Status messages which the Discord bot will use and switch between. Use {tps} to show the server's current ticks per second and {players} for the amount of players online"
	)
	val statuses = listOf("at {tps} TPS", "with {players} player(s)")

	@SerialEntry(
		value = "require_link_to_play",
		comment = "Whether playing on the server requires a link to Discord. Unlinked players will be restricted from interacting with the world and prompted to link their account using /discord link"
	)
	val requireLinkToPlay = false

	fun addLinkedAccount(uuid: String, id: String) {
		linkedAccounts[uuid] = id
		HANDLER.save()
	}

	fun removeLinkedAccount(uuid: String) {
		linkedAccounts.remove(uuid)
		HANDLER.save()
	}
}
