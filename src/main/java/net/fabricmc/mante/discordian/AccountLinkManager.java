package net.fabricmc.mante.discordian;

import net.dv8tion.jda.api.entities.Member;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.UUID;

public class AccountLinkManager {
    public ArrayList<AccountLinkDetails> pending;

    public boolean codeAlreadyExists(String code) {
        for (AccountLinkDetails details : pending) {
            if (details.code.equals(code)) {
                return true;
            }
        }

        return false;
    }

    private String getFourDigitSequence() {
        return String.valueOf(Discordian.RANDOM.nextInt(1000, 10000));
    }

    public String generateCode() {
        String code;

        do {
            code = getFourDigitSequence();
        }
        while (codeAlreadyExists(code));

        return code;
    }

    public AccountLinkManager() {
        pending = new ArrayList<>();
    }

    public void addPending(String uuid, String code) {
        pending.add(new AccountLinkDetails(uuid, code));
    }

    public void removePending(String code) {
        pending.removeIf(accountLinkDetails -> accountLinkDetails.code.equals(code));
    }

    public void link(String id, String code) {
        for (AccountLinkDetails account : pending) {
            if (code.equals((account.code))) {
                Discordian.configManager.addLinkedAccount(account.uuid, id);
                removePending(code);

                ServerPlayerEntity player = Discordian.server.getPlayerManager().getPlayer(UUID.fromString(account.uuid));

                if (player != null) {
                    Member member = Discordian.getMember(id);
                    if (member == null)
                        return;

                    player.sendMessage(
                            Text.literal("Your account has been linked to ").styled(style -> style.withColor(TextColor.fromFormatting(Formatting.GREEN)))
                                    .append(Text.literal(member.getUser().getAsTag()).styled(style -> style.withColor(TextColor.fromFormatting(Formatting.YELLOW)))),
                            false);

                    if (Discordian.configManager.config.get("require_link_to_play").getAsBoolean()) {
                        player.changeGameMode(GameMode.DEFAULT);
                        ServerWorld spawnWorld = Discordian.server.getWorld(player.getSpawnPointDimension());
                        BlockPos spawnPos = player.getSpawnPointPosition();
                        if (spawnPos == null) {
                            spawnPos = Discordian.server.getOverworld().getSpawnPos();
                        }
                        player.teleport(spawnWorld, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), 0.0f, 0.0f);
                    }
                }

                return;
            }
        }
    }

    public boolean isLinked(String uuid) {
        return Discordian.configManager.config.get("linked_accounts").getAsJsonObject().has(uuid);
    }

    public String uuidToId(String uuid) {
        return Discordian.configManager.config.get("linked_accounts").getAsJsonObject().has(uuid) ? Discordian.configManager.config.get("linked_accounts").getAsJsonObject().get(uuid).getAsString() : "";
    }
}