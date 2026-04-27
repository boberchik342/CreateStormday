package org.boberchik342.CreateStormday;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
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
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.boberchik342.CreateStormday.debug.WindDebugBlock;
import org.boberchik342.CreateStormday.debug.WindDebugBlockEntity;
import org.boberchik342.CreateStormday.debug.WindDebugRenderer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@Mod(CreateStormday.MODID)
public class CreateStormday {
    public static final String MODID = "create_stormday";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static int raycasts = 0;

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, CreateStormday.MODID);

    public static final Supplier<Block> WIND_DEBUG_BLOCK =
            BLOCKS.register("wind_debug_block", () ->
                    new WindDebugBlock(BlockBehaviour.Properties.of()
                            .strength(1.0f)
                            .noOcclusion()
                    )
            );

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreateStormday.MODID);

    public static final Supplier<BlockEntityType<WindDebugBlockEntity>> WIND_DEBUG =
            BLOCK_ENTITIES.register("wind_debug", () ->
                    BlockEntityType.Builder.of(
                            WindDebugBlockEntity::new,
                            WIND_DEBUG_BLOCK.get()
                    ).build(null)
            );

    public CreateStormday(IEventBus modEventBus, ModContainer modContainer) {
//        NeoForge.EVENT_BUS.register(this);
        BLOCKS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
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
//            BlockPos pos = new BlockPos(0, 0, 0);
//            for (var dir : Direction.values()) {
//                LOGGER.info(dir.getName());
//                BlockPos sidePos = pos.offset(dir.getNormal());
//                LOGGER.info(sidePos.toString());
//                Vec3i n = dir.getNormal().multiply(-1);
//                Vec3 a = new Vec3(1 - Math.abs(dir.getNormal().getX()), 1 - Math.abs(dir.getNormal().getY()), 1 - Math.abs(dir.getNormal().getZ()));
//                LOGGER.info(a.toString());
//                Vec3 b = new Vec3(Math.max(n.getX(), 0), Math.max(n.getY(), 0), Math.max(n.getZ(), 0));
//                LOGGER.info(b.toString());
//            }
        }

        @SubscribeEvent // on the mod event bus only on the physical client
        public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(
                    // The block entity type to register the renderer for.
                    WIND_DEBUG.get(),
                    // A function of BlockEntityRendererProvider.Context to BlockEntityRenderer.
                    WindDebugRenderer::new
            );
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
                        if (system.getBlockWindExposure(level, pos) * system.getWind().x <= 10) continue;
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
            raycasts = WindSystem.windComputations;
            WindSystem.windComputations = 0;
            if (Minecraft.getInstance().level == null) return;
            var system = WindSystem.get(Minecraft.getInstance().level);
//            Vec2 wind = system.getWind();
//            LOGGER.info("Wind is {} {}", wind.x, wind.y);
            spawnWindParticles(Minecraft.getInstance());
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
            event.getGuiGraphics().drawString(Minecraft.getInstance().font, String.valueOf(raycasts), 20, 60, 0xFFFFFFFF);
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

        int size = 5;
        volume = (int) Math.pow(size * 2 + 1, 3);
//        volume * strength / 64
        for (int i = 0; i < volume * strength / 64; i++) {
            BlockPos pos = new BlockPos(
                    (int) playerPos.x + level.random.nextInt(1 + 2 * size) - size,
                    (int) playerPos.y + level.random.nextInt(1 + 2 * size) - size,
                    (int) playerPos.z + level.random.nextInt(1 + 2 * size) - size
            );
            if (WindSystem.get(level).getBlockWindExposure(level, pos) > 0.9) {
                BlockState state = level.getBlockState(pos);
                if (!state.isAir()) continue;
                for (var dir : Direction.values()) {
                    BlockPos sidePos = pos.offset(dir.getNormal());
                    BlockState s = level.getBlockState(sidePos);
                    if (s.isAir()) continue;
                    Vec3 offset = new Vec3(
                        level.random.nextDouble(),
                        level.random.nextDouble(),
                        level.random.nextDouble()
                    );
                    Vec3i n = dir.getNormal().multiply(-1);
                    Vec3 a = new Vec3(1 - Math.abs(dir.getNormal().getX()), 1 - Math.abs(dir.getNormal().getY()), 1 - Math.abs(dir.getNormal().getZ()));
                    Vec3 b = new Vec3(Math.max(n.getX(), 0), Math.max(n.getY(), 0), Math.max(n.getZ(), 0));
                    offset = offset.multiply(a).add(b);
                    level.addParticle(
                            new BlockParticleOption(ParticleTypes.BLOCK, s),
//                            ParticleTypes.CLOUD,
                            sidePos.getX() + offset.x,
                            sidePos.getY() + offset.y,
                            sidePos.getZ() + offset.z,
                            0, 0, 0
                    );
                }
            }
        }
    }
}
