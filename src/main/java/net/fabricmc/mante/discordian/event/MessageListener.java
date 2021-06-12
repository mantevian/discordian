package net.fabricmc.mante.discordian.event;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.fabricmc.mante.discordian.Discordian;
import net.fabricmc.mante.discordian.DiscordUtil;
import net.minecraft.network.MessageType;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.apache.logging.log4j.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MessageListener extends ListenerAdapter {
    String content;
    String name;
    Member member;
    User author;
    Message message;

    MutableText coloredText(String text, TextColor color, boolean bold) {
        return new LiteralText(text).styled(style -> style
                .withColor(color)
                .withBold(bold));
    }

    MutableText tooltipText(String text, String tooltip, TextColor color) {
        return new LiteralText(text).styled(style -> style
                .withColor(color)
                .withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new LiteralText(tooltip))));
    }

    MutableText clickableText(String text, String tooltip, TextColor color, ClickEvent clickEvent) {
        return new LiteralText(text).styled(style -> style
                .withColor(color)
                .withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new LiteralText(tooltip)))
                .withClickEvent(clickEvent)
        );
    }

    MutableText arrowText() {
        return coloredText(" » ", TextColor.parse("gray"), false);
    }

    MutableText replyText() {
        return coloredText("┌ ", TextColor.parse("gray"), false);
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

        MutableText text = new LiteralText("");
        if (reply)
            text.append(replyText());

        String content = message.getContentDisplay();

        if (reply) {
            content = String.join(" ", content.split("\n"));
            if (content.length() > 30)
                content = content.substring(0, 29) + " ...";
        }

        text.append(clickableText(name, message.getAuthor().getAsTag() + " (click to mention)", TextColor.fromRgb(color),
                new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "<@" + message.getAuthor().getId() + ">")))
                .append(arrowText())
                .append(tooltipText(content, message.getTimeCreated().toString(), reply ? TextColor.parse("gray") : TextColor.parse("white")));

        if (edited)
            text.append(coloredText(" [edited]", TextColor.parse("gray"), false));

        return text;
    }

    void send(Text message) {
        Discordian.server.getPlayerManager().broadcastChatMessage(
                message,
                MessageType.CHAT,
                Util.NIL_UUID);
    }

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        if (Discordian.server == null) return;

        if (event.getMember() == null) return;
        if (!event.getChannel().getId().equals(Discordian.channelID)) return;
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
                if (!Discordian.config.get("send_replies").getAsBoolean())
                    return;

                int replyColor = 0xffffff;
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
                    .append(new LiteralText(info).styled(style -> style
                            .withColor(TextColor.parse("blue"))
                            .withBold(false)
                            .withFormatting(Formatting.UNDERLINE)
                            .withHoverEvent(new HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    new LiteralText("Click to open")))
                            .withClickEvent(new ClickEvent(
                                    ClickEvent.Action.OPEN_URL,
                                    atts.get(0).getUrl()))
                    )));
        }
    }

    @Override
    public void onGuildMessageUpdate(@NotNull GuildMessageUpdateEvent event) {
        if (Discordian.server == null) return;

        if (event.getMember() == null) return;
        if (!event.getChannel().getId().equals(Discordian.channelID)) return;
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
            case "tps" -> {
                long[] ticks = Discordian.server.lastTickLengths;
                double avgTick = 0;
                for (long l : ticks)
                    avgTick += l;
                avgTick /= ticks.length;
                avgTick /= 1000000;
                message.getChannel().sendMessage("TPS: " + (Math.round(10000 / avgTick) / 10) + " (" + (Math.round(MathHelper.clamp(1000 / avgTick, 0, 20) * 10) / 10) + " effective, " + (Math.round(avgTick * 10) / 10) + " MSPT)")
                        .reference(message).mentionRepliedUser(false).queue();
            }
            case "players" -> {
                String players = String.join(", ", Discordian.server.getPlayerNames());
                int p = Discordian.server.getPlayerNames().length;
                String text = p + " player" + (p > 1 ? "s" : "") + " online: " + players;
                if (p == 0)
                    text = "No players online";
                message.getChannel().sendMessage(text)
                        .reference(message).mentionRepliedUser(false).queue();
            }
            case "scores" -> {
                ScoreboardObjective objective = Discordian.server.getScoreboard().getObjectiveForSlot(1);
                if (objective == null) {
                    message.getChannel().sendMessage("There is no displayed objective at the time.")
                            .reference(message).mentionRepliedUser(false).queue();
                    return;
                }
                EmbedBuilder embedBuilder = new EmbedBuilder()
                        .setTitle(objective.getName());
                String names = "";
                String scores = "";
                Collection<ScoreboardPlayerScore> playerScores = Discordian.server.getScoreboard().getAllPlayerScores(objective);
                Collections.reverse((List<ScoreboardPlayerScore>) playerScores);
                for (ScoreboardPlayerScore playerScore : playerScores) {
                    if (names.concat(playerScore.getPlayerName() + "\n").length() < 2000) {
                        names = names.concat("`" + playerScore.getPlayerName() + "`\n");
                        scores = scores.concat(playerScore.getScore() + "\n");
                    }
                }
                embedBuilder
                        .addField("Name", names, true)
                        .addField("Score", scores, true);

                message.getChannel().sendMessage(embedBuilder.build())
                        .reference(message).mentionRepliedUser(false).queue();
            }
            default -> {
                if (Discordian.config.get("op_role_id").getAsString().isEmpty())
                    break;
                if (member.getRoles().contains(message.getGuild().getRoleById(Discordian.config.get("op_role_id").getAsString()))) {
                    Discordian.server.getCommandManager().execute(
                            Discordian.discordCommandSource(),
                            content);
                    Discordian.logger.log(Level.INFO, author.getAsTag() + " executed command: " + content);
                } else
                    Discordian.logger.log(Level.INFO, author.getAsTag() + " tried to execute command but didn't have permissions to do so: " + content);
            }
        }
    }
}
