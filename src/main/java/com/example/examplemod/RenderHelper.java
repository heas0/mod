package com.example.examplemod;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = ExampleMod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class RenderHelper {
    public static final List<Entity> entities = new ArrayList<>();
    // === Управление сущностями ===

    public static void addEntity(Entity entity) {
        if (entity != null && !entities.contains(entity)) {
            entities.add(entity);
        }
    }

    public static void removeEntity(Entity entity) {
        if (entity != null) {
            entities.remove(entity);
        }
    }

    public static void clearEntities() {
        entities.clear();
    }
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        if (entities.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused() || mc.player == null) return;

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();

        renderBoxEntities(entities, camPos, 0.5f, 0.5f, 0.5f, 0.5f);
    }
    /**
     * Рисует AABB сущности как параллелепипед с заданным цветом и прозрачностью.
     *
     * @param entities - сущность, чей хитбокс нужно отрисовать
     * @param r      - красный канал (0.0f - 1.0f)
     * @param g      - зелёный канал (0.0f - 1.0f)
     * @param b      - синий канал (0.0f - 1.0f)
     * @param a      - альфа-канал (прозрачность, 0.0f - 1.0f)
     */
    public static void renderBoxEntities(List<Entity> entities, Vec3 camPos, float r, float g, float b, float a) {

        // Включаем смешивание для прозрачности
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        // Получаем BufferBuilder и начинаем рисование
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (Entity entity : entities) {
            if (entity.isRemoved() || !entity.isAddedToLevel()) continue;

            AABB bb = entity.getBoundingBox()
                    .inflate(0.05)
                    .move(-camPos.x(), -camPos.y(), -camPos.z());

            if (!bb.isInfinite()) {
                drawBox(bufferBuilder, bb, r, g, b, a);
            }
        }

        // Завершаем рисование
        MeshData meshData = bufferBuilder.build();
        if (meshData != null) {
            BufferUploader.drawWithShader(meshData);
        }

        // Восстанавливаем настройки OpenGL
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /**
     * Рисует грани AABB.
     */
    private static void drawBox(BufferBuilder bufferBuilder, AABB box, float r, float g, float b, float a) {
        double x1 = box.minX;
        double y1 = box.minY;
        double z1 = box.minZ;
        double x2 = box.maxX;
        double y2 = box.maxY;
        double z2 = box.maxZ;

        // Нижняя грань
        drawQuad(bufferBuilder, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, r, g, b, a);
        // Верхняя грань
        drawQuad(bufferBuilder, x1, y2, z1, x2, y2, z1, x2, y2, z2, x1, y2, z2, r, g, b, a);
        // Передняя грань
        drawQuad(bufferBuilder, x2, y1, z2, x2, y2, z2, x1, y2, z2, x1, y1, z2, r, g, b, a);
        // Задняя грань
        drawQuad(bufferBuilder, x2, y1, z1, x2, y2, z1, x1, y2, z1, x1, y1, z1, r, g, b, a);
        // Левая грань
        drawQuad(bufferBuilder, x1, y1, z1, x1, y2, z1, x1, y2, z2, x1, y1, z2, r, g, b, a);
        // Правая грань
        drawQuad(bufferBuilder, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, r, g, b, a);
    }

    /**
     * Рисует четырехугольник (QUAD) с заданными координатами и цветом.
     */
    private static void drawQuad(BufferBuilder bufferBuilder, double x1, double y1, double z1,
                                 double x2, double y2, double z2,
                                 double x3, double y3, double z3,
                                 double x4, double y4, double z4,
                                 float r, float g, float b, float a) {
        bufferBuilder.addVertex((float) x1, (float) y1, (float) z1).setColor(r, g, b, a);
        bufferBuilder.addVertex((float) x2, (float) y2, (float) z2).setColor(r, g, b, a);
        bufferBuilder.addVertex((float) x3, (float) y3, (float) z3).setColor(r, g, b, a);
        bufferBuilder.addVertex((float) x4, (float) y4, (float) z4).setColor(r, g, b, a);
    }

}
