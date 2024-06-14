package net.fabricmc.mante.discordian.kord

import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Member
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.Channel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.MessageUpdateEvent
import dev.kord.core.exception.KordInitializationException
import dev.kord.core.on
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import net.fabricmc.mante.discordian.Discordian
import org.apache.logging.log4j.Level

class KordManager {

	private lateinit var kord: Kord

	@OptIn(PrivilegedIntent::class)
	suspend fun start(token: String) {
		Discordian.logger.log(Level.INFO, "Logging in to Discord...")
		try {
			kord = Kord(token)
		} catch (e: KordInitializationException) {
			Discordian.logger.error("Could not connect to Discord")
			return
		}

		Discordian.logger.log(Level.INFO, "Connected to Discord!")

		kord.on<MessageCreateEvent> {
			MessageEvents.onMessageCreated(this)
		}

		kord.on<MessageUpdateEvent> {
			MessageEvents.onMessageUpdated(this)
		}

		kord.login {
			intents += Intent.GuildPresences
			intents += Intent.GuildMembers
			intents += Intent.MessageContent
		}
	}

	fun sendMessage(channel: TextChannel, content: String) {
		Discordian.kordScope.launch {
			channel.createMessage(content)
		}
	}

	fun setPlaying(content: String) {
		Discordian.kordScope.launch {
			kord.editPresence {
				playing(content)
			}
		}
	}

	suspend fun getChannel(id: String): Channel? {
		return kord.getChannel(Snowflake(id))
	}

	suspend fun getSelf(): User {
		return kord.getSelf(EntitySupplyStrategy.rest)
	}

	suspend fun getMember(id: String, guild: Guild): Member? {
		val user = kord.getUser(Snowflake(id), EntitySupplyStrategy.rest)
		return user?.asMember(guild.id)
	}

	fun sendEmbed(channel: TextChannel, text: String, javaColor: java.awt.Color, footer: String? = null) {
		Discordian.kordScope.launch {
			channel.createEmbed {
				this.description = text
				this.color = Color(javaColor.rgb)

				footer?.let {
					this.footer {
						this.text = footer
					}
				}
			}
		}
	}

	companion object Utils {
		suspend fun Member.getEffectiveColor(): Color {
			return roles.toList(mutableListOf()).sortedByDescending { it.rawPosition }.find { it.color.rgb != 0 }?.color
				?: Color(
					255,
					255,
					255
				)
		}
	}
}