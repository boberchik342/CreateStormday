package org.boberchik342.CreateStormday.all;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.boberchik342.CreateStormday.CreateStormday;
import org.boberchik342.CreateStormday.debug.WindDebugBlock;
import org.boberchik342.CreateStormday.pinwheel.PinwheelBlock;

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

    public static final Supplier<Block> PINWHEEL = BLOCKS.register(
            "pinwheel",
            () -> new PinwheelBlock(BlockBehaviour.Properties.of()
                    .destroyTime(2.0f)
                    .explosionResistance(10.0f)
                    .sound(SoundType.GRAVEL)
                    .noOcclusion()
                    .noCollission()
            ));
}
