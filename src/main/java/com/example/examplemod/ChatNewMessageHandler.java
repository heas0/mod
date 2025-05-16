//package com.example.examplemod;
//
//import com.mojang.logging.LogUtils;
//import net.minecraft.client.multiplayer.ClientPacketListener;
//import net.minecraft.client.multiplayer.MultiPlayerGameMode;
//import net.minecraft.network.Connection;
//import net.minecraft.network.chat.Component;
//import net.minecraft.network.chat.LastSeenMessages;
//import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
//import net.minecraft.network.protocol.game.ServerboundChatPacket;
//import net.minecraft.world.entity.player.Player;
//import net.neoforged.bus.api.SubscribeEvent;
//import net.neoforged.fml.common.EventBusSubscriber;
//import net.neoforged.neoforge.client.event.ClientChatEvent;
//import net.minecraft.client.Minecraft;
//import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
//import net.neoforged.neoforge.event.ServerChatEvent;
//import org.slf4j.Logger;
//
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.time.Instant;
//import java.util.ArrayList;
//import java.util.BitSet;
//import java.util.List;
//import java.util.Random;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.atomic.AtomicLong;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//@EventBusSubscriber(modid = ExampleMod.MODID)
//public class ChatNewMessageHandler {
//    private static final AtomicLong lastRequestTime = new AtomicLong(0);
//    private static final long COOLDOWN_MS = 15_000; // 10 секунд в миллисекундах
//    private static final String FILE_PATH = "C:\\Users\\amero\\source\\message.txt";
//    private static final Logger LOGGER = LogUtils.getLogger();
//    private static final OllamaChat ollamaChat = new OllamaChat();
//    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
//    private static final String BOT_NAME = "heas0";
//
//    @SubscribeEvent
//    public static void onChatMessage(ClientChatReceivedEvent event) {
//        // Атомарная проверка и установка флага обработки
//        if (!cooldownCheck()) {
//            LOGGER.info("Request skipped - cooldown active");
//            return;
//        }
//        LOGGER.info("Request accepted");
//        String message = extractUserMessage(event.getMessage().toString());
//        // Проверяем, содержит ли строка "heas0"
////        if (!message.contains("акакий")) {
////            return;
////        }
//        // Создаем родительские директории, если их нет
//        File file = new File(FILE_PATH);
//
//        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
//            writer.write("message: " + message);
//            writer.newLine(); // Переход на новую строку
//        } catch (IOException e) {
//            System.err.println("Ошибка при записи в файл: " + e.getMessage());
//            e.printStackTrace();
//        }
//        LOGGER.info("message: {}", message);
//
//        // Обработка в отдельном потоке
//        executor.submit(() -> {
//            try {
//                String response = processMessage(message); // Ваш метод обработки через Ollama
//
//                // Ограничиваем длину до 256 символов
//                if (response.length() > 256) {
//                    response = response.substring(0, 256);
//                }
//
//                // Удаляем недопустимые символы (включая кириллицу)
//                response = response.replaceAll("[^a-zA-Z0-9а-яА-ЯёЁ .,!?]", "");
//
//                // Проверка на пустое сообщение
//                if (response.isEmpty()) {
//                    response = "...";
//                }
//                LOGGER.info(response);
//                sendBotMessageToServer(response);
//            } catch (Exception e) {
//                LOGGER.error("Error processing chat input", e);
//                displayErrorMessage();
//            }
//        });
//    }
//    public static String extractUserMessage(String messageLine) {
//        // Регулярное выражение для поиска всех блоков literal{...}
//        String regex = "literal\\{([^}]*)}";
//        Pattern pattern = Pattern.compile(regex);
//        Matcher matcher = pattern.matcher(messageLine);
//
//        List<String> literals = new ArrayList<>();
//
//        // Сбор всех совпадений
//        while (matcher.find()) {
//            literals.add(matcher.group(1)); // Добавляем текст из первой группы (внутри literal{})
//        }
//
//        // Возвращаем последний найденный literal, если есть совпадения
//        if (!literals.isEmpty()) {
//            return literals.get(literals.size() - 1);
//        }
//
//        return null; // Если совпадений нет
//    }
//
//    private static String processMessage(String input) throws OllamaException {
//        return ollamaChat.sendMessage(input);
//    }
//
//    private static void sendBotMessageToServer(String message) {
//        Minecraft mc = Minecraft.getInstance();
//        if (mc.player == null || mc.getConnection() == null) return;
//
//        ClientPacketListener connection = mc.getConnection();
//        Connection networkManager = connection.getConnection();
//
//        // Generate random salt
//        long salt = new Random().nextLong();
//
//        // Create empty last-seen update
//        LastSeenMessages.Update lastSeenUpdate = new LastSeenMessages.Update(0, new BitSet());
//
//        // Create chat packet with null signature (server may reject it)
//        ServerboundChatPacket packet = new ServerboundChatPacket(
//                message,
//                Instant.now(),
//                salt,
//                null, // Signature (requires private key)
//                lastSeenUpdate
//        );
//
//        networkManager.send(packet);
//    }
//
//    private static void displayErrorMessage() {
//        Minecraft.getInstance().execute(() -> {
//            if (Minecraft.getInstance().player != null) {
//                Minecraft.getInstance().gui.getChat().addMessage(
//                        Component.literal("[" + BOT_NAME + "]: Error processing request")
//                );
//            }
//        });
//    }
//    private static boolean cooldownCheck() {
//        long now = System.currentTimeMillis();
//        long last = lastRequestTime.get();
//        LOGGER.info("Cooldown check: " + (now - last));
//        if ((now - last) >= COOLDOWN_MS) {
//            lastRequestTime.compareAndSet(last, now);
//            return true;
//        } else {
//            return false;
//        }
//
//    }
//}