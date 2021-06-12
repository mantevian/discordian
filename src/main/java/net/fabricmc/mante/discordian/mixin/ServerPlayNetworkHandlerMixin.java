package net.fabricmc.mante.discordian.mixin;

import net.dv8tion.jda.api.entities.TextChannel;
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
    @Shadow public ServerPlayerEntity player;

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Ljava/util/function/Function;Lnet/minecraft/network/MessageType;Ljava/util/UUID;)V"), method = "handleMessage", cancellable = true)
    private void handleMessage(TextStream.Message message, CallbackInfo ci) {
        String string = message.getRaw();
        String msg = StringUtils.normalizeSpace(string);
        Text text = new TranslatableText("chat.type.text", this.player.getDisplayName(), msg);

        TextChannel channel = Discordian.jda.getTextChannelById(Discordian.channelID);
        if (channel == null)
            return;

        channel.sendMessage(text.getString()).queue();
    }
}
