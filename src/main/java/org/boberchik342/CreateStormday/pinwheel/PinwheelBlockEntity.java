package org.boberchik342.CreateStormday.pinwheel;

import dev.ryanhcode.sable.companion.math.JOMLConversion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.boberchik342.CreateStormday.all.AllBlockEntities;
import org.boberchik342.CreateStormday.wind.WindSystem;

public class PinwheelBlockEntity extends BlockEntity {
    public float prevRotation;
    public float rotation;
    public float speed;
    public float prevDir;
    public float dir;

    public PinwheelBlockEntity(BlockPos pos, BlockState blockState) {
        super(AllBlockEntities.PINWHEEL.get(), pos, blockState);
    }

    public Vec3 getWind() {
        return WindSystem.get(getLevel()).getWind(getLevel(), worldPosition.above().getCenter());
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PinwheelBlockEntity blockEntity) {
        blockEntity.prevRotation = blockEntity.rotation;
        blockEntity.prevDir = blockEntity.dir;
        Vec3 wind = blockEntity.getWind();
        if (wind.length() > 0.01) {
            blockEntity.dir = (float) -Math.atan2(-wind.x, wind.z);
        }

        float targetSpeed = (float) (blockEntity.getWind().length() / 10);
        blockEntity.speed = (float) (blockEntity.speed * 0.9 + targetSpeed * 0.1);
        blockEntity.rotation += blockEntity.speed;
    }

    public float getRotation(float pt) {
        return prevRotation * (1 - pt) + rotation * pt;
    }

    public float getDir(float pt) {
        return prevDir * (1 - pt) + dir * pt;
    }
}
