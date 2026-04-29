package org.boberchik342.CreateStormday.all;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.boberchik342.CreateStormday.CreateStormday;
import org.boberchik342.CreateStormday.debug.WindDebugBlock;

import java.util.function.Supplier;

public class AllBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, CreateStormday.MODID);
    public static final Supplier<Block> WIND_DEBUG =
            BLOCKS.register("wind_debug", () ->
                    new WindDebugBlock(BlockBehaviour.Properties.of()
                            .strength(1.0f)
                            .noOcclusion()
                    )
            );
}
