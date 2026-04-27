package org.boberchik342.CreateStormday.debug;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class WindDebugBlock extends Block implements EntityBlock {

    public WindDebugBlock(Properties props) {
        super(props);
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, BlockState state) {
        return new WindDebugBlockEntity(pos, state);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.INVISIBLE; // we render manually
    }
}