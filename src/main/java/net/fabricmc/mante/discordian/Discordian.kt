package net.fabricmc.mante.discordian

import club.minnced.discord.webhook.WebhookClient
import club.minnced.discord.webhook.send.WebhookMessageBuilder
import dev.kord.core.entity.channel.TextChannel
import eu.pb4.placeholders.api.TextParserUtils
import kotlinx.coroutines.*
import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.mante.discordian.DiscordUtil.sendLeaveMessage
import net.fabricmc.mante.discordian.kord.KordManager
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.text.TextColor
import net.minecraft.util.Formatting
import net.minecraft.util.math.MathHelper
import net.minecraft.world.GameMode
import org.apache.commons.lang3.StringUtils
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.random.Random
import kotlin.random.asJavaRandom

object Discordian : DedicatedServerModInitializer {

	lateinit var server: MinecraftServer
	var kord = KordManager()
	var channel: TextChannel? = null
	var webhook: WebhookClient? = null

	val logger: Logger = LogManager.getLogger("Discord")
	val accountLinkManager: AccountLinkManager = AccountLinkManager()

	val random = Random.asJavaRandom()

	@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
	val kordScope = CoroutineScope(newSingleThreadContext("kord"))

	override fun onInitializeServer() {
		Config.HANDLER.load()
		DiscordCommand.load()

		kordScope.launch {
			kord.start(Config.inst().token)
		}

		ServerLifecycleEvents.SERVER_STARTED.register { s ->
			server = s
			load()

			kordScope.launch {
				channel = kord.getChannel(Config.inst().channelId) as TextChannel

				channel?.let { c ->
					kord.sendMessage(
						c,
						Config.inst().startMessage.replace("{world}", s.saveProperties.levelName)
					)
				}
			}
		}

		ServerLifecycleEvents.SERVER_STOPPED.register { s ->
			channel?.let {
				kord.sendMessage(it, Config.inst().stopMessage)
			}
		}

		ServerTickEvents.END_SERVER_TICK.register {
			for (i in 0..<Config.inst().statuses.size) {
				var str = Config.inst().statuses[i]
				str = str.replace("{players}", it.currentPlayerCount.toString())
				str = str.replace(
					"{tps}",
					(Math.round(
						MathHelper.clamp(
							1000.0 / DiscordUtil.avgTick(),
							0.0,
							20.0
						) * 10.0
					) / 10.0).toString()
				)

				if (it.ticks % (20 * 20 * Config.inst().statuses.size) == 20 * 20 * i) {
					kord.setPlaying(str)
				}
			}
		}

		ServerPlayConnectionEvents.JOIN.register { handler, sender, server ->
			val player = handler.getPlayer()

			if (Config.inst().sendPlayerConnections) {
				DiscordUtil.sendJoinMessage(
					Text.translatable("multiplayer.player.joined", player.displayName).string
				)
			}

			if (Config.inst().requireLinkToPlay) {
				if (!Config.inst().linkedAccounts.contains(player.uuidAsString)) {
					player.sendMessage(
						Text.literal("This server requires linking Minecraft accounts to Discord accounts to play. Please run ")
							.styled { it.withColor(TextColor.fromFormatting(Formatting.WHITE)) }
							.append(
								Text.literal("/discord link")
									.styled { it.withColor(TextColor.fromFormatting(Formatting.AQUA)) }
							)
							.append(
								Text.literal(" and follow the next instructions")
									.styled { it.withColor(TextColor.fromFormatting(Formatting.WHITE)) }
							)
					)
				}
			}
		}

		ServerPlayConnectionEvents.DISCONNECT.register { handler, server ->
			if (Config.inst().sendPlayerConnections) {
				val player = handler.getPlayer()
				sendLeaveMessage(Text.translatable("multiplayer.player.left", player.displayName).string)
			}
		}

		ServerTickEvents.END_SERVER_TICK.register {
			if (Config.inst().requireLinkToPlay) {
				for (player in it.playerManager.playerList) {
					if (!Config.inst().linkedAccounts.contains(player.uuidAsString)) {
						player.changeGameMode(GameMode.SPECTATOR)
						player.teleport(server.overworld, 0.0, -100.0, 0.0, 0.0f, 0.0f)
					}
				}
			}
		}

		ServerMessageEvents.CHAT_MESSAGE.register { message, sender, params ->
			val raw = StringUtils.normalizeSpace(message.content.string).replace(
				"@everyone",
				"`@everyone`"
			).replace("@here", "`@here`")

			val displayName = sender.displayName
			var name = "unknown"
			if (displayName != null) {
				name = displayName.string
			}
			val format = Config.inst().format
				.replace("{name}", name)
				.replace("{message}", raw)

			val text = TextParserUtils.formatText(format)

			Discordian.kordScope.launch {
				if (Config.inst().useWebhook && Config.inst().linkedAccounts.contains(sender.uuidAsString)) {
					val wmb = WebhookMessageBuilder()

					val member = kord.getMember(
						Config.inst().linkedAccounts[sender.uuidAsString] ?: "",
						channel!!.getGuild()
					)
					member?.let {
						wmb.setAvatarUrl((it.memberAvatar ?: it.avatar ?: it.defaultAvatar).cdnUrl.toUrl())
						wmb.setUsername(it.effectiveName)
						wmb.setContent(raw)
						webhook?.send(wmb.build())
					}
				} else {
					channel?.let {
						kord.sendMessage(it, text.string)
					}
				}
			}
		}
	}

	fun load(): Int {
		Config.HANDLER.load()

		if (Config.inst().useWebhook) {
			try {
				webhook = WebhookClient.withUrl(Config.inst().webhookUrl)
			} catch (e: Exception) {
				logger.error("Couldn't load the webhook. Make sure you have created the webhook in #" + channel?.name + " and provided correct details. Switching to no webhook mode. Details: " + e)
				Config.inst().useWebhook = false
			}
		}

		return 1
	}

	fun discordCommandSource(opLevel: Int): ServerCommandSource {
		val clampedOpLevel = MathHelper.clamp(opLevel, 1, 4)

		return ServerCommandSource(
			DiscordCommandOutput(),
			server.commandSource.position,
			server.commandSource.rotation,
			server.commandSource.world,
			clampedOpLevel,
			server.commandSource.name,
			server.commandSource.displayName,
			server.commandSource.server,
			server.commandSource.entity
		)
	}
}
