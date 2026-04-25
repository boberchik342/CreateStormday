package org.boberchik342.CreateStormday.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.boberchik342.CreateStormday.ServerWindSystem;
import org.boberchik342.CreateStormday.WindSystem;

public class WindCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("wind")
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("set")
                    .then(Commands.argument("strength", FloatArgumentType.floatArg())
                        .then(Commands.argument("direction", FloatArgumentType.floatArg())
                            .executes(ctx -> {
                                float strength = FloatArgumentType.getFloat(ctx, "strength");
                                float direction = FloatArgumentType.getFloat(ctx, "direction");

                                var level = ctx.getSource().getLevel();
                                ServerWindSystem system = (ServerWindSystem) WindSystem.get(level);
                                system.setWind(level, strength, direction);

                                return 1;
                            })
                        )
                    )
                ).then(Commands.literal("reset")
                    .executes(ctx -> {
                        var level = ctx.getSource().getLevel();
                        ServerWindSystem system = (ServerWindSystem) WindSystem.get(level);
                        system.resetWind();
                        return 1;
                    })
                )
        );
    }
}
