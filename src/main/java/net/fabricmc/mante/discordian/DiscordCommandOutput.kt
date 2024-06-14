package net.fabricmc.mante.discordian

import net.minecraft.server.command.CommandOutput
import net.minecraft.text.Text

class DiscordCommandOutput : CommandOutput {
	override fun sendMessage(message: Text) {
		Discordian.channel?.let {
			Discordian.kord.sendMessage(it, message.string)
		}
	}

	override fun shouldReceiveFeedback(): Boolean {
		return true
	}

	override fun shouldTrackOutput(): Boolean {
		return true
	}

	override fun shouldBroadcastConsoleToOps(): Boolean {
		return true
	}
}
