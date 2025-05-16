//package com.example.examplemod;
//
//
//import baritone.api.BaritoneAPI;
//import baritone.api.Settings;
//import baritone.api.pathing.goals.GoalNear;
//import com.mojang.blaze3d.systems.RenderSystem;
//import com.mojang.blaze3d.vertex.*;
//import com.mojang.logging.LogUtils;
//import net.minecraft.client.Camera;
//import net.minecraft.client.KeyMapping;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.renderer.GameRenderer;
//import net.minecraft.core.BlockPos;
//import net.minecraft.world.entity.Entity;
//import net.minecraft.world.entity.EntityDimensions;
//import net.minecraft.world.entity.player.Player;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.phys.AABB;
//import net.minecraft.world.phys.Vec3;
//import net.neoforged.api.distmarker.Dist;
//import net.neoforged.bus.api.SubscribeEvent;
//import net.neoforged.fml.common.EventBusSubscriber;
//import net.neoforged.neoforge.client.event.ClientTickEvent;
//import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
//import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
//import net.neoforged.neoforge.event.entity.EntityEvent;
//import org.lwjgl.glfw.GLFW;
//import org.slf4j.Logger;
//
//import java.util.ArrayList;
//import java.util.Comparator;
//import java.util.Iterator;
//import java.util.List;
//
//@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
//public class RenderLevelStage {
//    private static final Logger LOGGER = LogUtils.getLogger();
//    private static List<Player> targets = new ArrayList<>();
//    private static Level lastLevel = null;
//    private static final double SEARCH_RADIUS = 20.0;
//    private static boolean enabled = false;
//
//
//
//    // Клавиши управления
//    public static final KeyMapping TOGGLE_ON_KEY = new KeyMapping(
//            "key.examplemod.enable",
//            GLFW.GLFW_KEY_EQUAL,
//            "key.category.examplemod"
//    );
//
//    public static final KeyMapping TOGGLE_OFF_KEY = new KeyMapping(
//            "key.examplemod.disable",
//            GLFW.GLFW_KEY_MINUS,
//            "key.category.examplemod"
//    );
//
//    @SubscribeEvent
//    public static void onClientTick(ClientTickEvent.Post event) {
//        Minecraft mc = Minecraft.getInstance();
//        if (mc.player == null) return;
//
//        // Обработка включения
//        while (TOGGLE_ON_KEY.consumeClick()) {
//            enabled = true;
//            targets.clear();
//            LOGGER.info("Mod ENABLED");
//        }
//
//        // Обработка выключения
//        while (TOGGLE_OFF_KEY.consumeClick()) {
//            enabled = false;
//            targets.clear();
//            if (BaritoneAPI.getProvider().getPrimaryBaritone() != null) {
//                BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
//            }
//            LOGGER.info("Mod DISABLED");
//        }
//    }
//
//    @SubscribeEvent
//    public static void onRenderLevelStage(RenderLevelStageEvent event) {
//        if (!enabled) {
//            if (!targets.isEmpty()) {
//                targets.clear();
//                if (BaritoneAPI.getProvider().getPrimaryBaritone() != null) {
//                    BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
//                }
//            }
//            return;
//        }
//
//        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
//        Minecraft mc = Minecraft.getInstance();
//        if (mc.player == null || mc.level == null || mc.isPaused()) {
//            targets.clear();
//            lastLevel = null;
//            return;
//        }
//
//        if (lastLevel != mc.level) {
//            targets.clear(); // Сбрасываем цели при смене мира
//            lastLevel = mc.level;
//        }
//
//        // Обновляем список целей
//        if(targets.isEmpty()) {
//            targets = sortPlayersByDistance(mc);
//        }
//
//        if(!targets.isEmpty()) {
//            Iterator<Player> iterator = targets.iterator();
//            while (iterator.hasNext()) {
//                Player player = iterator.next();
//                if (mc.player.distanceTo(player) > SEARCH_RADIUS) {
//                    iterator.remove(); // БЕЗОПАСНОЕ удаление
//                }
//            }
//            if(!targets.isEmpty()) {
//                Player target = targets.getFirst();
//                setBaritoneGoal(target);
//                if(hasReachedTarget(mc.player, target)) {
//                    targets.removeFirst();
//                }
//            }
//        }
//
//        // --- Остальной рендеринг ---
//        RenderSystem.enableBlend();
//        RenderSystem.defaultBlendFunc();
//        RenderSystem.disableDepthTest();
//        RenderSystem.disableCull();
//        RenderSystem.setShader(GameRenderer::getPositionColorShader);
//
//        PoseStack poseStack = event.getPoseStack();
//        Camera camera = event.getCamera();
//        Vec3 camPos = camera.getPosition();
//
//        Tesselator tesselator = Tesselator.getInstance();
//        BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
//
//        //for (Entity entity : mc.level.entitiesForRendering()) {
//        for (Entity entity : targets) {
//            if (entity == mc.player || !(entity instanceof Player)) continue;
//
//            AABB bb = entity.getBoundingBox()
//                    .inflate(0.05)
//                    .move(-camPos.x(), -camPos.y(), -camPos.z());
//
//            if (bb.isInfinite()) continue;
//
//            drawFlagBox(bufferBuilder, bb);
//        }
//
//        MeshData meshData = bufferBuilder.build();
//        if (meshData != null) {
//            BufferUploader.drawWithShader(meshData);
//        }
//
//        RenderSystem.enableDepthTest();
//        RenderSystem.enableCull();
//        RenderSystem.disableBlend();
//    }
//    // Сортировка игроков по расстоянию
//    private static List<Player> sortPlayersByDistance(Minecraft mc) {
//        List<Player> players = new ArrayList<>();
//        for (Player player : mc.level.players()) {
//            if (player != mc.player) {
//                players.add(player);
//            }
//        }
//
//        players.sort(Comparator.comparingDouble(p -> p.distanceToSqr(mc.player)));
//        return players;
//    }
//
//    // Проверка достижения цели
//    private static boolean hasReachedTarget(Player self, Player target) {
//        double distance = self.distanceTo(target);
//        return distance < 3.0; // Дистанция, при которой считаем, что цель достигнута
//    }
//
//    // Установка цели для Baritone
//    private static void setBaritoneGoal(Player target) {
//        if (BaritoneAPI.getProvider().getBaritoneForPlayer(Minecraft.getInstance().player) == null) {
//            return; // Baritone не загружена
//        }
//        int x = (int) Math.floor(target.getX());
//        int y = (int) Math.floor(target.getY());
//        int z = (int) Math.floor(target.getZ());
//        BlockPos pos = new BlockPos(x, y, z);
//        LOGGER.info("{}, {}, {}", x, y ,z );
//        // Используем GoalNear с радиусом 2 вместо GoalBlock
//        GoalNear goal = new GoalNear(pos, 2);
//        baritone.api.Settings settings = BaritoneAPI.getSettings();
//        settings.allowBreak.value = false; // Не разрушать блоки
//        settings.allowPlace.value = false; // Не ставить блоки
//        BaritoneAPI.getProvider().getPrimaryBaritone()
//                .getCustomGoalProcess()
//                .setGoalAndPath(goal);
//    }
//
//    private static void drawFlagBox(BufferBuilder bufferBuilder, AABB bb) {
//        double minX = bb.minX;
//        double minY = bb.minY;
//        double minZ = bb.minZ;
//        double maxX = bb.maxX;
//        double maxY = bb.maxY;
//        double maxZ = bb.maxZ;
//
//        // Вычисляем средние Y-значения для белой и синей зон
//        double mid1 = minY + (maxY - minY) * (1.0 / 3.0); // нижняя треть -> красный
//        double mid2 = minY + (maxY - minY) * (2.0 / 3.0); // средняя треть -> синий
//        // верхняя треть -> белый
//
//        // Белый верхний слой
//        addQuad(bufferBuilder, minX, mid2, minZ, maxX, mid2, minZ,
//                maxX, maxY, minZ, minX, maxY, minZ, 1.0f, 1.0f, 1.0f, 0.4f);
//
//        addQuad(bufferBuilder, minX, mid2, maxZ, maxX, mid2, maxZ,
//                maxX, maxY, maxZ, minX, maxY, maxZ, 1.0f, 1.0f, 1.0f, 0.4f);
//
//        addQuad(bufferBuilder, minX, mid2, minZ, minX, mid2, maxZ,
//                minX, maxY, maxZ, minX, maxY, minZ, 1.0f, 1.0f, 1.0f, 0.4f);
//
//        addQuad(bufferBuilder, maxX, mid2, minZ, maxX, mid2, maxZ,
//                maxX, maxY, maxZ, maxX, maxY, minZ, 1.0f, 1.0f, 1.0f, 0.4f);
//
//        addQuad(bufferBuilder, minX, mid2, minZ, maxX, mid2, minZ,
//                maxX, mid2, maxZ, minX, mid2, maxZ, 1.0f, 1.0f, 1.0f, 0.4f);
//
//        addQuad(bufferBuilder, minX, maxY, maxZ, maxX, maxY, maxZ,
//                maxX, maxY, minZ, minX, maxY, minZ, 1.0f, 1.0f, 1.0f, 0.4f);
//
//        // Синий средний слой
//        addQuad(bufferBuilder, minX, mid1, minZ, maxX, mid1, minZ,
//                maxX, mid2, minZ, minX, mid2, minZ, 0.0f, 0.0f, 1.0f, 0.4f);
//
//        addQuad(bufferBuilder, minX, mid1, maxZ, maxX, mid1, maxZ,
//                maxX, mid2, maxZ, minX, mid2, maxZ, 0.0f, 0.0f, 1.0f, 0.4f);
//
//        addQuad(bufferBuilder, minX, mid1, minZ, minX, mid1, maxZ,
//                minX, mid2, maxZ, minX, mid2, minZ, 0.0f, 0.0f, 1.0f, 0.4f);
//
//        addQuad(bufferBuilder, maxX, mid1, minZ, maxX, mid1, maxZ,
//                maxX, mid2, maxZ, maxX, mid2, minZ, 0.0f, 0.0f, 1.0f, 0.4f);
//
//        addQuad(bufferBuilder, minX, mid1, minZ, maxX, mid1, minZ,
//                maxX, mid1, maxZ, minX, mid1, maxZ, 0.0f, 0.0f, 1.0f, 0.4f);
//
//        addQuad(bufferBuilder, minX, mid2, maxZ, maxX, mid2, maxZ,
//                maxX, mid2, minZ, minX, mid2, minZ, 0.0f, 0.0f, 1.0f, 0.4f);
//
//        // Красный нижний слой
//        addQuad(bufferBuilder, minX, minY, minZ, maxX, minY, minZ,
//                maxX, mid1, minZ, minX, mid1, minZ, 1.0f, 0.0f, 0.0f, 0.4f);
//
//        addQuad(bufferBuilder, minX, minY, maxZ, maxX, minY, maxZ,
//                maxX, mid1, maxZ, minX, mid1, maxZ, 1.0f, 0.0f, 0.0f, 0.4f);
//
//        addQuad(bufferBuilder, minX, minY, minZ, minX, minY, maxZ,
//                minX, mid1, maxZ, minX, mid1, minZ, 1.0f, 0.0f, 0.0f, 0.4f);
//
//        addQuad(bufferBuilder, maxX, minY, minZ, maxX, minY, maxZ,
//                maxX, mid1, maxZ, maxX, mid1, minZ, 1.0f, 0.0f, 0.0f, 0.4f);
//
//        addQuad(bufferBuilder, minX, minY, minZ, maxX, minY, minZ,
//                maxX, minY, maxZ, minX, minY, maxZ, 1.0f, 0.0f, 0.0f, 0.4f);
//
//        addQuad(bufferBuilder, minX, mid1, maxZ, maxX, mid1, maxZ,
//                maxX, mid1, minZ, minX, mid1, minZ, 1.0f, 0.0f, 0.0f, 0.4f);
//    }
//
//    private static void addQuad(BufferBuilder bufferBuilder,
//                                double x1, double y1, double z1,
//                                double x2, double y2, double z2,
//                                double x3, double y3, double z3,
//                                double x4, double y4, double z4,
//                                float r, float g, float b, float a) {
//        bufferBuilder.addVertex((float) x1, (float) y1, (float) z1).setColor(r, g, b, a);
//        bufferBuilder.addVertex((float) x2, (float) y2, (float) z2).setColor(r, g, b, a);
//        bufferBuilder.addVertex((float) x3, (float) y3, (float) z3).setColor(r, g, b, a);
//        bufferBuilder.addVertex((float) x4, (float) y4, (float) z4).setColor(r, g, b, a);
//    }
//}
