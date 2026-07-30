package ru.gohasoft.wanderingtable.support

import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Запускает [threads] задач максимально одновременно и возвращает их результаты.
 *
 * Барьер на [CountDownLatch] нужен, чтобы потоки стартовали вместе: без него первый поток
 * успевает завершить транзакцию до старта остальных, и гонка не воспроизводится.
 */
fun <T> runConcurrently(threads: Int, task: (Int) -> T): List<T> {
    val pool = Executors.newFixedThreadPool(threads)
    try {
        val startGate = CountDownLatch(1)
        val ready = CountDownLatch(threads)
        val futures = (0 until threads).map { index ->
            pool.submit(Callable {
                ready.countDown()
                startGate.await()
                task(index)
            })
        }
        check(ready.await(30, TimeUnit.SECONDS)) { "threads failed to start" }
        startGate.countDown()
        return futures.map { it.get(60, TimeUnit.SECONDS) }
    } finally {
        pool.shutdownNow()
    }
}
