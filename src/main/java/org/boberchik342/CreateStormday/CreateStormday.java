package org.boberchik342.CreateStormday;

import com.mojang.logging.LogUtils;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.LevelChunk;
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
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.boberchik342.CreateStormday.all.AllBlockEntities;
import org.boberchik342.CreateStormday.all.AllBlocks;
import org.boberchik342.CreateStormday.all.AllItems;
import org.boberchik342.CreateStormday.debug.RaycastDebugCommand;
import org.boberchik342.CreateStormday.debug.WindDebugRenderer;
import org.boberchik342.CreateStormday.pinwheel.PinwheelItemExtensions;
import org.boberchik342.CreateStormday.pinwheel.PinwheelItemRenderer;
import org.boberchik342.CreateStormday.raycast.RaycastHelper;
import org.boberchik342.CreateStormday.raycast.RaycastOctree;
import org.boberchik342.CreateStormday.wind.*;
import org.joml.RoundingMode;
import org.joml.Vector3i;
import org.slf4j.Logger;

@Mod(CreateStormday.MODID)
public class CreateStormday {
    public static final String MODID = "create_stormday";
    private static final Logger LOGGER = LogUtils.getLogger();


    public CreateStormday(IEventBus modEventBus, ModContainer modContainer) {
        AllBlocks.BLOCKS.register(modEventBus);
        AllBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        AllItems.ITEMS.register(modEventBus);
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
            RaycastOctree.test();
        }

        @SubscribeEvent
        public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(
                    AllBlockEntities.WIND_DEBUG.get(),
                    WindDebugRenderer::new
            );
        }

        @SubscribeEvent
        public static void onServerPreTick(ServerTickEvent.Pre event) {
            WindSystem.tickWind(event.getServer().getAllLevels());
        }

        @SubscribeEvent
        public static void onChunkLoad(ChunkEvent.Load event) {
            if (!(event.getChunk() instanceof LevelChunk chunk)) return;
            WindSystem.onChunkLoad(chunk);
            RaycastHelper.get(chunk.getLevel()).loadChunk(chunk);
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            if (Minecraft.getInstance().isPaused()) return;
            PinwheelItemRenderer.tick();
            if (Minecraft.getInstance().level == null) return;
            WindGraphics.spawnParticles(Minecraft.getInstance());
        }

        @SubscribeEvent
        public static void registerAdditional(ModelEvent.RegisterAdditional event) {
            event.register(ModelResourceLocation.standalone(CreateStormday.id("pinwheel_handle")));
            event.register(ModelResourceLocation.standalone(CreateStormday.id("pinwheel_blades")));
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
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            WindCommand.register(event.getDispatcher());
            RaycastDebugCommand.register(event.getDispatcher());
        }
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> ItemProperties.register(
                    AllItems.PINWHEEL.get(),
                    CreateStormday.id("dummy"),
                    (stack, level, entity, seed) -> 0f
            ));
        }

        @SubscribeEvent
        public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
            event.registerItem(new PinwheelItemExtensions(), AllItems.PINWHEEL.get());
        }

        @SubscribeEvent
        public static void onEntityTick(EntityTickEvent.Pre event) {
            Entity entity = event.getEntity();
            WindSystem system = WindSystem.get(entity.level());
            if (entity instanceof Player player) {
                if (player.getAbilities().flying) return;
            }
            Vec3 pos = entity.position();
            if (entity instanceof EntityMovementExtension ext) {
                SubLevel subLevel = ext.sable$getTrackingSubLevel();
                if (subLevel != null) {
                    Vec3 plotPos = subLevel.logicalPose().transformPositionInverse(entity.position());
                    if (subLevel.getPlot().getBoundingBox().contains(new Vector3i(plotPos.x, plotPos.y, plotPos.z, RoundingMode.FLOOR))) {
                        pos = plotPos;
                    }
                }
            }
            entity.level().addParticle(ParticleTypes.BUBBLE, pos.x, pos.y, pos.z, 0, 0, 0);
            Vec3 r = system.getWindVelocityAt(entity.level(), BlockPos.containing(pos)).scale(1.0/20).subtract(entity.getDeltaMovement());
            event.getEntity().addDeltaMovement(r.scale((double) Config.windPushStrength / 100));
        }
    }
}
