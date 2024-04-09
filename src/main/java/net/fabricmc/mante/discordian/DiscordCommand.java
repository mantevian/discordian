package net.fabricmc.mante.discordian;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.dv8tion.jda.api.entities.Member;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

public class DiscordCommand {
    public static void load() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, environment) -> {
            LiteralCommandNode<ServerCommandSource> discord_node = CommandManager
                    .literal("discord")
                    .build();

            LiteralCommandNode<ServerCommandSource> discord_link_node = CommandManager
                    .literal("link")
                    .executes(DiscordCommand::link)
                    .build();

            LiteralCommandNode<ServerCommandSource> discord_unlink_node = CommandManager
                    .literal("unlink")
                    .executes(DiscordCommand::unlink)
                    .build();

            LiteralCommandNode<ServerCommandSource> discord_reload_node = CommandManager
                    .literal("reload")
                    .requires(req -> req.hasPermissionLevel(1))
                    .executes(DiscordCommand::reload)
                    .build();

            dispatcher.getRoot().addChild(discord_node);
            discord_node.addChild(discord_link_node);
            discord_node.addChild(discord_unlink_node);
            discord_node.addChild(discord_reload_node);
        });
    }

    private static int reload(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        switch (Discordian.load()) {
            case 0 -> {
                source.sendFeedback(() -> Text.literal("Something went wrong when reloading the mod")
                                .styled(style -> style.withColor(TextColor.fromFormatting(Formatting.RED))),
                        false);
                return 0;
            }
            case 1 -> {
                source.sendFeedback(() -> Text.literal("Reloaded the mod successfully!")
                                .styled(style -> style.withColor(TextColor.fromFormatting(Formatting.GREEN))),
                        false);
                return 1;
            }
        }

        return 0;
    }

    private static int link(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (player == null) {
            source.sendFeedback(() ->
                    Text.literal("This command can only be executed by a player")
                            .styled(style -> style.withColor(TextColor.fromFormatting(Formatting.RED))),
                    false);
            return 0;
        }

        String code = Discordian.accountLinkManager.generateCode();
        String uuid = player.getUuidAsString();

        if (Discordian.accountLinkManager.isLinked(uuid)) {
            Member member = Discordian.getMember(Discordian.accountLinkManager.uuidToId(uuid));
            String text;

            if (member != null)
                text = member.getUser().getAsTag();
            else
                text = "unknown";

            source.sendFeedback(() ->
                    Text.literal("Your account is already linked to " + text)
                            .styled(style -> style.withColor(TextColor.fromFormatting(Formatting.RED))),
                    false);

            return 0;
        }

        Discordian.accountLinkManager.addPending(player.getUuidAsString(), code);
        source.sendFeedback(() ->
                Text.literal("Please DM the bot ")
                        .append(Text.literal(Discordian.jda.getSelfUser().getAsTag())
                                .styled(style -> style
                                        .withColor(TextColor.fromFormatting(Formatting.YELLOW)))
                        )
                        .append(Text.literal(" in Discord with the code to verify your account "))
                        .append(Text.literal("(click to copy code)")
                                .styled(style -> style
                                        .withColor(TextColor.fromFormatting(Formatting.AQUA))
                                        .withUnderline(true)
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, code))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to copy")))
                                )),
                false);
        return 1;
    }

    private static int unlink(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (player == null) {
            source.sendFeedback(() ->
                    Text.literal("This command can only be executed by a player")
                            .styled(style -> style.withColor(TextColor.fromFormatting(Formatting.RED))),
                    false);
            return 0;
        }

        if (!Discordian.accountLinkManager.isLinked(player.getUuidAsString())) {
            player.sendMessage(
                    Text.literal("Your account is not linked to a Discord account")
                            .styled(style -> style.withColor(TextColor.fromFormatting(Formatting.RED))),
                    false);

            return 0;
        }

        Discordian.configManager.removeLinkedAccount(player.getUuidAsString());
        player.sendMessage(Text.literal("Your Discord account has been unlinked").styled(style -> style.withColor(TextColor.fromFormatting(Formatting.GREEN))), false);
        return 1;
    }
}
