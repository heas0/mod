//package com.example.examplemod;
//
//import com.mojang.logging.LogUtils;
//import net.minecraft.core.BlockPos;
//import net.minecraft.server.MinecraftServer;
//import net.minecraft.server.level.ServerLevel;
//import net.minecraft.world.entity.Entity;
//import net.minecraft.world.entity.EntityType;
//import net.minecraft.world.entity.item.PrimedTnt;
//import net.minecraft.world.entity.monster.Phantom;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.level.block.state.BlockState;
//import net.neoforged.api.distmarker.Dist;
//import net.neoforged.bus.api.SubscribeEvent;
//import net.neoforged.fml.common.EventBusSubscriber;
//import net.neoforged.neoforge.event.tick.ServerTickEvent;
//import org.slf4j.Logger;
//
//@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
//public class PhantomExplosionHandler {
//    private static final Logger LOGGER = LogUtils.getLogger();
//
//    @SubscribeEvent
//    public static void onServerTick(ServerTickEvent.Post event) {
//        MinecraftServer server = event.getServer();
//        ServerLevel level = server.getLevel(Level.OVERWORLD);
//
//        if (level == null || level.isClientSide()) return;
//
//        // Перебираем всех фантомов в мире
//        for (Entity entity : level.getAllEntities()) {
//            if (entity instanceof Phantom phantom) {
//                // Обработка фантома
//                BlockPos pos = BlockPos.containing(phantom.getX(), phantom.getY(), phantom.getZ());
//
//                boolean hit = false;
//
//                // Проверяем все блоки в радиусе 1 блока по горизонтали (всего 8 позиций)
//                for (int dx = -2; dx <= 2; dx++) {
//                    for (int dz = -2; dz <= 2; dz++) {
//                        for (int dy = -2; dy <= 2; dy++) {
//                            BlockPos checkPos = pos.offset(dx, dy, dz);
//
//                            // Проверяем, загружен ли чанк
//                            if (!level.hasChunkAt(checkPos)) continue;
//
//                            BlockState state = level.getBlockState(checkPos);
//
//                            if (!state.isAir()) {
//                                hit = true;
//                                break;
//                            }
//                        }
//                    }
//                    if (hit) break;
//                }
//
//                if (hit) {
//                    // Создаём TNT
//                    PrimedTnt tnt = EntityType.TNT.create(level);
//                    if (tnt != null) {
//                        tnt.setPos(phantom.getX(), phantom.getY(), phantom.getZ());
//                        tnt.setFuse((byte) 1);
//                        level.addFreshEntity(tnt);
//
//                        LOGGER.info("Phantom collided with a block and TNT was spawned at {}", pos);
//                    }
//
//                    // Удаляем фантома
//                    phantom.discard();
//                }
//            }
//        }
//    }
//}