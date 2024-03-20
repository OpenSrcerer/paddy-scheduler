package online.danielstefani.paddy.jwt.dto

enum class JwtType(
    val lifetime: Long,
    val audience: String
) {
    ADMIN(31540000, "paddy~internal"),
    DAEMON(31540000, "paddy~daemon"),
    USER(3600, "paddy~user")
}