package com.example.examplemod;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalNear;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;

public class BartioneHelper {
    private static boolean isAiming = false;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Minecraft mc = Minecraft.getInstance();
    private static final float ROTATION_SPEED = 60.0f; // Градусов в секунду
    private static final float AIM_THRESHOLD = 0.5f; // Порог для определения достижения цели (в градусах)
    private static Entity currentTarget = null;
    private static float currentYaw;
    private static float currentPitch;
    private static float initialYaw = 0f;
    private static float initialPitch = 0f;

    /**
     * Устанавливает цель для Baritone (перемещение к указанной позиции)
     *
     * @param entity Целевая позиция
     */
    public static void setGoal(Entity entity) {
        if (BaritoneAPI.getProvider().getBaritoneForPlayer(Minecraft.getInstance().player) == null) {
            return; // Baritone не загружена
        }
        int x = (int) Math.floor(entity.getX());
        int y = (int) Math.floor(entity.getY());
        int z = (int) Math.floor(entity.getZ());
        BlockPos pos = new BlockPos(x, y, z);
        GoalNear goal = new GoalNear(pos, 3);
        // Получаем экземпляр Baritone
        baritone.api.Settings settings = BaritoneAPI.getSettings();
        settings.allowBreak.value = false; // Не разрушать блоки
        settings.allowPlace.value = false; // Не ставить блоки

        // Устанавливаем цель для Baritone
        BaritoneAPI.getProvider().getPrimaryBaritone()
                .getCustomGoalProcess()
                .setGoalAndPath(goal);
    }

    // Проверка достижения цели
    public static boolean hasReachedTarget(Entity self, Entity target) {
        double distance = self.distanceTo(target);
        return distance < 4.0; // Дистанция, при которой считаем, что цель достигнута
    }
    /**
     * Проверяет, достиг ли игрок цели по направлению взгляда
     */
    /**
     * Проверяет, достиг ли игрок цели по направлению взгляда
     */
    public static boolean hasReachedAim() {
        if (currentTarget == null) {
            return false; // Нет цели — не можем проверить
        }

        // Вычисляем текущую разницу между целевыми и текущими углами
        float targetYaw = isAiming ? calculateTargetYaw() : initialYaw;
        float targetPitch = isAiming ? calculateTargetPitch() : initialPitch;
        targetYaw = normalizeAngle(targetYaw);
        targetPitch = normalizeAngle(targetPitch);
        float deltaYaw = Math.abs(targetYaw - currentYaw);
        float deltaPitch = Math.abs(targetPitch - currentPitch);

        // Нормализуем разницу для yaw, чтобы учитывать минимальный угол поворота
        if (deltaYaw > 180.0f) {
            deltaYaw = 360.0f - deltaYaw;
        }

        // Проверяем, находится ли разница в пределах заданного порога
        return deltaYaw <= AIM_THRESHOLD && deltaPitch <= AIM_THRESHOLD;
    }
    /**
     * Начинает плавное наведение на указанную цель
     *
     * @param target Целевая сущность
     */
    public static void startAiming(Entity target) {
        currentTarget = target;
        isAiming = true;
        if (mc.player != null) {
            initialYaw = mc.player.getYRot();
            initialPitch = mc.player.getXRot();
            currentYaw = initialYaw;
            currentPitch = initialPitch;
        }
    }

    /**
     * Останавливает наведение
     */
    public static void stopAiming() {
        isAiming = false;
    }

    /**
     * Обновляет направление взгляда игрока для плавного наведения на цель
     *
     * @return
     */
    public static void updateAiming() {
        if (mc.player == null) {
            return;
        }

        float targetYaw = isAiming ? calculateTargetYaw() : initialYaw;
        float targetPitch = isAiming ? calculateTargetPitch() : initialPitch;

        // Нормализуем углы
        currentYaw = normalizeAngle(currentYaw);
        targetYaw = normalizeAngle(targetYaw);

        currentPitch = normalizeAngle(currentPitch);
        targetPitch = normalizeAngle(targetPitch);

        // Вычисляем дельты
        float deltaYaw = targetYaw - currentYaw;
        if (deltaYaw < -180.0f) deltaYaw += 360.0f;
        if (deltaYaw > 180.0f) deltaYaw -= 360.0f;

        float deltaPitch = targetPitch - currentPitch;
        if (deltaPitch < -180.0f) deltaPitch += 360.0f;
        if (deltaPitch > 180.0f) deltaPitch -= 360.0f;

        // Ограничиваем скорость поворота
        float delta = ROTATION_SPEED * 0.05f;
        if (Math.abs(deltaYaw) > delta) {
            deltaYaw = delta * Math.signum(deltaYaw);
        }
        if (Math.abs(deltaPitch) > delta) {
            deltaPitch = delta * Math.signum(deltaPitch);
        }

        // Обновляем текущие углы
        currentYaw += deltaYaw;
        currentPitch += deltaPitch;

        // Ограничиваем pitch
        currentPitch = Math.max(-90.0f, Math.min(90.0f, currentPitch));

        // Устанавливаем углы игроку
        mc.player.setYRot(currentYaw);
        mc.player.setXRot(currentPitch);
    }

    private static float calculateTargetYaw() {
        if (currentTarget == null || mc.player == null) return 0f;
        double targetX = currentTarget.getX();
        double targetZ = currentTarget.getZ();
        double playerX = mc.player.getX();
        double playerZ = mc.player.getZ();

        double dX = targetX - playerX;
        double dZ = targetZ - playerZ;

        return (float) (Math.atan2(dZ, dX) * 180.0 / Math.PI) - 90.0f;
    }

    private static float calculateTargetPitch() {
        if (currentTarget == null || mc.player == null) return 0f;
        double targetX = currentTarget.getX();
        double targetY = currentTarget.getY() + currentTarget.getEyeHeight();
        double targetZ = currentTarget.getZ();
        double playerX = mc.player.getX();
        double playerY = mc.player.getY() + mc.player.getEyeHeight();
        double playerZ = mc.player.getZ();

        double dX = targetX - playerX;
        double dY = targetY - playerY;
        double dZ = targetZ - playerZ;

        double distance = Math.sqrt(dX * dX + dZ * dZ);

        return (float) (Math.atan2(dY, distance) * 180.0 / Math.PI);
    }

    private static float normalizeAngle(float angle) {
        angle = angle % 360.0f;
        if (angle < 0) angle += 360.0f;
        return angle;
    }
}
