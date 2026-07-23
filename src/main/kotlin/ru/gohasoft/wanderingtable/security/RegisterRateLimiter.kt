package ru.gohasoft.wanderingtable.security

import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Component
class RegisterRateLimiter {

    private class Window(var count: Int, var start: Instant)

    private val windows = ConcurrentHashMap<String, Window>()

    fun tryAcquire(key: String): Boolean {
        val window = windows.compute(key) { _, existing ->
            val now = Instant.now()
            if (existing == null || Duration.between(existing.start, now) > WINDOW) {
                Window(count = 1, start = now)
            } else {
                existing.apply { count++ }
            }
        }
        return (window?.count ?: 0) <= MAX_REQUESTS
    }

    companion object {
        private val WINDOW: Duration = Duration.ofMinutes(1)
        private const val MAX_REQUESTS = 5
    }
}
