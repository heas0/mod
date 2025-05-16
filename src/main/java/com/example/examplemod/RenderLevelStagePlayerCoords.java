//package com.example.examplemod;
//
//import com.mojang.blaze3d.systems.RenderSystem;
//import com.mojang.blaze3d.vertex.*;
//import net.minecraft.client.Camera;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.renderer.GameRenderer;
//import net.minecraft.world.entity.Entity;
//import net.minecraft.world.entity.player.Player;
//import net.minecraft.world.phys.AABB;
//import net.minecraft.world.phys.Vec3;
//import net.neoforged.api.distmarker.Dist;
//import net.neoforged.bus.api.SubscribeEvent;
//import net.neoforged.fml.common.EventBusSubscriber;
//import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
//import com.mojang.logging.LogUtils;
//import org.slf4j.Logger;
//
//@EventBusSubscriber(modid = "examplemod", value = Dist.CLIENT)
//public class RenderLevelStagePlayerCoords {
//    private static final Logger LOGGER = LogUtils.getLogger();
//
//    @SubscribeEvent
//    public static void onRenderLevelStage(RenderLevelStageEvent event) {
//        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
//
//        Minecraft mc = Minecraft.getInstance();
//        if (mc.player == null || mc.level == null || mc.isPaused()) return;
//
//        for (Entity entity : mc.level.entitiesForRendering()) {
//            if (entity == mc.player || !(entity instanceof Player player)) continue;
//            Vec3 pos = player.position();
//            LOGGER.info("Player '{}' at coords [x={}, y={}, z={}]",
//                    player.getName().getString(),
//                    String.format("%.2f", pos.x),
//                    String.format("%.2f", pos.y),
//                    String.format("%.2f", pos.z)
//            );
//        }
//    }
//}
