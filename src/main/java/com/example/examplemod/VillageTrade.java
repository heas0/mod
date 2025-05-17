//package com.example.examplemod;
//
//import com.mojang.logging.LogUtils;
//import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
//import net.minecraft.client.KeyMapping;
//import net.minecraft.client.Minecraft;
//import net.minecraft.core.BlockPos;
//import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
//import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
//import net.minecraft.network.protocol.game.ServerboundInteractPacket;
//import net.minecraft.world.InteractionHand;
//import net.minecraft.world.entity.npc.Villager;
//import net.minecraft.world.entity.npc.VillagerProfession;
//import net.minecraft.world.entity.player.Player;
//import net.minecraft.world.inventory.ClickType;
//import net.minecraft.world.inventory.MerchantMenu;
//import net.minecraft.world.inventory.Slot;
//import net.minecraft.world.item.ItemStack;
//import net.minecraft.world.item.Items;
//import net.minecraft.world.item.trading.MerchantOffer;
//import net.minecraft.world.item.trading.MerchantOffers;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.phys.AABB;
//import net.neoforged.api.distmarker.Dist;
//import net.neoforged.bus.api.SubscribeEvent;
//import net.neoforged.fml.common.EventBusSubscriber;
//import net.neoforged.neoforge.client.event.ClientTickEvent;
//import org.lwjgl.glfw.GLFW;
//import org.slf4j.Logger;
//
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Optional;
//import java.util.Queue;
//
//import static com.example.examplemod.BartioneHelper.*;
//import static com.example.examplemod.RenderHelper.addEntity;
//import static com.example.examplemod.RenderHelper.clearEntities;
//
//@EventBusSubscriber(modid = ExampleMod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
//public class VillageTrade {
//    public enum BotState {
//        FIND,        // Поиск жителей
//        NEAR_AND_AIM, // Подход и наведение
//        TRADING      // Торговля
//    }
//    private static BotState currentState = BotState.FIND;
//    // Клавиши управления
//    public static final KeyMapping TOGGLE_KEY = new KeyMapping(
//            "key.examplemod.enable",
//            GLFW.GLFW_KEY_EQUAL,
//            "key.category.examplemod"
//    );
//    private static final Logger LOGGER = LogUtils.getLogger();
//    private static final Minecraft minecraft = Minecraft.getInstance();
//    private static boolean enabled = false;
//    private static boolean wasTrade = false;
//    // Состояние для обработки жителей
//    private static Queue<Villager> villagerQueue = new LinkedList<>();
//    private static Villager currentVillager = null;
//
//    private static int timeout = 0;
//
//
//    @SubscribeEvent
//    public static void onClientTick(ClientTickEvent.Post event) {
//        Player player = minecraft.player;
//        if (player == null) return;
//
//        if (TOGGLE_KEY.consumeClick()) {
//            enabled = !enabled;
//            if (enabled) {
//                currentState = BotState.FIND; // Сброс состояния при включении
//            } else {
//                resetState();
//            }
//            LOGGER.info("Mod {}", enabled ? "ENABLED" : "DISABLED");
//        }
//
//        if (!enabled) return;
//
//        switch (currentState) {
//            case FIND -> handleFindState(player);
//            case NEAR_AND_AIM -> handleNearAndAimState(player);
//            case TRADING -> handleTradingState(player);
//        }
//
//    }
//
//    /**
//     * Находит ближайших жителей к игроку
//     */
//    public static Optional<List<Villager>> findNearestVillagers(double radius) {
//        Player player = minecraft.player;
//        Level level = minecraft.level;
//
//        if (player == null || level == null) {
//            return Optional.empty();
//        }
//
//        BlockPos playerPos = player.blockPosition();
//        AABB searchBox = new AABB(
//                playerPos.getX() - radius, playerPos.getY() - radius, playerPos.getZ() - radius,
//                playerPos.getX() + radius, playerPos.getY() + radius, playerPos.getZ() + radius
//        );
//        List<Villager> villagers = level.getEntitiesOfClass(Villager.class, searchBox);
//        LOGGER.info("Found {} villagers.", villagers.size());
//
//        return Optional.of(villagers.stream().filter(v -> v.getVillagerData().getProfession() == VillagerProfession.MASON).toList());
//    }
//
//    private static void resetState() {
//        villagerQueue.clear();
//        currentVillager = null;
//        clearEntities();
//        stopAiming();
//    }
//    private static void handleFindState(Player player) {
//        Optional<List<Villager>> villagersOpt = findNearestVillagers(30);
//        if (villagersOpt.isPresent() && !villagersOpt.get().isEmpty()) {
//            List<Villager> villagers = villagersOpt.get();
//            villagers.forEach(villager -> addEntity(villager));
//
//            // Инициализируем очередь и начальный таймер
//            villagerQueue = new LinkedList<>(villagers);
//            currentVillager = villagerQueue.poll();
//
//            if (currentVillager != null) {
//                setGoal(currentVillager);
//                startAiming(currentVillager);
//                LOGGER.info("Starting with villager: {}", currentVillager);
//                currentState = BotState.NEAR_AND_AIM; // Переход к следующему состоянию
//            }
//        } else {
//            LOGGER.warn("No villagers found nearby.");
//            enabled = false;
//        }
//    }
//    private static void handleNearAndAimState(Player player) {
//        if (currentVillager == null) {
//            currentState = BotState.FIND;
//            return;
//        }
//
//        updateAiming();
//
//        if (hasReachedTarget(player, currentVillager)) {
//            stopGoal();
//            if (hasReachedAim()) {
//                LOGGER.info("Reached villager: {}", currentVillager);
//                stopAiming();
//                if (!wasTrade) {
//                    currentState = BotState.TRADING; // Только переход в торговлю
//                } else {
//                    moveToNextVillager(); // Вынесено в отдельный метод
//                }
//            }
//        }
//    }
//    private static void handleTradingState(Player player) {
//        if (currentVillager == null) {
//            currentState = BotState.FIND;
//            return;
//        }
//        if (timeout < 10) {
//            timeout++;
//            return;
//        }
//        timeout= 0;
//
//        ServerboundInteractPacket interactPacket = ServerboundInteractPacket.createInteractionPacket(
//                currentVillager, false, InteractionHand.MAIN_HAND
//        );
//        if (minecraft.getConnection() != null) {
//            minecraft.getConnection().send(interactPacket);
//        }
//
//        // Проверяем, открыто ли меню торговца
//        if (!(player.containerMenu instanceof MerchantMenu merchantMenu)) {
//            return;
//        }
//
//        MerchantOffers offers = merchantMenu.getOffers();
//        int cost = -1;
//
//        // Поиск предложения (глина → изумруды)
//        for (int i = 0; i < offers.size(); i++) {
//            MerchantOffer offer = offers.get(i);
//            if (offer.getResult().getItem() == Items.EMERALD &&
//                    offer.getCostA().getItem() == Items.CLAY_BALL) {
//                cost = offer.getCostA().getCount();
//                break;
//            }
//        }
//
//        if (cost != -1 && cost == 10) {
//            // Шаг 4: Проверка наличия глины в слоте оплаты (слот 0)
//            Slot paymentSlot = merchantMenu.getSlot(0);
//            if (paymentSlot.getItem().getItem() != Items.CLAY_BALL) {
//                // Шаг 5: Поиск глины в инвентаре (слоты 3-39)
//                int claySlot = -1;
//                for (int i = 3; i < 39; i++) {
//                    Slot slot = merchantMenu.getSlot(i);
//                    if (!slot.getItem().isEmpty() && slot.getItem().getItem() == Items.CLAY_BALL
//                    && slot.getItem().getCount() >= cost) {
//                        claySlot = i;
//                        break;
//                    }
//                }
//
//                if (claySlot == -1) {
//                    LOGGER.info("Глина не найдена в инвентаре!");
//                    return;
//                }
//
//                // Шаг 6: Клик по глине в инвентаре (выбор предмета)
//                ServerboundContainerClickPacket pickClayPacket = new ServerboundContainerClickPacket(
//                        merchantMenu.containerId,
//                        merchantMenu.incrementStateId(),
//                        claySlot,
//                        0, ClickType.PICKUP,
//                        ItemStack.EMPTY,
//                        Int2ObjectMaps.emptyMap()
//                );
//                minecraft.getConnection().send(pickClayPacket);
//
//                // Шаг 7: Клик по слоту оплаты (0) для помещения глины
//                ServerboundContainerClickPacket placeClayPacket = new ServerboundContainerClickPacket(
//                        merchantMenu.containerId,
//                        merchantMenu.incrementStateId(),
//                        0,
//                        0, ClickType.PICKUP,
//                        ItemStack.EMPTY,
//                        Int2ObjectMaps.emptyMap()
//                );
//                minecraft.getConnection().send(placeClayPacket);
//
//            }
//
//            // Перемещение предметов (например, забираем изумруды)
//            ServerboundContainerClickPacket clickPacket = new ServerboundContainerClickPacket(
//                    merchantMenu.containerId,
//                    merchantMenu.incrementStateId(),
//                    2, 0, ClickType.QUICK_MOVE, ItemStack.EMPTY,
//                    Int2ObjectMaps.emptyMap() // Пустая карта изменений
//            );
//            minecraft.getConnection().send(clickPacket);
//        }
//        // Закрытие контейнера
//        ServerboundContainerClosePacket closePacket = new ServerboundContainerClosePacket(merchantMenu.containerId);
//        minecraft.getConnection().send(closePacket);
//        player.closeContainer();
//        moveToNextVillager();
//    }
//
//    private static void moveToNextVillager() {
//        if (!villagerQueue.isEmpty()) {
//            currentVillager = villagerQueue.poll();
//            if (currentVillager != null) {
//                setGoal(currentVillager);
//                startAiming(currentVillager);
//                wasTrade = false;
//            }
//        } else {
//            LOGGER.info("No more villagers in queue.");
//            resetState();
//        }
//        currentState = BotState.NEAR_AND_AIM;
//    }
//
//}