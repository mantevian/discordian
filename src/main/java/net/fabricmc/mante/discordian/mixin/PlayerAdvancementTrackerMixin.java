package net.fabricmc.mante.discordian.mixin;

import net.fabricmc.mante.discordian.DiscordUtil;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(PlayerAdvancementTracker.class)
public class PlayerAdvancementTrackerMixin {
	@Shadow
	private ServerPlayerEntity owner;

	@Inject(method = "grantCriterion", at = @At(value = "INVOKE", target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V", shift = At.Shift.AFTER))
	private void grantCriterion(AdvancementEntry advancement, String criterionName, CallbackInfoReturnable<Boolean> cir) {
		advancement.value().display().ifPresent((display) -> {
			if (display.shouldAnnounceToChat() && this.owner.getWorld().getGameRules().getBoolean(GameRules.ANNOUNCE_ADVANCEMENTS)) {
				DiscordUtil.INSTANCE.sendAdvancement(Text.translatable("chat.type.advancement." + display.getFrame().asString(), this.owner.getDisplayName(), Advancement.getNameFromIdentity(advancement)).getString(), advancement.value().display().get().getDescription().getString());
			}
		});
	}
}