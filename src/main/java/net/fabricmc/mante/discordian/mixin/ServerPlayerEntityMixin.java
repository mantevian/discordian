package net.fabricmc.mante.discordian.mixin;

import net.fabricmc.mante.discordian.DiscordUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
    @Inject(method = "onDeath", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;broadcast(Lnet/minecraft/text/Text;Z)V", shift = At.Shift.AFTER))
    public void onDeath(DamageSource source, CallbackInfo ci) {
        LivingEntity serverPlayerEntity = (ServerPlayerEntity) (Object) this;
        DiscordUtil.INSTANCE.sendDeathMessage(serverPlayerEntity.getDamageTracker().getDeathMessage().getString());
    }
}
