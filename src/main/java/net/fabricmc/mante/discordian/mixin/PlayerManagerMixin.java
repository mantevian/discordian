package net.fabricmc.mante.discordian.mixin;

import net.dv8tion.jda.api.entities.TextChannel;
import net.fabricmc.mante.discordian.DiscordUtil;
import net.fabricmc.mante.discordian.Discordian;
import net.minecraft.network.MessageType;
import net.minecraft.server.PlayerManager;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
import java.util.regex.Pattern;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
    @Inject(method = "broadcastChatMessage", at = @At(value = "HEAD"), cancellable = true)
    public void broadcastChatMessage(Text message, MessageType type, UUID senderUuid, CallbackInfo ci) {
        String text = message.getString();

        Pattern patternDefault = Pattern.compile(".+ » .+", Pattern.DOTALL);
        Pattern patternReply = Pattern.compile("┌ .+ ».+", Pattern.DOTALL);

        if (text.isEmpty() || patternDefault.matcher(text).matches() || patternReply.matcher(text).matches() || DiscordUtil.isBlacklistedMessageMTD(text))
            return;

        TextChannel channel = Discordian.jda.getTextChannelById(Discordian.channelID);

        if (channel == null)
            return;

        if (text.matches("[A-Za-z0-9_]+ has made the advancement (\\[)(.+)(\\])")
                || text.matches("[A-Za-z0-9_]+ has reached the goal (\\[)(.+)(\\])")
                || text.matches("[A-Za-z0-9_]+ has completed the challenge (\\[)(.+)(\\])"))
            DiscordUtil.sendAdvancement(text);

        else if (text.matches("[A-Za-z0-9_]+ joined the game"))
            DiscordUtil.sendJoinMessage(text);

        else if (text.matches("[A-Za-z0-9_]+ left the game"))
            DiscordUtil.sendLeaveMessage(text);

        else
            channel.sendMessage(text).queue();
    }
}
