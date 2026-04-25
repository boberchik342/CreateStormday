package org.boberchik342.CreateStormday;

import com.mojang.logging.LogUtils;
import dev.ryanhcode.sable.api.block.propeller.BlockEntityPropeller;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mod(CreateStormday.MODID)
public class CreateStormday {
    public static final String MODID = "create_stormday";
    private static final Logger LOGGER = LogUtils.getLogger();

    public CreateStormday(IEventBus modEventBus, ModContainer modContainer) {
//        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    public static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(CreateStormday.MODID, name);
    }

    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onCommonSetup(FMLCommonSetupEvent event) {
            LOGGER.info("Wind breaks crops: {}", Config.windBreaksCrops);
        }
        @SubscribeEvent
        public static void onServerPreTick(ServerTickEvent.Pre event) {
            for (var level : event.getServer().getAllLevels()) {
                WindSystem system = WindSystem.get(level);
                if (system instanceof ServerWindSystem serverSystem) {
                    serverSystem.tick(level);
                }
                if (system.getWind().x > 10 && Config.windBreaksCrops) {
                    List<BlockPos> snapshot = new ArrayList<>();

                    for (Set<BlockPos> set : system.crops.values()) {
                        snapshot.addAll(set);
                    }
                    for (var pos : snapshot) {
                        if (!system.isBlockExposed(level, pos)) continue;
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 0);
                    }
                }
            }

        }

        @SubscribeEvent
        public static void onChunkLoad(ChunkEvent.Load event) {
            if (!(event.getLevel() instanceof ServerLevel level)) return;
            if (!(event.getChunk() instanceof LevelChunk chunk)) return;

            var system = WindSystem.get(level);
            Set<BlockPos> crops = system.crops.computeIfAbsent(chunk, k -> new HashSet<>());

            int minY = chunk.getMinBuildHeight();
            int maxY = chunk.getMaxBuildHeight();

            ChunkPos cp = chunk.getPos();

            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = minY; y < maxY; y++) {

                        BlockPos pos = new BlockPos(
                                cp.getMinBlockX() + x,
                                y,
                                cp.getMinBlockZ() + z
                        );

                        BlockState state = chunk.getBlockState(pos);

                        if (state.getBlock() instanceof CropBlock) {
                            LOGGER.info("found crop");
                            crops.add(pos);
                        }
                    }
                }
            }

        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            if (Minecraft.getInstance().level == null) return;
            var system = WindSystem.get(Minecraft.getInstance().level);
            Vec2 wind = system.getWind();
//            LOGGER.info("Wind is {} {}", wind.x, wind.y);
            spawnWindParticles(Minecraft.getInstance());
        }
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {

        }
        @SubscribeEvent
        public static void register(final RegisterPayloadHandlersEvent event) {
            final PayloadRegistrar registrar = event.registrar("1");
            registrar.commonToClient(
                WindPacket.TYPE,
                WindPacket.CODEC,
                ClientPayloadHandler::handleWindPacket
            );
        }

        @SubscribeEvent
        public static void onRender(RenderGuiEvent.Post event) {
            float strength = WindSystem.get(Minecraft.getInstance().level).getWind().x;
            event.getGuiGraphics().setColor(1, 1, 1, 1);
            event.getGuiGraphics().drawString(Minecraft.getInstance().font, String.valueOf(strength), 20, 20, 0xFFFFFFFF);
        }

        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            WindCommand.register(event.getDispatcher());
        }
    }

    private static void spawnWindParticles(Minecraft mc) {
        if (mc.isPaused()) return;

        ClientLevel level = mc.level;

        if (mc.level == null || mc.player == null) return;

        Vec2 wind = WindSystem.get(level).getWind();

        Vec3 playerPos = mc.player.position();

        float direction = wind.y;
        float strength = wind.x;

        double vx = Math.cos(direction) * strength;
        double vz = Math.sin(direction) * strength;
        int volume = Config.windParticleSpawnAreaSize * Config.windParticleSpawnAreaSize * Config.windParticleSpawnAreaSize;
        for (int i = 0; i < Math.min(Math.pow(Math.max(strength - 0.5, 0), 1.5) * volume / 2000, 1000); i++) {
            double x = playerPos.x + (level.random.nextDouble() - 0.5) * 200;
            double y = playerPos.y + (level.random.nextDouble() - 0.5) * 200;
            double z = playerPos.z + (level.random.nextDouble() - 0.5) * 200;

            double jitterX = (level.random.nextDouble() - 0.5) * 0.2;
            double jitterZ = (level.random.nextDouble() - 0.5) * 0.2;

            level.addParticle(
                    ParticleTypes.CLOUD,
                    x, y, z,
                    vx + jitterX,
                    0,
                    vz + jitterZ
            );
        }

        int size = 20;
        volume = (int) Math.pow(size * 2 + 1, 3);
        for (int i = 0; i < volume * strength / 64; i++) {
            BlockPos pos = new BlockPos(
                    (int) playerPos.x + level.random.nextInt(1 + 2 * size) - size,
                    (int) playerPos.y + level.random.nextInt(1 + 2 * size) - size,
                    (int) playerPos.z + level.random.nextInt(1 + 2 * size) - size
            );
            if (WindSystem.get(level).isBlockExposed(level, pos)) {
                BlockState state = level.getBlockState(pos);
                if (state.isAir()) continue;
                level.addParticle(
                        new BlockParticleOption(ParticleTypes.BLOCK, state),
                        pos.getX() + level.random.nextDouble(),
                        pos.getY() + 1,
                        pos.getZ() + level.random.nextDouble(),
                        vx * 3, 10, vz * 3
                );
            }
        }
    }
}
