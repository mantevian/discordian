package net.fabricmc.mante.discordian.event;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.fabricmc.mante.discordian.AccountLinkDetails;
import net.fabricmc.mante.discordian.DiscordUtil;
import net.fabricmc.mante.discordian.Discordian;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.apache.logging.log4j.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MessageListener extends ListenerAdapter {
    String content;
    String name;
    Member member;
    User author;
    Message message;

    MutableText coloredText(String text, TextColor color, boolean bold) {
        return Text.literal(text).styled(style -> style
                .withColor(color)
                .withBold(bold));
    }

    MutableText tooltipText(String text, String tooltip, TextColor color) {
        return Text.literal(text).styled(style -> style
                .withColor(color)
                .withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        Text.literal(tooltip))));
    }

    MutableText clickableText(String text, String tooltip, TextColor color, ClickEvent clickEvent) {
        return Text.literal(text).styled(style -> style
                .withColor(color)
                .withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        Text.literal(tooltip)))
                .withClickEvent(clickEvent)
        );
    }

    MutableText arrowText() {
        return coloredText(" » ", TextColor.fromFormatting(Formatting.GRAY), false);
    }

    MutableText replyText() {
        return coloredText("┌ ", TextColor.fromFormatting(Formatting.GRAY), false);
    }

    int multiplyColor(int color, double m) {
        int r = color & 0xff0000;
        int g = color & 0x00ff00;
        int b = color & 0x0000ff;

        r *= m;
        g *= m;
        b *= m;

        r &= 0xFF0000;
        g &= 0x00FF00;

        return r + g + b;
    }

    MutableText arrowMessage(Message message, int color, boolean reply, boolean edited) {
        String name = message.getAuthor().getName();
        if (message.getMember() != null)
            name = message.getMember().getEffectiveName();

        MutableText text = Text.literal("");
        if (reply)
            text.append(replyText());

        String content = message.getContentDisplay();

        if (reply) {
            content = String.join(" ", content.split("\n"));
            if (content.length() > 60)
                content = content.substring(0, 59) + " ...";
        }

        text.append(clickableText(name, message.getAuthor().getAsTag() + " (click to mention)", TextColor.fromRgb(color),
                new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "<@" + message.getAuthor().getId() + ">")))
                .append(arrowText())
                .append(tooltipText(content, message.getTimeCreated().toString(), reply ? TextColor.fromFormatting(Formatting.GRAY) : TextColor.fromFormatting(Formatting.WHITE)));

        if (edited)
            text.append(coloredText(" [edited]", TextColor.fromFormatting(Formatting.GRAY), false));

        return text;
    }

    void send(Text message) {
        Discordian.server.getPlayerManager().broadcast(message, false);
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.isFromGuild()) {
            String content = event.getMessage().getContentStripped();
            for (AccountLinkDetails account : Discordian.accountLinkManager.pending) {
                if (account.code.equals(content)) {
                    Discordian.accountLinkManager.link(event.getAuthor().getId(), content);
                    event.getMessage().reply("Account confirmed successfully!").queue();
                    return;
                }
            }

            return;
        }

        if (Discordian.server == null) return;

        if (event.getMember() == null) return;
        if (!event.getChannel().equals(Discordian.channel)) return;
        if (event.getAuthor().isBot()) return;

        message = event.getMessage();
        member = event.getMember();
        author = event.getAuthor();
        name = author.getName();

        if (member.getNickname() != null) name = member.getNickname();

        content = event.getMessage().getContentDisplay();

        int color = 0xffffff;
        if (member.getColor() != null) color = member.getColor().getRGB() & 0xffffff;

        if (content.length() != 0) {
            if (DiscordUtil.isBlacklistedMessageDTM(content))
                return;

            if (content.startsWith("/")) {
                handleCommands();
                return;
            }

            if (message.getReferencedMessage() != null) {
                if (!Discordian.configManager.config.get("send_replies").getAsBoolean())
                    return;

                int replyColor = 0xffffff;
                System.out.println(message.getReferencedMessage().getMember());
                if (message.getReferencedMessage().getMember() != null)
                    replyColor = message.getReferencedMessage().getMember().getColorRaw();

                replyColor = multiplyColor(replyColor, 0.75);
                send(arrowMessage(message.getReferencedMessage(), replyColor, true, false));
            }

            send(arrowMessage(message, color, false, false));
        }

        List<Message.Attachment> atts = event.getMessage().getAttachments();

        if (!atts.isEmpty()) {
            String info = "Attachment";
            if (atts.get(0).isImage()) info = "Image";
            if (atts.get(0).isVideo()) info = "Video";
            if (atts.get(0).isSpoiler()) info += " (Spoiler)";

            send(clickableText(name, author.getAsTag() + " (click to mention)", TextColor.fromRgb(color),
                    new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "<@" + author.getId() + ">"))
                    .append(arrowText())
                    .append(Text.literal(info).styled(style -> style
                            .withColor(TextColor.fromFormatting(Formatting.BLUE))
                            .withBold(false)
                            .withFormatting(Formatting.UNDERLINE)
                            .withHoverEvent(new HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    Text.literal("Click to open")))
                            .withClickEvent(new ClickEvent(
                                    ClickEvent.Action.OPEN_URL,
                                    atts.get(0).getUrl()))
                    )));
        }
    }

    @Override
    public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
        if (Discordian.server == null) return;

        if (event.getMember() == null) return;
        if (!event.getChannel().equals(Discordian.channel)) return;
        if (event.getAuthor().isBot()) return;

        message = event.getMessage();
        member = event.getMember();
        author = event.getAuthor();
        name = author.getName();

        if (member.getNickname() != null) name = member.getNickname();

        content = event.getMessage().getContentDisplay();

        int color = 0xffffff;
        if (member.getColor() != null) color = member.getColor().getRGB() & 0xffffff;

        if (content.length() != 0) {
            if (DiscordUtil.isBlacklistedMessageDTM(content))
                return;

            if (content.startsWith("/")) {
                handleCommands();
                return;
            }

            if (message.getReferencedMessage() != null) {
                int replyColor = 0xffffff;
                if (message.getReferencedMessage().getMember() != null)
                    replyColor = message.getReferencedMessage().getMember().getColorRaw();

                replyColor = multiplyColor(replyColor, 0.75);
                send(arrowMessage(message.getReferencedMessage(), replyColor, true, false));
            }

            send(arrowMessage(message, color, false, true));
        }
    }

    public void handleCommands() {
        switch (content.split(" ")[0].substring(1)) {
            case "tps" -> message.getChannel().sendMessage("TPS: " + (Math.round(10000 / DiscordUtil.avgTick()) / 10d) + " (" + (Math.round(MathHelper.clamp(1000 / DiscordUtil.avgTick(), 0, 20) * 10) / 10d) + " effective, " + (Math.round(DiscordUtil.avgTick() * 10) / 10d) + " MSPT)")
                    .reference(message).mentionRepliedUser(false).queue();
            case "players" -> {
                String players = String.join(", ", Discordian.server.getPlayerNames());
                int p = Discordian.server.getPlayerNames().length;
                String text = p + " player" + (p > 1 ? "s" : "") + " online: " + players;
                if (p == 0)
                    text = "No players online";
                message.getChannel().sendMessage(text)
                        .reference(message).mentionRepliedUser(false).queue();
            }
            default -> {
                int level = 0;
                String id = message.getAuthor().getId();

                JsonObject obj = Discordian.configManager.config.get("discord_operators").getAsJsonObject();

                if (obj.has(id)) {
                    level = obj.get(id).getAsInt();
                }

                Discordian.server.getCommandManager().executeWithPrefix(Discordian.discordCommandSource(level), content);

                Discordian.logger.log(Level.INFO, author.getAsTag() + " executed command: " + content);
            }
        }
    }
}
