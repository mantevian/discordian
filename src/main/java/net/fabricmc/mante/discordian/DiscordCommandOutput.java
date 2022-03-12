package net.fabricmc.mante.discordian;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.text.Text;

import java.awt.*;
import java.util.UUID;

public class DiscordCommandOutput implements CommandOutput {
    @Override
    public void sendSystemMessage(Text message, UUID senderUuid) {
        TextChannel channel = Discordian.channel;

        if (channel == null)
            return;

        channel.sendMessageEmbeds(new EmbedBuilder()
                .setDescription(message.getString())
                .setColor(Color.getHSBColor(0, 0, 0))
                .build()).queue();
    }

    @Override
    public boolean shouldReceiveFeedback() {
        return true;
    }

    @Override
    public boolean shouldTrackOutput() {
        return true;
    }

    @Override
    public boolean shouldBroadcastConsoleToOps() {
        return true;
    }
}
