package org.boberchik342.CreateStormday.all;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.boberchik342.CreateStormday.CreateStormday;
import org.boberchik342.CreateStormday.debug.WindDebugBlockEntity;

import java.util.function.Supplier;

public class AllBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreateStormday.MODID);

    public static final Supplier<BlockEntityType<WindDebugBlockEntity>> WIND_DEBUG =
            BLOCK_ENTITIES.register("wind_debug", () ->
                    BlockEntityType.Builder.of(
                            WindDebugBlockEntity::new,
                            AllBlocks.WIND_DEBUG.get()
                    ).build(    null)
            );
}
