package com.example.examplemod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;

import net.minecraft.world.entity.player.Player;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;

import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(ExampleMod.MODID)
public class ExampleMod
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "examplemod";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "examplemod" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "examplemod" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Creates a new Block with tFhe id "examplemod:example_block", combining the namespace and path
    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block", BlockBehaviour.Properties.of().mapColor(MapColor.STONE));
    // Creates a new BlockItem with the id "examplemod:example_block", combining the namespace and path
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block", EXAMPLE_BLOCK);

    // Creates a new food item with the id "examplemod:example_id", nutrition 1 and saturation 2
    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("example_item", new Item.Properties().food(new FoodProperties.Builder()
            .alwaysEdible().nutrition(1).saturationModifier(2f).build()));

    // Creates a creative tab with the id "examplemod:example_tab" for the example item, that is placed after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.examplemod")) //The language key for the title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(EXAMPLE_ITEM.get()); // Add the example item to the tab. For your own tabs, this method is preferred over the event
            }).build());

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public ExampleMod(IEventBus modEventBus, ModContainer modContainer)
    {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
            event.accept(EXAMPLE_BLOCK_ITEM);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    @SubscribeEvent
    public void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.isPaused()) return;

        // Setup render states
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        // Сохраняем текущую матрицу
        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        boolean hasData = false;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player || !(entity instanceof Player)) continue;

            AABB bb = entity.getBoundingBox()
                    .inflate(0.05)  // Slightly expand for visibility
                    .move(-camPos.x(), -camPos.y(), -camPos.z());

            if (bb.isInfinite()) continue;  // Skip invalid boxes

            drawFlagBox(bufferBuilder, bb);
            hasData = true;
        }

        if (hasData) {
            BufferUploader.drawWithShader(bufferBuilder.build());
        }

        // Restore default render states
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }


    private static void drawFlagBox(BufferBuilder bufferBuilder, AABB bb) {
        double minX = bb.minX;
        double minY = bb.minY;
        double minZ = bb.minZ;
        double maxX = bb.maxX;
        double maxY = bb.maxY;
        double maxZ = bb.maxZ;

        // Вычисляем средние Y-значения для белой и синей зон
        double mid1 = minY + (maxY - minY) * (1.0 / 3.0); // нижняя треть -> красный
        double mid2 = minY + (maxY - minY) * (2.0 / 3.0); // средняя треть -> синий
        // верхняя треть -> белый

        // Белый верхний слой
        addQuad(bufferBuilder, minX, mid2, minZ, maxX, mid2, minZ,
                maxX, maxY, minZ, minX, maxY, minZ, 1.0f, 1.0f, 1.0f, 0.4f);

        addQuad(bufferBuilder, minX, mid2, maxZ, maxX, mid2, maxZ,
                maxX, maxY, maxZ, minX, maxY, maxZ, 1.0f, 1.0f, 1.0f, 0.4f);

        addQuad(bufferBuilder, minX, mid2, minZ, minX, mid2, maxZ,
                minX, maxY, maxZ, minX, maxY, minZ, 1.0f, 1.0f, 1.0f, 0.4f);

        addQuad(bufferBuilder, maxX, mid2, minZ, maxX, mid2, maxZ,
                maxX, maxY, maxZ, maxX, maxY, minZ, 1.0f, 1.0f, 1.0f, 0.4f);

        addQuad(bufferBuilder, minX, mid2, minZ, maxX, mid2, minZ,
                maxX, mid2, maxZ, minX, mid2, maxZ, 1.0f, 1.0f, 1.0f, 0.4f);

        addQuad(bufferBuilder, minX, maxY, maxZ, maxX, maxY, maxZ,
                maxX, maxY, minZ, minX, maxY, minZ, 1.0f, 1.0f, 1.0f, 0.4f);

        // Синий средний слой
        addQuad(bufferBuilder, minX, mid1, minZ, maxX, mid1, minZ,
                maxX, mid2, minZ, minX, mid2, minZ, 0.0f, 0.0f, 1.0f, 0.4f);

        addQuad(bufferBuilder, minX, mid1, maxZ, maxX, mid1, maxZ,
                maxX, mid2, maxZ, minX, mid2, maxZ, 0.0f, 0.0f, 1.0f, 0.4f);

        addQuad(bufferBuilder, minX, mid1, minZ, minX, mid1, maxZ,
                minX, mid2, maxZ, minX, mid2, minZ, 0.0f, 0.0f, 1.0f, 0.4f);

        addQuad(bufferBuilder, maxX, mid1, minZ, maxX, mid1, maxZ,
                maxX, mid2, maxZ, maxX, mid2, minZ, 0.0f, 0.0f, 1.0f, 0.4f);

        addQuad(bufferBuilder, minX, mid1, minZ, maxX, mid1, minZ,
                maxX, mid1, maxZ, minX, mid1, maxZ, 0.0f, 0.0f, 1.0f, 0.4f);

        addQuad(bufferBuilder, minX, mid2, maxZ, maxX, mid2, maxZ,
                maxX, mid2, minZ, minX, mid2, minZ, 0.0f, 0.0f, 1.0f, 0.4f);

        // Красный нижний слой
        addQuad(bufferBuilder, minX, minY, minZ, maxX, minY, minZ,
                maxX, mid1, minZ, minX, mid1, minZ, 1.0f, 0.0f, 0.0f, 0.4f);

        addQuad(bufferBuilder, minX, minY, maxZ, maxX, minY, maxZ,
                maxX, mid1, maxZ, minX, mid1, maxZ, 1.0f, 0.0f, 0.0f, 0.4f);

        addQuad(bufferBuilder, minX, minY, minZ, minX, minY, maxZ,
                minX, mid1, maxZ, minX, mid1, minZ, 1.0f, 0.0f, 0.0f, 0.4f);

        addQuad(bufferBuilder, maxX, minY, minZ, maxX, minY, maxZ,
                maxX, mid1, maxZ, maxX, mid1, minZ, 1.0f, 0.0f, 0.0f, 0.4f);

        addQuad(bufferBuilder, minX, minY, minZ, maxX, minY, minZ,
                maxX, minY, maxZ, minX, minY, maxZ, 1.0f, 0.0f, 0.0f, 0.4f);

        addQuad(bufferBuilder, minX, mid1, maxZ, maxX, mid1, maxZ,
                maxX, mid1, minZ, minX, mid1, minZ, 1.0f, 0.0f, 0.0f, 0.4f);
    }

    private static void addQuad(BufferBuilder bufferBuilder,
                                double x1, double y1, double z1,
                                double x2, double y2, double z2,
                                double x3, double y3, double z3,
                                double x4, double y4, double z4,
                                float r, float g, float b, float a) {
        bufferBuilder.addVertex((float)x1, (float)y1, (float)z1).setColor(r, g, b, a);
        bufferBuilder.addVertex((float)x2, (float)y2, (float)z2).setColor(r, g, b, a);
        bufferBuilder.addVertex((float)x3, (float)y3, (float)z3).setColor(r, g, b, a);
        bufferBuilder.addVertex((float)x4, (float)y4, (float)z4).setColor(r, g, b, a);
    }
}

