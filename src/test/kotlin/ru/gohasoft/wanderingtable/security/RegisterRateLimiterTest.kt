package ru.gohasoft.wanderingtable.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.gohasoft.wanderingtable.support.runConcurrently

class RegisterRateLimiterTest {

    private fun limiter(maxRequests: Int = 3, windowSeconds: Long = 60) =
        RegisterRateLimiter(maxRequests = maxRequests, windowSeconds = windowSeconds)

    @Test
    fun `allows requests up to the limit and denies the rest`() {
        val limiter = limiter(maxRequests = 3)

        assertThat((1..3).map { limiter.tryAcquire("1.2.3.4") }).containsOnly(true)
        assertThat(limiter.tryAcquire("1.2.3.4")).isFalse()
    }

    @Test
    fun `counts each ip separately`() {
        val limiter = limiter(maxRequests = 1)

        assertThat(limiter.tryAcquire("1.1.1.1")).isTrue()
        assertThat(limiter.tryAcquire("2.2.2.2")).isTrue()
        assertThat(limiter.tryAcquire("1.1.1.1")).isFalse()
    }

    @Test
    fun `starts a fresh window once the old one expires`() {
        val limiter = limiter(maxRequests = 1, windowSeconds = 0)

        assertThat(limiter.tryAcquire("1.2.3.4")).isTrue()
        Thread.sleep(50)
        assertThat(limiter.tryAcquire("1.2.3.4"))
            .describedAs("window of 0 seconds must have elapsed")
            .isTrue()
    }

    @Test
    fun `eviction drops expired windows`() {
        val limiter = limiter(maxRequests = 1, windowSeconds = 0)
        limiter.tryAcquire("1.2.3.4")
        Thread.sleep(50)

        limiter.evictExpiredWindows()

        assertThat(limiter.tryAcquire("1.2.3.4")).isTrue()
    }

    /** Счётчик не должен «протекать» под конкуренцией: ровно maxRequests разрешений. */
    @Test
    fun `grants exactly the limit under concurrent access`() {
        val limiter = limiter(maxRequests = 5)

        val results = runConcurrently(20) { limiter.tryAcquire("9.9.9.9") }

        assertThat(results.count { it }).isEqualTo(5)
    }
}
