package net.fabricmc.mante.discordian

import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import net.minecraft.text.Text
import net.minecraft.text.TextColor
import net.minecraft.util.Formatting
import net.minecraft.world.GameMode
import java.util.*

class AccountLinkManager {
	var pending: ArrayList<AccountLinkDetails> = ArrayList()

	fun codeAlreadyExists(code: String): Boolean {
		for (details in pending) {
			if (details.code == code) {
				return true
			}
		}
		return false
	}

	private val fourDigitSequence: String
		get() = java.lang.String.valueOf(Discordian.random.nextInt(1000, 10000))

	fun generateCode(): String {
		var code: String
		do {
			code = fourDigitSequence
		} while (codeAlreadyExists(code))
		return code
	}

	fun addPending(uuid: String, code: String) {
		pending.add(AccountLinkDetails(uuid, code))
	}

	fun removePending(code: String) {
		pending.removeIf { accountLinkDetails: AccountLinkDetails -> accountLinkDetails.code == code }
	}

	fun link(id: String, code: String) {
		val account = pending.find { it.code == code } ?: return

		Config.inst().addLinkedAccount(account.uuid, id)
		removePending(code)

		Discordian.kordScope.launch {
			val player =
				Discordian.server.playerManager.getPlayer(UUID.fromString(account.uuid)) ?: return@launch
			val member =
				Discordian.channel?.getGuild()?.let { Discordian.kord.getMember(id, it) } ?: return@launch

			player.sendMessage(
				Text.literal("Your account has been linked to ").styled {
					it.withColor(TextColor.fromFormatting(Formatting.GREEN))
				}
					.append(Text.literal(member.username).styled {
						it.withColor(TextColor.fromFormatting(Formatting.YELLOW))
					}),
				false
			)
			if (Config.inst().requireLinkToPlay) {
				player.changeGameMode(GameMode.DEFAULT)
				val spawnWorld =
					Discordian.server.getWorld(player.spawnPointDimension) ?: Discordian.server.overworld
				val spawnPos = player.spawnPointPosition ?: Discordian.server.overworld.spawnPos

				player.teleport(
					spawnWorld,
					spawnPos.x.toDouble(),
					spawnPos.y.toDouble(),
					spawnPos.z.toDouble(),
					0.0f,
					0.0f
				)
			}
		}
	}
}