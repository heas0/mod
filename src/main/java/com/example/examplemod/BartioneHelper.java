package com.example.examplemod;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalNear;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;

@EventBusSubscriber(modid = ExampleMod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class BartioneHelper {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Minecraft mc = Minecraft.getInstance();
    private static final float ROTATION_SPEED = 60.0f;
    private static final float AIM_THRESHOLD = 0.01f;
    private static Entity currentTarget = null;

    private static float currentYaw;
    private static float currentPitch;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if(currentTarget != null){
            updateAiming();
        }
}

    public static void setGoal(Entity entity) {
        if (BaritoneAPI.getProvider().getBaritoneForPlayer(mc.player) == null) return;

        BlockPos pos = BlockPos.containing(entity.getX(), entity.getY(), entity.getZ());
        GoalNear goal = new GoalNear(pos, 3);

        BaritoneAPI.getSettings().allowBreak.value = false;
        BaritoneAPI.getSettings().allowPlace.value = false;

        BaritoneAPI.getProvider().getPrimaryBaritone()
                .getCustomGoalProcess()
                .setGoalAndPath(goal);
    }

    public static boolean hasReachedGoal() {
        return !BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().isActive();
    }

    public static boolean hasReachedAim() {
        if (currentTarget == null || mc.player == null) return false;

        float targetYaw = calculateTargetYaw();
        float targetPitch = calculateTargetPitch();

        float deltaYaw = Math.abs(normalizeAngle(targetYaw) - normalizeAngle(currentYaw));
        float deltaPitch = Math.abs(normalizeAngle(targetPitch) - normalizeAngle(currentPitch));

        if (deltaYaw > 180.0f) deltaYaw = 360.0f - deltaYaw;
        return deltaYaw <= AIM_THRESHOLD && deltaPitch <= AIM_THRESHOLD;
    }

    public static void startAiming(Entity target) {
        currentTarget = target;
        if (mc.player != null) {
            currentYaw = mc.player.getYRot();
            currentPitch = mc.player.getXRot();
        }
    }

    public static void stopAiming() {
        currentTarget = null;
    }


    public static void stopGoal() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
    }

    public static void updateAiming() {
        if (mc.player == null || currentTarget == null) return;

        float targetYaw = calculateTargetYaw();
        float targetPitch = calculateTargetPitch();

        currentYaw = normalizeAngle(currentYaw);
        targetYaw = normalizeAngle(targetYaw);
        currentPitch = normalizeAngle(currentPitch);
        targetPitch = normalizeAngle(targetPitch);

        float deltaYaw = targetYaw - currentYaw;
        if (deltaYaw < -180.0f) deltaYaw += 360.0f;
        if (deltaYaw > 180.0f) deltaYaw -= 360.0f;

        float deltaPitch = targetPitch - currentPitch;
        if (deltaPitch < -180.0f) deltaPitch += 360.0f;
        if (deltaPitch > 180.0f) deltaPitch -= 360.0f;

        float delta = ROTATION_SPEED * 0.05f;
        deltaYaw = Math.abs(deltaYaw) > delta ? delta * Math.signum(deltaYaw) : deltaYaw;
        deltaPitch = Math.abs(deltaPitch) > delta ? delta * Math.signum(deltaPitch) : deltaPitch;

        currentYaw += deltaYaw;
        currentPitch += deltaPitch;
        currentPitch = Math.max(-90.0f, Math.min(90.0f, currentPitch));

        mc.player.setYRot(currentYaw % 360.0f);
        mc.player.setXRot(currentPitch);
    }

    private static float calculateTargetYaw() {
        if (currentTarget == null || mc.player == null) return 0f;
        double dX = currentTarget.getX() - mc.player.getX();
        double dZ = currentTarget.getZ() - mc.player.getZ();
        return (float) Math.toDegrees(Math.atan2(dZ, dX)) - 90.0f;
    }

    private static float calculateTargetPitch() {
        if (currentTarget == null || mc.player == null) return 0f;
        double dX = currentTarget.getX() - mc.player.getX();
        double dY = currentTarget.getEyeY() - mc.player.getEyeY();
        double dZ = currentTarget.getZ() - mc.player.getZ();
        double distance = Math.sqrt(dX * dX + dZ * dZ);
        return (float) Math.toDegrees(Math.atan2(dY, distance));
    }

    private static float normalizeAngle(float angle) {
        angle = angle % 360.0f;
        return angle < 0 ? angle + 360.0f : angle;
    }
}