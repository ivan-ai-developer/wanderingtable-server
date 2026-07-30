package ru.gohasoft.wanderingtable.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Лимитер попыток регистрации, по одному скользящему окну на IP.
 *
 * Лимитер **локален для инстанса**: при горизонтальном масштабировании фактический лимит
 * умножается на число реплик. Для распределённого лимита понадобился бы общий счётчик
 * (например, в Redis).
 */
@Component
class RegisterRateLimiter(
    @Value($$"${app.rate-limit.register.max-requests:5}") private val maxRequests: Int,
    @Value($$"${app.rate-limit.register.window-seconds:60}") windowSeconds: Long
) {

    private class Window(var count: Int, var start: Instant)

    private val window: Duration = Duration.ofSeconds(windowSeconds)
    private val windows = ConcurrentHashMap<String, Window>()

    fun tryAcquire(key: String): Boolean {
        // Решение принимается внутри compute: раньше счётчик читался уже после выхода из
        // атомарного блока, и конкурентный поток успевал его увеличить — из-за чего лимитер
        // под нагрузкой отклонял запросы, которые должен был пропустить.
        var allowed = false
        windows.compute(key) { _, existing ->
            val now = Instant.now()
            val updated = if (existing == null || Duration.between(existing.start, now) > window) {
                Window(count = 1, start = now)
            } else {
                existing.apply { count++ }
            }
            allowed = updated.count <= maxRequests
            updated
        }
        return allowed
    }

    /**
     * Удаляет протухшие окна: без этого map рос неограниченно по числу уникальных IP,
     * то есть был вектором исчерпания памяти.
     */
    @Scheduled(fixedDelayString = "\${app.rate-limit.register.cleanup-interval-ms:300000}")
    fun evictExpiredWindows() {
        val now = Instant.now()
        windows.entries.removeIf { Duration.between(it.value.start, now) > window }
    }
}
