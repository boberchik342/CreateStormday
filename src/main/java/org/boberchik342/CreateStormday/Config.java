package org.boberchik342.CreateStormday;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = CreateStormday.MODID)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.BooleanValue WIND_BREAKS_CROPS = BUILDER.define("wind_breaks_crops", true);
    private static final ModConfigSpec.IntValue WIND_PARTICLE_SPAWN_AREA_SIZE = BUILDER.defineInRange("wind_particle_spawn_area_size", 200, 50, 1000);
    private static final ModConfigSpec.IntValue WIND_SAMPLE_INTERVAL = BUILDER.defineInRange("wind_sample_interval", 4, 1, 16);
    private static final ModConfigSpec.IntValue WIND_PUSH_STRENGTH = BUILDER.defineInRange("wind_push_strength", 5, 0, 200);
    static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean windBreaksCrops;
    public static int windParticleSpawnAreaSize;
    public static int windSampleInterval;
    public static int windPushStrength;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        windBreaksCrops = WIND_BREAKS_CROPS.get();
        windParticleSpawnAreaSize = WIND_PARTICLE_SPAWN_AREA_SIZE.get();
        windSampleInterval = WIND_SAMPLE_INTERVAL.get();
        windPushStrength = WIND_PUSH_STRENGTH.get();
    }
}
