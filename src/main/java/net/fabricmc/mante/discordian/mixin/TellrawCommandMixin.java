package net.fabricmc.mante.discordian.mixin;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.mante.discordian.Config;
import net.fabricmc.mante.discordian.Discordian;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.TextArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.TellRawCommand;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.text.Texts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.util.Collection;
import java.util.Iterator;

@Mixin(TellRawCommand.class)
public class TellrawCommandMixin {
	@Inject(method = "register", at = @At(value = "HEAD"), cancellable = true)
	private static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CallbackInfo ci) {
		dispatcher.register((CommandManager.literal("tellraw").requires((serverCommandSource) -> serverCommandSource.hasPermissionLevel(2))).then(CommandManager.argument("targets", EntityArgumentType.players()).then(CommandManager.argument("message", TextArgumentType.text(commandRegistryAccess)).executes((commandContext) -> {
			if (!Config.Companion.inst().getSendTellraw())
				return 0;

			int i = 0;
			Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(commandContext, "targets");

			Text text = Texts.parse(commandContext.getSource(), TextArgumentType.getTextArgument(commandContext, "message"), players.iterator().next(), 0);

			for (Iterator<ServerPlayerEntity> var2 = players.iterator(); var2.hasNext(); ++i) {
				ServerPlayerEntity serverPlayerEntity = var2.next();
				serverPlayerEntity.sendMessage(Texts.parse(commandContext.getSource(), TextArgumentType.getTextArgument(commandContext, "message"), serverPlayerEntity, 0));
			}

			if (players.containsAll(Discordian.server.getPlayerManager().getPlayerList()) && Discordian.INSTANCE.getChannel() != null) {
				TextColor color = text.getStyle().getColor();
				if (color == null)
					color = TextColor.fromRgb(1);

				Discordian.INSTANCE.getKord().sendEmbed(
						Discordian.INSTANCE.getChannel(),
						text.getString(),
						new Color(color.getRgb()),
						null
				);
			}

			return i;
		}))));
		ci.cancel();
	}
}
