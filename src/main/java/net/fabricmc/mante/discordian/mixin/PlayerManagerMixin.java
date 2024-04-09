package net.fabricmc.mante.discordian.mixin;

import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import eu.pb4.placeholders.api.TextParserUtils;
import net.dv8tion.jda.api.entities.Member;
import net.fabricmc.mante.discordian.DiscordUtil;
import net.fabricmc.mante.discordian.Discordian;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {
    @Inject(method = "onPlayerConnect", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Z)V", shift = At.Shift.AFTER))
    private void onPlayerConnect(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci) {
        DiscordUtil.sendJoinMessage(Text.translatable("multiplayer.player.joined", player.getDisplayName()).getString());
    }

    @Inject(method = "broadcast(Lnet/minecraft/network/message/SignedMessage;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/network/message/MessageType$Parameters;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/network/message/SignedMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/network/message/MessageType$Parameters;)V"))
    private void sendChatMessage(SignedMessage message, ServerPlayerEntity sender, MessageType.Parameters params, CallbackInfo ci) {
        String raw = StringUtils.normalizeSpace(message.getContent().getString()).replaceAll("@everyone", "`@everyone`").replaceAll("@here", "`@here`");

        if (DiscordUtil.isBlacklistedMessageMTD(raw))
            return;

        Text displayName = sender.getDisplayName();
        String name = "unknown";
        if (displayName != null) {
            name = displayName.getString();
        }
        String format = Discordian.configManager.config.get("format").getAsString()
                .replaceAll("\\$\\{name}", name)
                .replaceAll("\\$\\{message}", raw);

        Text text = TextParserUtils.formatText(format);

        Discordian.server.logChatMessage(text, params, "");

        if (Discordian.useWebhook && Discordian.accountLinkManager.isLinked(sender.getUuidAsString())) {
            WebhookMessageBuilder wmb = new WebhookMessageBuilder();

            Member member = Discordian.getMember(Discordian.accountLinkManager.uuidToId(sender.getUuidAsString()));
            if (member != null) {
                wmb.setAvatarUrl(member.getEffectiveAvatarUrl());
                wmb.setUsername(member.getEffectiveName());
                wmb.setContent(raw);
                Discordian.webhook.send(wmb.build());
            }
        } else
            Discordian.channel.sendMessage(text.getString()).queue();
    }
}
