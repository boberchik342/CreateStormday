package org.boberchik342.CreateStormday.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.boberchik342.CreateStormday.raycast.RaycastHelper;
import org.boberchik342.CreateStormday.wind.WindSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public class LevelMixin {
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z", at = @At(value = "TAIL"))
    void trackBlocks(BlockPos pos, BlockState newState, int flags, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            RaycastHelper.get((Level) (Object) this).set(pos, WindSystem.isBlockWindPassable(newState));
        }
    }
}
