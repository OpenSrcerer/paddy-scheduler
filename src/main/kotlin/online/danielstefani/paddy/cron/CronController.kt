package online.danielstefani.paddy.cron

import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import io.quarkus.logging.Log
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import online.danielstefani.paddy.schedule.ScheduleRepository

@ApplicationScoped
class CronController(
    private val scheduleRepository: ScheduleRepository
) {
    fun onScheduleUpdate(mqtt5Publish: Mqtt5Publish) {
        val scheduleId = mqtt5Publish.topic.levels.last()
        Log.info("[cron] Received an update for schedule <$scheduleId>")
    }

    @Scheduled(every = "1m")
    fun loadSchedules() {
        val schedules = scheduleRepository.getAll()
        Log.info("[cron] There are ${schedules.size} schedules in the DB!")
    }
}