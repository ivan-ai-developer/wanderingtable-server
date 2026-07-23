package ru.gohasoft.wanderingtable.controllers

import ru.gohasoft.wanderingtable.security.AuthService
import ru.gohasoft.wanderingtable.security.RegisterRateLimiter
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
    private val registerRateLimiter: RegisterRateLimiter
) {

    data class AuthRequest(
        @field:Email(message = "Invalid email format.")
        val email: String,
        @field:Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}\$",
            message = "Password must be at least 8 characters long and contain at least one digit, uppercase and lowercase character."
        )
        val password: String
    )

    data class RefreshRequest(
        val refreshToken: String
    )

    @PostMapping("/register")
    fun register(
        @Valid @RequestBody body: AuthRequest,
        request: HttpServletRequest
    ) {
        if (!registerRateLimiter.tryAcquire(request.clientIp())) {
            throw ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many registration attempts. Try again later."
            )
        }
        authService.register(body.email, body.password)
    }

    @PostMapping("/login")
    fun login(
        @RequestBody body: AuthRequest
    ): AuthService.TokenPair {
        return authService.login(body.email, body.password)
    }

    @PostMapping("/refresh")
    fun refresh(
        @RequestBody body: RefreshRequest
    ): AuthService.TokenPair {
        return authService.refresh(body.refreshToken)
    }
}

private fun HttpServletRequest.clientIp(): String {
    return getHeader("X-Forwarded-For")
        ?.substringBefore(",")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: remoteAddr
}