package com.example.examplemod;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

import static com.example.examplemod.BartioneHelper.*;
import static com.example.examplemod.RenderHelper.addEntity;
import static com.example.examplemod.RenderHelper.clearEntities;

@EventBusSubscriber(modid = ExampleMod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class VillagerTrader {
    // Состояния
    public enum State {
        STARTED,
        FINDING,
        MOVING,
        AIMING,
        TRADING,
        SOLVER,
        STOPED
    }
    // Клавиши управления
    public static final KeyMapping TOGGLE_KEY = new KeyMapping(
            "key.examplemod.enable",
            GLFW.GLFW_KEY_EQUAL,
            "key.category.examplemod"
    );
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Minecraft minecraft = Minecraft.getInstance();
    private static State currentState = null;
    private static Villager currentVillager = null;
    private static VillagerProfession currentVillagerProfession = null;
    private static Queue<Villager> villagerQueue = new LinkedList<>();
    private static final double radiusSearchVillager = 100;
    private static boolean isEnabled = false;
    private static boolean hasGoalPath = false;
    private static boolean hasGoalAim = false;
    private static int timeToTrade = 0;
    
    
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Player player = minecraft.player;
        if (player == null) return;

        if (TOGGLE_KEY.consumeClick()) {
            isEnabled = !isEnabled;
            if (isEnabled) {
                currentState = State.STARTED;
            } else {
                handleStop();
            }
            LOGGER.info("Mod {}", isEnabled ? "isEnabled" : "DISABLED");
        }

        if (!isEnabled) return;

        switch (currentState) {
            case STARTED -> handleStart();
            case FINDING -> handleFindVillagers();
            case MOVING -> handleMoveToNextVillager();
            case AIMING -> handleAimToVillager();
            case TRADING -> handleTradeToVillager();
            case SOLVER -> handleSolver();
            case STOPED-> handleStop();
        }
    }

    private static void handleStart() {
        currentVillagerProfession = VillagerProfession.MASON;
        currentState = State.FINDING;
    }
    private static void handleFindVillagers() {
        Player player = minecraft.player;
        Level level = minecraft.level;
        if (player == null || level == null) {
            currentState = State.SOLVER;
            return;
        }

        List<Villager> villagers = level.getEntitiesOfClass(Villager.class, getSearchBox());
        List<Villager> filtered = villagers.stream()
                .filter(v -> v.getVillagerData().getProfession() == currentVillagerProfession)
                .toList();
        villagerQueue = new LinkedList<>(filtered);
        if (villagerQueue.isEmpty()) {
            currentState = State.SOLVER;
            return;
        } else {
            currentState = State.MOVING;
            villagerQueue.forEach(villager -> addEntity(villager));
        }
    }
    private static void handleMoveToNextVillager() {
        if(!hasGoalPath) {
            currentVillager = villagerQueue.poll();
            if (currentVillager == null) {
                currentState = State.SOLVER;
                stopGoal();
                return;
            } else {
                setGoal(currentVillager);
                hasGoalPath = true;
                LOGGER.info("goal set");
            }
        }
        if(hasReachedGoal()){
            hasGoalPath = false;
            currentState = State.AIMING;
            LOGGER.info("goal reached");
            return;
        }
    }

    private static void handleAimToVillager() {
        if(!hasGoalAim) {
            if (currentVillager == null) {
                currentState = State.SOLVER;
                stopAiming();
                return;
         }
            startAiming(currentVillager);
            hasGoalAim = true;
            LOGGER.info("aim set");
        }
        if(hasReachedAim()) {
            stopAiming();
            hasGoalAim = false;
            currentState = State.TRADING;
        }
    }
    private static void handleTradeToVillager() {
        Player player = minecraft.player;
        if(player == null){
            currentState= State.STOPED;
        }
        if (currentVillager == null) {
            currentState = State.SOLVER;
            return;
        }
        if (timeToTrade < 10) {
            timeToTrade++;
            return;
        }
        timeToTrade= 0;

        ServerboundInteractPacket interactPacket = ServerboundInteractPacket.createInteractionPacket(
                currentVillager, false, InteractionHand.MAIN_HAND
        );
        if (minecraft.getConnection() != null) {
            minecraft.getConnection().send(interactPacket);
        }

        // Проверяем, открыто ли меню торговца
        if (!(player.containerMenu instanceof MerchantMenu merchantMenu)) {
            return;
        }

        MerchantOffers offers = merchantMenu.getOffers();
        int cost = -1;

        // Поиск предложения (глина → изумруды)
        for (int i = 0; i < offers.size(); i++) {
            MerchantOffer offer = offers.get(i);
            if (offer.getResult().getItem() == Items.EMERALD &&
                    offer.getCostA().getItem() == Items.CLAY_BALL) {
                cost = offer.getCostA().getCount();
                break;
            }
        }

        if (cost != -1 && cost == 10) {
            // Шаг 4: Проверка наличия глины в слоте оплаты (слот 0)
            Slot paymentSlot = merchantMenu.getSlot(0);
            if (paymentSlot.getItem().getItem() != Items.CLAY_BALL) {
                // Шаг 5: Поиск глины в инвентаре (слоты 3-39)
                int claySlot = -1;
                for (int i = 3; i < 39; i++) {
                    Slot slot = merchantMenu.getSlot(i);
                    if (!slot.getItem().isEmpty() && slot.getItem().getItem() == Items.CLAY_BALL
                    && slot.getItem().getCount() >= cost) {
                        claySlot = i;
                        break;
                    }
                }

                if (claySlot == -1) {
                    LOGGER.info("Глина не найдена в инвентаре!");
                    return;
                }

                // Шаг 6: Клик по глине в инвентаре (выбор предмета)
                ServerboundContainerClickPacket pickClayPacket = new ServerboundContainerClickPacket(
                        merchantMenu.containerId,
                        merchantMenu.incrementStateId(),
                        claySlot,
                        0, ClickType.PICKUP,
                        ItemStack.EMPTY,
                        Int2ObjectMaps.emptyMap()
                );
                minecraft.getConnection().send(pickClayPacket);

                // Шаг 7: Клик по слоту оплаты (0) для помещения глины
                ServerboundContainerClickPacket placeClayPacket = new ServerboundContainerClickPacket(
                        merchantMenu.containerId,
                        merchantMenu.incrementStateId(),
                        0,
                        0, ClickType.PICKUP,
                        ItemStack.EMPTY,
                        Int2ObjectMaps.emptyMap()
                );
                minecraft.getConnection().send(placeClayPacket);

            }

            // Перемещение предметов (например, забираем изумруды)
            ServerboundContainerClickPacket clickPacket = new ServerboundContainerClickPacket(
                    merchantMenu.containerId,
                    merchantMenu.incrementStateId(),
                    2, 0, ClickType.QUICK_MOVE, ItemStack.EMPTY,
                    Int2ObjectMaps.emptyMap() // Пустая карта изменений
            );
            minecraft.getConnection().send(clickPacket);
        }
        // Закрытие контейнера
        ServerboundContainerClosePacket closePacket = new ServerboundContainerClosePacket(merchantMenu.containerId);
        minecraft.getConnection().send(closePacket);
        player.closeContainer();
        currentState = State.MOVING;
    }
    private static void handleSolver() {
        currentState = State.FINDING;
    }
    private static void handleStop() {
        clear();
        isEnabled = false;
    }
    private static void clear() {
        villagerQueue.clear();
        currentVillager = null;
        currentVillagerProfession = null;
        hasGoalPath = false;
        hasGoalAim = false;
        clearEntities();
        stopGoal();
        stopAiming();

    }
    private static AABB getSearchBox() {
        Player player = minecraft.player;
        BlockPos playerPos = player.blockPosition();
        AABB searchBox = new AABB(
                playerPos.getX() - radiusSearchVillager, playerPos.getY() - radiusSearchVillager, playerPos.getZ() - radiusSearchVillager,
                playerPos.getX() + radiusSearchVillager, playerPos.getY() + radiusSearchVillager, playerPos.getZ() + radiusSearchVillager
        );
        return searchBox;
    }
}
