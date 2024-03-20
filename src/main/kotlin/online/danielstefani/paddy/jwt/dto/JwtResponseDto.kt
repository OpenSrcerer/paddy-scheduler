package online.danielstefani.paddy.jwt.dto

data class JwtResponseDto(
    val jwt: String,
    val absoluteExpiryUnixSeconds: Long
)