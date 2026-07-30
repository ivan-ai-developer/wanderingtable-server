package ru.gohasoft.wanderingtable.controllers

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import ru.gohasoft.wanderingtable.controllers.dto.LoginRequest
import ru.gohasoft.wanderingtable.controllers.dto.RefreshRequest
import ru.gohasoft.wanderingtable.controllers.dto.RegisterRequest
import ru.gohasoft.wanderingtable.controllers.dto.TokenPairResponse
import ru.gohasoft.wanderingtable.controllers.dto.UserResponse
import ru.gohasoft.wanderingtable.controllers.dto.toResponse
import ru.gohasoft.wanderingtable.security.AuthService
import ru.gohasoft.wanderingtable.security.RegisterRateLimiter
import ru.gohasoft.wanderingtable.security.TokenPair

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
    private val registerRateLimiter: RegisterRateLimiter
) {

    @PostMapping("/register")
    fun register(
        @Valid @RequestBody body: RegisterRequest,
        request: HttpServletRequest
    ): UserResponse {
        if (!registerRateLimiter.tryAcquire(request.clientIp())) {
            throw ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many registration attempts. Try again later."
            )
        }
        return authService.register(body.name, body.email, body.password).toResponse()
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody body: LoginRequest): TokenPairResponse =
        authService.login(body.email, body.password).toResponse()

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody body: RefreshRequest): TokenPairResponse =
        authService.refresh(body.refreshToken).toResponse()

    /** Отзывает переданный refresh-токен. Идемпотентен — всегда 204. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(@Valid @RequestBody body: RefreshRequest) {
        authService.logout(body.refreshToken)
    }
}

private fun TokenPair.toResponse() = TokenPairResponse(
    accessToken = accessToken,
    refreshToken = refreshToken
)

private fun HttpServletRequest.clientIp(): String {
    return getHeader("X-Forwarded-For")
        ?.substringBefore(",")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: remoteAddr
}
