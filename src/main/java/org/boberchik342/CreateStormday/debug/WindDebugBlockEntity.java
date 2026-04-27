package org.boberchik342.CreateStormday.debug;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.boberchik342.CreateStormday.CreateStormday;
import org.boberchik342.CreateStormday.WindSystem;

public class WindDebugBlockEntity extends BlockEntity {

    public WindDebugBlockEntity(BlockPos pos, BlockState state) {
        super(CreateStormday.WIND_DEBUG.get(), pos, state);
    }

    public double getExposure(Level level, Direction dir) {
        return WindSystem.get(level).getBlockWindExposure(level, worldPosition.offset(dir.getNormal()));
    }
}