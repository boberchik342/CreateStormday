package org.boberchik342.CreateStormday.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import org.boberchik342.CreateStormday.mixin.LevelMixin;
import org.boberchik342.CreateStormday.raycast.RaycastHelper;
import org.boberchik342.CreateStormday.raycast.RaycastOctree;
import org.boberchik342.CreateStormday.wind.ServerWindSystem;
import org.boberchik342.CreateStormday.wind.WindSystem;

public class RaycastDebugCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("wind")
                    .requires(source -> source.hasPermission(4))
                    .executes((ctx) -> {
                        ChunkPos cp = new ChunkPos(BlockPos.containing(ctx.getSource().getPosition()));
                        RaycastOctree.frozen = true;
                        for (int x = cp.getMinBlockX(); x <= cp.getMaxBlockX(); x++) {
                            for (int z = cp.getMinBlockZ(); z <= cp.getMaxBlockZ(); z++) {
                                for (int y = ctx.getSource().getLevel().getMinBuildHeight(); y <= ctx.getSource().getLevel().getMaxBuildHeight(); y++) {
                                    ctx.getSource().getLevel().setBlock(
                                            new BlockPos(x, y, z),
                                            RaycastHelper.get(ctx.getSource().getLevel()).get(new BlockPos(x, y, z)) ? Blocks.RED_WOOL.defaultBlockState() : Blocks.AIR.defaultBlockState()
                                            , 3
                                    );
                                }
                            }
                        }
                        RaycastOctree.frozen = false;
                        return 1;
                    }));
    }
}
