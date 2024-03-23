package online.danielstefani.paddy.cron

import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import io.quarkus.logging.Log
import io.quarkus.runtime.StartupEvent
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import online.danielstefani.paddy.schedule.ScheduleRepository

@ApplicationScoped
class CronController(
    private val scheduleRepository: ScheduleRepository,
    private val cronService: CronService
) {
    // Execute schedules if needed
    @Scheduled(every = "5s")
    fun executeSchedules() {
        cronService.executeSchedules()
            .doOnSubscribe { Log.info("[cron] Executing schedules...") }
            .doOnComplete { Log.info("[cron] Finished executing schedules") }
            .doOnError { Log.error("[cron] Failed executing schedules", it) }
            .subscribe()
    }

    // Load schedules at startup
    fun loadSchedules(@Observes event: StartupEvent) {
        val schedules = scheduleRepository.getAll()
            .onEach { cronService.reloadSchedule(it.id!!) }
            .size

        Log.info("[cron] Loaded <$schedules>")
    }

    // Receive CRUD events from backend
    fun onScheduleUpdate(mqtt5Publish: Mqtt5Publish) {
        val scheduleId = mqtt5Publish.topic.levels.last().toLong()
        Log.debug("[cron] Received an update for schedule <$scheduleId>")

        cronService.reloadSchedule(scheduleId)
    }
}