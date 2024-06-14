package net.fabricmc.mante.discordian

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.CommandManager.RegistrationEnvironment
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.text.TextColor
import net.minecraft.util.Formatting
import net.minecraft.util.math.MathHelper

object DiscordCommand {
	fun load() {
		CommandRegistrationCallback.EVENT.register(CommandRegistrationCallback { dispatcher: CommandDispatcher<ServerCommandSource?>, dedicated: CommandRegistryAccess?, environment: RegistrationEnvironment? ->
			val discordNode = CommandManager
				.literal("discord")
				.build()
			val discordLinkNode = CommandManager
				.literal("link")
				.executes { link(it) }
				.build()
			val discordUnlinkNode = CommandManager
				.literal("unlink")
				.executes { unlink(it) }
				.build()
			val discordReloadNode = CommandManager
				.literal("reload")
				.requires { req: ServerCommandSource -> req.hasPermissionLevel(1) }
				.executes { reload(it) }
				.build()

			val tpsNode = CommandManager
				.literal("tps")
				.executes { tps(it) }
				.build()

			val playersNode = CommandManager
				.literal("players")
				.executes { players(it) }
				.build()

			dispatcher.root.addChild(discordNode)
			discordNode.addChild(discordLinkNode)
			discordNode.addChild(discordUnlinkNode)
			discordNode.addChild(discordReloadNode)

			dispatcher.root.addChild(tpsNode)
			dispatcher.root.addChild(playersNode)
		})
	}

	private fun reload(context: CommandContext<ServerCommandSource>): Int {
		val source = context.source
		when (Discordian.load()) {
			0 -> {
				source.sendFeedback(
					{
						Text.literal("Something went wrong when reloading the mod")
							.styled { it.withColor(TextColor.fromFormatting(Formatting.RED)) }
					},
					false
				)
				return 0
			}

			1 -> {
				source.sendFeedback(
					{
						Text.literal("Reloaded the mod successfully!")
							.styled { it.withColor(TextColor.fromFormatting(Formatting.GREEN)) }
					},
					false
				)
				return 1
			}
		}
		return 0
	}

	private fun link(context: CommandContext<ServerCommandSource>): Int {
		val source = context.source
		val player = source.player
		if (player == null) {
			source.sendFeedback(
				{
					Text.literal("This command can only be executed by a player")
						.styled { it.withColor(TextColor.fromFormatting(Formatting.RED)) }
				},
				false
			)
			return 0
		}

		val code = Discordian.accountLinkManager.generateCode()
		val uuid = player.uuidAsString

		if (Config.inst().linkedAccounts.contains(uuid)) {
			Discordian.kordScope.launch {
				val member = Discordian.channel?.getGuild()?.let {
					Discordian.kord.getMember(Config.inst().linkedAccounts[uuid] ?: "", it)
				}
				val text: String = member?.username ?: "unknown"
				source.sendFeedback(
					{
						Text.literal("Your account is already linked to $text")
							.styled { it.withColor(TextColor.fromFormatting(Formatting.RED)) }
					},
					false
				)
			}
			return 0
		}

		Discordian.accountLinkManager.addPending(player.uuidAsString, code)

		Discordian.kordScope.launch {
			val selfName = Discordian.kord.getSelf().username

			source.sendFeedback(
				{
					Text.literal("Please DM the bot ")
						.append(
							Text.literal(selfName)
								.styled {
									it.withColor(TextColor.fromFormatting(Formatting.YELLOW))
								}
						)
						.append(Text.literal(" in Discord with the code to verify your account "))
						.append(
							Text.literal("(click to copy code)")
								.styled {
									it
										.withColor(TextColor.fromFormatting(Formatting.AQUA))
										.withUnderline(true)
										.withClickEvent(ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, code))
										.withHoverEvent(
											HoverEvent(
												HoverEvent.Action.SHOW_TEXT,
												Text.literal("Click to copy")
											)
										)
								})
				},
				false
			)
		}

		return 1
	}

	private fun unlink(context: CommandContext<ServerCommandSource>): Int {
		val source = context.source
		val player = source.player
		if (player == null) {
			source.sendFeedback(
				{
					Text.literal("This command can only be executed by a player")
						.styled { it.withColor(TextColor.fromFormatting(Formatting.RED)) }
				},
				false
			)
			return 0
		}
		if (!Config.inst().linkedAccounts.contains(player.uuidAsString)) {
			player.sendMessage(
				Text.literal("Your account is not linked to a Discord account")
					.styled { it.withColor(TextColor.fromFormatting(Formatting.RED)) },
				false
			)
			return 0
		}

		Config.inst().removeLinkedAccount(player.uuidAsString)

		player.sendMessage(Text.literal("Your Discord account has been unlinked").styled {
			it.withColor(
				TextColor.fromFormatting(
					Formatting.GREEN
				)
			)
		}, false)
		return 1
	}

	private fun tps(context: CommandContext<ServerCommandSource>): Int {
		val avgTick = DiscordUtil.avgTick()
		val tps = Math.round((10000 / avgTick).toFloat()) / 10.0
		val effectiveTps = Math.round((MathHelper.clamp(1000 / avgTick, 0, 20) * 10).toFloat()) / 10.0
		val mspt = Math.round((avgTick * 10).toFloat()) / 10.0

		context.source.sendFeedback({
			Text.literal("TPS: $tps ($effectiveTps effective, $mspt MSPT)")
		}, false)

		return 1
	}

	private fun players(context: CommandContext<ServerCommandSource>): Int {
		val players = Discordian.server.playerNames
		val count = players.size
		val text = if (count == 0) {
			"No players online"
		} else {
			"$count player${if (count > 1) "s" else ""} online: ${players.joinToString(", ")}"
		}

		context.source.sendFeedback({
			Text.literal(text)
		}, false)

		return 1
	}
}
