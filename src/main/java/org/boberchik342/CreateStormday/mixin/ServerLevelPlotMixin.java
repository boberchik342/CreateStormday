package org.boberchik342.CreateStormday.mixin;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.event.level.ChunkEvent;
import org.boberchik342.CreateStormday.CreateStormday;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ServerLevelPlot.class)
public class ServerLevelPlotMixin {
    @Inject(method = "load", at = @At(value = "TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void chunksLoaded(CompoundTag tag, CallbackInfo ci, int logSize, int dataVersion, ServerSubLevel subLevel, ServerLevel level, CompoundTag chunks, SubLevelPhysicsSystem physicsSystem) {
        for (final String key : chunks.getAllKeys()) {
            final long chunkPos = Long.parseLong(key);

            final int x = ChunkPos.getX(chunkPos);
            final int z = ChunkPos.getZ(chunkPos);
            final ChunkPos local = new ChunkPos(x, z);
            final LevelChunk chunk = ((ServerLevelPlot) (Object) this).getChunk(local);

            CreateStormday.ModEvents.onChunkLoad(new ChunkEvent.Load(chunk, false));
        }
    }
}
