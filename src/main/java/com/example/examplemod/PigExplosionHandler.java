//package com.example.examplemod;
//
//import com.mojang.logging.LogUtils;
//import net.minecraft.core.particles.ParticleTypes;
//import net.minecraft.server.level.ServerLevel;
//import net.minecraft.sounds.SoundEvents;
//import net.minecraft.sounds.SoundSource;
//import net.minecraft.world.entity.Entity;
//import net.minecraft.world.entity.EntityType;
//import net.minecraft.world.entity.animal.Pig;
//import net.minecraft.world.entity.item.PrimedTnt;
//import net.minecraft.world.level.Explosion;
//import net.minecraft.world.level.Level;
//import net.neoforged.api.distmarker.Dist;
//import net.neoforged.bus.api.SubscribeEvent;
//import net.neoforged.fml.common.EventBusSubscriber;
//import net.neoforged.fml.common.Mod;
//import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
//import net.neoforged.neoforge.event.tick.ServerTickEvent;
//import net.neoforged.neoforge.server.ServerLifecycleHooks;
//import org.slf4j.Logger;
//
//import java.util.Iterator;
//import java.util.Map;
//import java.util.UUID;
//import java.util.concurrent.ConcurrentHashMap;
//
//@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
//public class PigExplosionHandler {
//    private static final Logger LOGGER = LogUtils.getLogger();
//    private static final Map<UUID, PigData> pigDataMap = new ConcurrentHashMap<>();
//
//    @SubscribeEvent
//    public static void onEntityJoin(EntityJoinLevelEvent event) {
//        if (event.getLevel().isClientSide()) return;
//        if (event.getEntity() instanceof Pig pig) {
//            ServerLevel level = (ServerLevel) event.getLevel();
//            pigDataMap.put(pig.getUUID(), new PigData(level, 60));
//            LOGGER.info("Pig joined: {}", pig.getUUID());
//        }
//    }
//
//    @SubscribeEvent
//    public static void onServerTick(ServerTickEvent.Post event) {
//        Iterator<Map.Entry<UUID, PigData>> iterator = pigDataMap.entrySet().iterator();
//        while (iterator.hasNext()) {
//            Map.Entry<UUID, PigData> entry = iterator.next();
//            UUID pigId = entry.getKey();
//            PigData data = entry.getValue();
//            ServerLevel level = data.level;
//            int countdown = data.countdown;
//
//            Entity entity = level.getEntity(pigId);
//
//            if (entity != null && !entity.isRemoved()) {
//                countdown--;
//                if (countdown <= 0) {
//                    LOGGER.info("Pig exploded: {}", pigId);
//                    PrimedTnt tnt = EntityType.TNT.create(level);
//                    if (tnt != null) {
//                        tnt.setPos(entity.getX(), entity.getY(), entity.getZ());
//                        // Устанавливаем время до взрыва в 1 тик (минимальное)
//                        tnt.setFuse((byte) 1);
//                        level.addFreshEntity(tnt);
//
//                        // Воспроизводим эффект фейерверка при установке TNT
//                        if (level instanceof ServerLevel serverLevel) {
//                            // Частицы: например, красные огоньки
//                            serverLevel.sendParticles(ParticleTypes.FLAME,
//                                    entity.getX(), entity.getY() + 1, entity.getZ(),
//                                    200, // количество частиц
//                                    3, 3, 3, // разброс по осям
//                                    0.1); // задержка
//
//                            // Звук взрыва или что-то подобное для драматического эффекта
//                            serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
//                                    SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.0F, 1.0F);
//                        }
//
//                        double explosionRadius = 100.0; // Радиус взрыва, как у стандартного TNT
//                        boolean fire = false;         // Может вызывать возгорание
//
//                        // Создаем стандартный взрыв
//                        level.explode(
//                                entity, // Источник взрыва
//                                entity.getX(), entity.getY(), entity.getZ(),
//                                (float) explosionRadius,
//                                fire,
//                                Level.ExplosionInteraction.TNT
//                        );
//                    }
//                    iterator.remove();
//                } else {
//                    entry.setValue(new PigData(level, countdown));
//                }
//            } else {
//                iterator.remove();
//            }
//        }
//    }
//
//    private static class PigData {
//        final ServerLevel level;
//        final int countdown;
//
//        PigData(ServerLevel level, int countdown) {
//            this.level = level;
//            this.countdown = countdown;
//        }
//    }
//}