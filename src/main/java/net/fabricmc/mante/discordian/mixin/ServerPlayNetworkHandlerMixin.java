package net.fabricmc.mante.discordian.mixin;

import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.fabricmc.mante.discordian.DiscordUtil;
import net.fabricmc.mante.discordian.Discordian;
import net.minecraft.server.filter.TextStream;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    @Shadow
    public ServerPlayerEntity player;

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Ljava/util/function/Function;Lnet/minecraft/network/MessageType;Ljava/util/UUID;)V"), method = "handleMessage")
    private void handleMessage(TextStream.Message message, CallbackInfo ci) {
        String raw = StringUtils.normalizeSpace(message.getRaw()).replaceAll("@everyone", "`@everyone`").replaceAll("@here", "`@here`");
        String text = new TranslatableText("chat.type.text", this.player.getDisplayName(), raw).getString();

        if (DiscordUtil.isBlacklistedMessageMTD(raw))
            return;

        if (Discordian.useWebhook && Discordian.accountLinkManager.isLinked(this.player.getUuidAsString())) {
            WebhookMessageBuilder wmb = new WebhookMessageBuilder();

            Member member = Discordian.getMember(Discordian.accountLinkManager.uuidToId(this.player.getUuidAsString()));
            if (member != null) {
                wmb.setAvatarUrl(member.getEffectiveAvatarUrl());
                wmb.setUsername(member.getEffectiveName());
                wmb.setContent(raw);
                Discordian.webhook.send(wmb.build());
                return;
            }
        }

        Discordian.channel.sendMessage(text).queue();
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Lnet/minecraft/network/MessageType;Ljava/util/UUID;)V"), method = "onDisconnected", cancellable = true)
    private void onDisconnected(Text reason, CallbackInfo ci) {
        DiscordUtil.sendLeaveMessage(new TranslatableText("multiplayer.player.left", this.player.getDisplayName()).getString());
    }
}
