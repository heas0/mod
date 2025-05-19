//package com.example.examplemod;
//
//import com.mojang.logging.LogUtils;
//import net.minecraft.client.KeyMapping;
//import net.minecraft.client.Minecraft;
//import net.minecraft.server.level.ServerPlayer;
//import net.minecraft.util.Mth;
//import net.minecraft.world.entity.Entity;
//import net.minecraft.world.entity.monster.Phantom;
//import net.minecraft.world.phys.Vec3;
//import net.neoforged.bus.api.SubscribeEvent;
//import net.neoforged.fml.common.EventBusSubscriber;
//import net.neoforged.neoforge.client.event.InputEvent;
//import net.neoforged.neoforge.event.tick.ServerTickEvent;
//import org.slf4j.Logger;
//
//@EventBusSubscriber(modid = ExampleMod.MODID)
//public class FPVPhantom {
//    private static final Logger LOGGER = LogUtils.getLogger();
//
//    // Серверная часть
//    @SubscribeEvent
//    public static void onServerTick(ServerTickEvent.Post event) {
//        event.getServer().getAllLevels().forEach(level -> {
//            level.getEntities().getAll().forEach(entity -> {
//                if (entity instanceof Phantom phantom) {
//                    // Отключаем все системы управления
//                    phantom.setNoAi(true);
//                    phantom.setNoGravity(true);
//                    phantom.getNavigation().stop();
//                }
//            });
//        });
//        event.getServer().getAllLevels().forEach(level -> {
//            level.players().forEach(player -> {
//                if (player.isSpectator() && player.getCamera() instanceof Phantom phantom) {
//                    LOGGER.info("spec {}", player.isSpectator());
//                    LOGGER.info("ins{}", player.getCamera() instanceof Phantom);
//                    Minecraft mc = Minecraft.getInstance();
//                    if (mc.player == null) return;
//
//                    KeyMapping keyForward = mc.options.keyUp;
//                    KeyMapping keyBack = mc.options.keyDown;
//                    KeyMapping keyLeft = mc.options.keyLeft;
//                    KeyMapping keyRight = mc.options.keyRight;
//                    KeyMapping keyJump = mc.options.keyJump;
//                    KeyMapping keyDrop = mc.options.keyDrop;
//
//                    Vec3 movement = phantom.position();
//
//                    if (keyForward.isDown()) movement = movement.add(0, 0, 1);
//                    if (keyBack.isDown()) movement = movement.add(0, 0, -1);
//                    if (keyLeft.isDown()) movement = movement.add(1, 0, 0);
//                    if (keyRight.isDown()) movement = movement.add(-1, 0, 0);
//                    if (keyJump.isDown()) movement = movement.add(0, 1, 0);
//                    if (keyDrop.isDown()) movement = movement.add(0, -1, 0);
//
//                    handlePhantomMovement(phantom, movement);
//                }
//            });
//        });
//    }
//
//    private static void handlePhantomMovement(Phantom phantom, Vec3 movement) {
//        phantom.teleportTo(movement.x, movement.y, movement.z);
//        phantom.setYRot(phantom.getYRot()); // Фиксация поворота
//    }
//
//}
//
