package ru.gohasoft.wanderingtable.database.model

import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Текущее время с точностью, которую действительно хранит PostgreSQL (микросекунды).
 *
 * `Instant.now()` на JDK даёт наносекунды, а столбец `timestamp` их отбрасывает. Из-за этого
 * значение в ответе на создание сущности не совпадало со значением при последующем чтении —
 * клиент видел два разных `createdAt` для одной и той же записи.
 */
fun nowTruncated(): Instant = Instant.now().truncatedTo(ChronoUnit.MICROS)
