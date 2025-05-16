//// KeyRegistration.java
//package com.example.examplemod;
//
//import net.neoforged.bus.api.SubscribeEvent;
//import net.neoforged.fml.common.EventBusSubscriber;
//import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
//
//@EventBusSubscriber(modid = ExampleMod.MODID, bus = EventBusSubscriber.Bus.MOD)
//public class KeyRegistration {
//    @SubscribeEvent
//    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
//        event.register(RenderLevelStage.TOGGLE_ON_KEY);
//        event.register(RenderLevelStage.TOGGLE_OFF_KEY);
//    }
//}