package online.danielstefani.paddy.schedule

enum class ScheduleType {
    ON, OFF, TOGGLE;

    fun toBoolean(): Boolean? {
        return when(this) {
            ON -> true
            OFF -> false
            TOGGLE -> null
        }
    }
}