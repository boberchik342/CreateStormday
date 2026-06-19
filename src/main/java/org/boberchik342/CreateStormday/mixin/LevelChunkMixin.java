package org.boberchik342.CreateStormday.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.boberchik342.CreateStormday.raycast.RaycastHelper;
import org.boberchik342.CreateStormday.wind.WindAirflowProvider;
import org.boberchik342.CreateStormday.wind.WindSystem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;

@Mixin(LevelChunk.class)
public class LevelChunkMixin {
    @Final
    @Shadow
    Level level;

    @Inject(method = "setBlockState", at = @At(value = "TAIL"))
    public void trackCrops(BlockPos pos, BlockState state, boolean p_62867_, CallbackInfoReturnable<BlockState> cir) {
        WindSystem windSystem = WindSystem.get(level);
        if (state.getBlock() instanceof CropBlock) {
            Set<BlockPos> crops = windSystem.crops.computeIfAbsent((LevelChunk) (Object) this, (k) -> new HashSet<BlockPos>());
            crops.add(pos);
        } else {
            Set<BlockPos> crops = windSystem.crops.get((LevelChunk) (Object) this);
            if (crops != null) {
                crops.remove(pos);
            }
        }
    }
}
