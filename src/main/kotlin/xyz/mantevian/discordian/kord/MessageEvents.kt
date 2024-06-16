package xyz.mantevian.discordian.kord

import dev.kord.common.Color
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.MessageUpdateEvent
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import xyz.mantevian.discordian.Config
import xyz.mantevian.discordian.Discordian
import xyz.mantevian.discordian.Discordian.discordCommandSource
import xyz.mantevian.discordian.kord.KordManager.Utils.getEffectiveColor
import net.minecraft.text.*
import net.minecraft.util.Formatting
import org.apache.logging.log4j.Level

object MessageEvents {
	private fun coloredText(text: String, color: TextColor, bold: Boolean): MutableText {
		return Text.literal(text).styled {
			it.withColor(color).withBold(bold)
		}
	}

	private fun tooltipText(text: String, tooltip: String, color: TextColor): MutableText {
		return Text.literal(text).styled {
			it
				.withColor(color)
				.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(tooltip)))
		}
	}

	private fun clickableText(text: String, tooltip: String, color: TextColor, clickEvent: ClickEvent): MutableText {
		return Text.literal(text).styled {
			it
				.withColor(color)
				.withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(tooltip)))
				.withClickEvent(clickEvent)
		}
	}

	private fun arrowText(): MutableText {
		return coloredText(" » ", TextColor.fromFormatting(Formatting.GRAY)!!, false)
	}

	private fun replyText(): MutableText {
		return coloredText("┌ ", TextColor.fromFormatting(Formatting.GRAY)!!, false)
	}

	private fun multiplyColor(color: Int, m: Double): Int {
		var r = color and 0xff0000
		var g = color and 0x00ff00
		var b = color and 0x0000ff
		r = (r * m).toInt()
		g = (g * m).toInt()
		b = (b * m).toInt()
		r = r and 0xFF0000
		g = g and 0x00FF00
		return r + g + b
	}

	private suspend fun arrowMessage(message: Message, color: Int, reply: Boolean, edited: Boolean): MutableText {
		val name = message.getAuthorAsMemberOrNull()?.effectiveName ?: message.author?.username ?: "unknown"

		val text = Text.literal("")
		var content = message.content

		if (reply) {
			text.append(replyText())

			content = content.replace("\n", "")

			if (content.length > 60) content = content.substring(0, 59) + " ..."
		}

		text.append(
			clickableText(
				name, (message.author?.username ?: "unknown") + " (click to mention)", TextColor.fromRgb(color),
				ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "<@" + message.author?.id + ">")
			)
		)
			.append(arrowText())
			.append(
				tooltipText(
					content,
					message.timestamp.toLocalDateTime(TimeZone.currentSystemDefault()).toString(),
					if (reply) {
						TextColor.fromFormatting(Formatting.GRAY)!!
					} else {
						TextColor.fromFormatting(Formatting.WHITE)!!
					}
				)
			)
		if (edited) text.append(coloredText(" [edited]", TextColor.fromFormatting(Formatting.GRAY)!!, false))
		return text
	}

	private fun send(message: Text?) {
		Discordian.server.playerManager.broadcast(message, false)
	}

	suspend fun onMessageCreated(event: MessageCreateEvent) {
		if (event.guildId == null) {
			val content = event.message.content
			for (account in Discordian.accountLinkManager.pending) {
				if (account.code == content) {
					Discordian.accountLinkManager.link(event.message.author?.id.toString(), content)
					event.message.reply {
						this.content = "Account confirmed successfully!"
					}
					return
				}
			}
			return
		}

		if (event.member == null) return
		if (event.message.channel != Discordian.channel) return
		if (event.message.author?.isBot == true) return

		val member = event.member!!
		val content = event.message.content
		val displayName = member.effectiveName

		val color = member.getEffectiveColor()

		if (content.isNotEmpty()) {
			if (content.startsWith("/")) {
				handleCommands(event.message)
				return
			}

			event.message.referencedMessage?.let { ref ->
				if (!Config.inst().sendReplies) {
					return
				}

				val replyColor = ref.getAuthorAsMemberOrNull()?.getEffectiveColor()
				send(arrowMessage(ref, multiplyColor(replyColor?.rgb ?: 16777215, 0.75), reply = true, edited = false))
			}

			send(arrowMessage(event.message, color.rgb, reply = false, edited = false))
		}

		val attachments = event.message.attachments.toList()

		if (attachments.isNotEmpty()) {
			var info = "Attachment"

			if (attachments[0].isImage) info = "Image"
			if (attachments[0].isSpoiler) info += " (Spoiler)"

			send(
				clickableText(
					displayName, event.message.author?.username + " (click to mention)", TextColor.fromRgb(color.rgb),
					ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "<@" + event.message.author?.id + ">")
				)
					.append(arrowText())
					.append(Text.literal(info).styled {
						it
							.withColor(TextColor.fromFormatting(Formatting.BLUE))
							.withBold(false)
							.withFormatting(Formatting.UNDERLINE)
							.withHoverEvent(
								HoverEvent(
									HoverEvent.Action.SHOW_TEXT,
									Text.literal("Click to open")
								)
							)
							.withClickEvent(
								ClickEvent(
									ClickEvent.Action.OPEN_URL,
									attachments[0].url
								)
							)
					})
			)
		}
	}

	suspend fun onMessageUpdated(event: MessageUpdateEvent) {
		if (event.getMessageOrNull() == null) return
		if (event.getMessage().getAuthorAsMemberOrNull() == null) return
		if (event.channel != Discordian.channel) return
		if (event.getMessage().author?.isBot == true) return

		val message = event.getMessage()
		val member = message.getAuthorAsMember()
		val content = message.content
		val color = member.getEffectiveColor()

		if (content.isNotEmpty()) {
			if (content.startsWith("/")) {
				handleCommands(event.getMessage())
				return
			}

			message.referencedMessage?.let { ref ->
				if (!Config.inst().sendReplies) {
					return
				}

				val replyColor = ref.getAuthorAsMemberOrNull()?.getEffectiveColor() ?: Color(255, 255, 255)
				send(
					arrowMessage(
						ref,
						multiplyColor(replyColor.rgb, 0.75),
						reply = true,
						edited = false
					)
				)
			}
			send(arrowMessage(event.getMessage(), color.rgb, reply = false, edited = true))
		}
	}

	private fun handleCommands(message: Message) {
		val id = message.author?.id
		val level = Config.inst().discordOperators[id.toString()] ?: 0
		Discordian.server.commandManager.executeWithPrefix(discordCommandSource(level), message.content)
		Discordian.logger.log(
			Level.INFO,
			(message.author?.username ?: "unknown") + " executed command: " + message.content
		)
	}
}
