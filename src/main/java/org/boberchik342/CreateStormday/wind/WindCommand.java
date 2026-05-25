package org.boberchik342.CreateStormday.wind;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

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
                                ServerWindAirflowProvider system = (ServerWindAirflowProvider) WindSystem.get(level).windProvider;
                                system.setWind(level, strength, direction);

                                return 1;
                            })
                        )
                    )
                ).then(Commands.literal("reset")
                    .executes(ctx -> {
                        var level = ctx.getSource().getLevel();
                        ServerWindAirflowProvider system = (ServerWindAirflowProvider) WindSystem.get(level).windProvider;
                        system.resetWind();
                        return 1;
                    })
                )
        );
    }
}
