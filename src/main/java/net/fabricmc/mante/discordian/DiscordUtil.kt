package net.fabricmc.mante.discordian

import java.awt.Color

object DiscordUtil {
	fun sendAdvancement(text: String, description: String) {
		if (Config.inst().sendAdvancements) {
			Discordian.channel?.let {
				Discordian.kord.sendEmbed(it, text, Color.CYAN, description)
			}
		}
	}

	fun sendJoinMessage(text: String) {
		if (Config.inst().sendPlayerConnections) {
			Discordian.channel?.let {
				Discordian.kord.sendEmbed(it, text, Color.GREEN)
			}
		}
	}

	fun sendLeaveMessage(text: String) {
		if (Config.inst().sendPlayerConnections) {
			Discordian.channel?.let {
				Discordian.kord.sendEmbed(it, text, Color.YELLOW)
			}
		}
	}

	fun sendDeathMessage(text: String) {
		if (Config.inst().sendPlayerDeaths) {
			Discordian.channel?.let {
				Discordian.kord.sendEmbed(it, text, Color.RED)
			}
		}
	}

	fun getDecColor(colorName: String): Int {
		val color = when (colorName) {
			"black" -> "000001"
			"dark_blue" -> "0000AA"
			"dark_green" -> "00AA00"
			"dark_aqua" -> "00AAAA"
			"dark_red" -> "AA0000"
			"dark_purple" -> "AA00AA"
			"gold" -> "FFAA00"
			"gray" -> "AAAAAA"
			"dark_gray" -> "555555"
			"blue" -> "5555FF"
			"green" -> "55FF55"
			"aqua" -> "55FFFF"
			"red" -> "FF5555"
			"light_purple" -> "FF55FF"
			"yellow" -> "FFFF55"
			"white" -> "FFFFFF"
			else -> "000001"
		}

		return Integer.parseInt(color, 16)
	}


	fun avgTick(): Int {
		val ticks = Discordian.server.tickTimes
		var avgTick = 0L

		for (l in ticks) {
			avgTick += l
		}

		avgTick /= ticks.size
		avgTick /= 1000000

		return avgTick.toInt()
	}
}