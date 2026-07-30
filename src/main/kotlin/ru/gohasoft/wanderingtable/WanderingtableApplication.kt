package ru.gohasoft.wanderingtable

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity

@SpringBootApplication
// Без @EnableMethodSecurity аннотации @PreAuthorize на сервисах не действуют,
// и любая проверка прав молча пропускает запрос.
@EnableMethodSecurity
@EnableScheduling
class WanderingtableApplication

fun main(args: Array<String>) {
	runApplication<WanderingtableApplication>(*args)
}
