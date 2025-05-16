package com.example.examplemod;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

import static com.example.examplemod.BartioneHelper.*;
import static com.example.examplemod.RenderHelper.addEntity;
import static com.example.examplemod.RenderHelper.clearEntities;

@EventBusSubscriber(modid = ExampleMod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class VillageTrade {
    public enum BotState {
        FIND,        // Поиск жителей
        NEAR_AND_AIM, // Подход и наведение
        TRADING      // Торговля
    }
    private static BotState currentState = BotState.FIND;
    // Клавиши управления
    public static final KeyMapping TOGGLE_KEY = new KeyMapping(
            "key.examplemod.enable",
            GLFW.GLFW_KEY_EQUAL,
            "key.category.examplemod"
    );
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Minecraft minecraft = Minecraft.getInstance();
    private static boolean enabled = false;
    private static boolean wasTrade = false;
    private static final int MAX_WAIT_TIME = 5000; // 5 секунд ожидания экрана торговли
    // Состояние для обработки жителей
    private static Queue<Villager> villagerQueue = new LinkedList<>();
    private static Villager currentVillager = null;
    private static long startTime = 0L;
    private static long totalStartTime = 0L;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Player player = minecraft.player;
        if (player == null) return;

        if (TOGGLE_KEY.consumeClick()) {
            enabled = !enabled;
            if (enabled) {
                currentState = BotState.FIND; // Сброс состояния при включении
            } else {
                resetState();
            }
            LOGGER.info("Mod {}", enabled ? "ENABLED" : "DISABLED");
        }

        if (!enabled) return;

        switch (currentState) {
            case FIND -> handleFindState(player);
            case NEAR_AND_AIM -> handleNearAndAimState(player);
            case TRADING -> handleTradingState(player);
        }

    }

    /**
     * Находит ближайших жителей к игроку
     */
    public static Optional<List<Villager>> findNearestVillagers(double radius) {
        Player player = minecraft.player;
        Level level = minecraft.level;

        if (player == null || level == null) {
            return Optional.empty();
        }

        BlockPos playerPos = player.blockPosition();
        AABB searchBox = new AABB(
                playerPos.getX() - radius, playerPos.getY() - radius, playerPos.getZ() - radius,
                playerPos.getX() + radius, playerPos.getY() + radius, playerPos.getZ() + radius
        );
        List<Villager> villagers = level.getEntitiesOfClass(Villager.class, searchBox);
        LOGGER.info("Found {} villagers.", villagers.size());

        return Optional.of(villagers);
    }

    private static void resetState() {
        villagerQueue.clear();
        currentVillager = null;
        startTime = 0L;
        totalStartTime = 0L;
        clearEntities();
        stopAiming();
    }
    private static void handleFindState(Player player) {
        Optional<List<Villager>> villagersOpt = findNearestVillagers(5);
        if (villagersOpt.isPresent() && !villagersOpt.get().isEmpty()) {
            List<Villager> villagers = villagersOpt.get();
            villagers.forEach(villager -> addEntity(villager));

            // Инициализируем очередь и начальный таймер
            villagerQueue = new LinkedList<>(villagers);
            currentVillager = villagerQueue.poll();
            totalStartTime = System.currentTimeMillis();

            if (currentVillager != null) {
                setGoal(currentVillager);
                startAiming(currentVillager);
                startTime = System.currentTimeMillis();
                LOGGER.info("Starting with villager: {}", currentVillager);
                currentState = BotState.NEAR_AND_AIM; // Переход к следующему состоянию
            }
        } else {
            LOGGER.warn("No villagers found nearby.");
            enabled = false;
        }
    }
    private static void handleNearAndAimState(Player player) {
        if (currentVillager == null) {
            currentState = BotState.FIND; // Если житель исчез, ищем новых
            return;
        }

        updateAiming(); // Обновляем наведение

        // Проверяем достижение цели
        if (hasReachedTarget(player, currentVillager) && hasReachedAim()) {
            LOGGER.info("Reached villager: {}", currentVillager);
            stopAiming(); // Останавливаем наведение
            if(!wasTrade) {
                currentState = BotState.TRADING; // Переход к торговле
            } else {
                // Переход к следующему жителю
                currentVillager = villagerQueue.poll();
                if (currentVillager != null) {
                    setGoal(currentVillager); // Обновляем цель для Baritone
                    startAiming(currentVillager); // Начинаем наведение на нового жителя
                    wasTrade = false;
                    startTime = System.currentTimeMillis(); // Сбрасываем таймер
                } else {
                    long totalElapsed = System.currentTimeMillis() - totalStartTime;
                    LOGGER.info("All villagers processed in {} ms", totalElapsed);
                    clearEntities();
                }
            }
        }
    }
    private static void handleTradingState(Player player) {
        if (currentVillager == null) {
            currentState = BotState.FIND;
            return;
        }

        player.interactOn(currentVillager, InteractionHand.MAIN_HAND);

        if (!(minecraft.screen instanceof MerchantScreen merchantScreen)) {
            return;
        }

        MerchantMenu merchantMenu = merchantScreen.getMenu();
        List<MerchantOffer> offers = merchantMenu.getOffers();

        for (MerchantOffer offer : offers) {
            if (isValidClayTrade(offer)) {
                processClayTrade(player, merchantMenu, offer);
                return;
            }
        }

        LOGGER.warn("Не найдено подходящих предложений");
        gotoNextVillager();
    }

    private static boolean isValidClayTrade(MerchantOffer offer) {
        return offer.getCostA().is(Items.CLAY_BALL)
                && offer.getCostB().isEmpty()
                && offer.getResult().is(Items.EMERALD);
    }

    private static void processClayTrade(Player player, MerchantMenu menu, MerchantOffer offer) {
        int requiredClay = offer.getCostA().getCount();
        int availableClay = getItemCount(player, Items.CLAY_BALL);

        if (availableClay < requiredClay) {
            LOGGER.warn("Недостаточно глины: {}/{}", availableClay, requiredClay);
            gotoNextVillager();
            return;
        }

        try {
            // Установка и обработка выбранного трейда
            int tradeIndex = menu.getOffers().indexOf(offer);
            menu.setSelectionHint(tradeIndex);
            menu.tryMoveItems(tradeIndex);

            // Проверка заполнения слотов оплаты
            MerchantContainer tradeContainer = menu.tradeContainer;
            if (isPaymentValid(tradeContainer, requiredClay)) {
                confirmTrade(menu, tradeIndex);
                LOGGER.info("Успешный обмен {} глины", requiredClay);
            } else {
                LOGGER.warn("Ошибка заполнения слотов оплаты");
            }
        } catch (Exception e) {
            LOGGER.error("Ошибка при обмене: ", e);
        }

        gotoNextVillager();
    }

    private static boolean isPaymentValid(MerchantContainer container, int required) {
        ItemStack payment = container.getItem(0);
        return payment.getCount() >= required
                && payment.is(Items.CLAY_BALL)
                && container.getItem(1).isEmpty();
    }

    private static void confirmTrade(MerchantMenu menu, int tradeIndex) {
        if (minecraft.getConnection() == null) return;

        // Отправка пакета подтверждения трейда
        minecraft.getConnection().send(
                new ServerboundSelectTradePacket(
                        tradeIndex
                )
        );

        // Синхронизация состояния
        menu.slotsChanged(menu.tradeContainer);
    }

    /**
     * Переход к следующему жителю или завершение работы
     */
    private static void gotoNextVillager() {
        // Закрываем интерфейс торговли
        if (minecraft.screen instanceof MerchantScreen) {
            assert minecraft.player != null;
            minecraft.player.closeContainer();
        }
        wasTrade = true;
        currentState = BotState.NEAR_AND_AIM;
    }

}