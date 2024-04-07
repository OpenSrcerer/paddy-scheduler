package online.danielstefani.paddy.cron

import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import io.quarkus.logging.Log
import io.quarkus.runtime.StartupEvent
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import online.danielstefani.paddy.schedule.ScheduleRepository
import reactor.core.publisher.Mono
import java.util.concurrent.TimeUnit

@ApplicationScoped
class CronController(
    private val scheduleRepository: ScheduleRepository,
    private val cronService: CronService
) {
    private var schedulesExecuted = 0

    @Scheduled(delay = 1, delayUnit = TimeUnit.MINUTES, every = "1m")
    fun scheduleStatistics() {
        Log.info("[cron] [pool: ${cronService.scheduledCrons.size}] Executed <$schedulesExecuted> schedule(s) in the last minute.")

        schedulesExecuted = 0
    }

    // Execute schedules if needed
    @Scheduled(delay = 5, delayUnit = TimeUnit.SECONDS, every = "1s")
    fun executeSchedules() {
        cronService.executeSchedules()
            .doOnSuccess { schedulesExecuted += it }
            .doOnError { Log.error("[cron] Failed executing schedule(s)", it) }
            .subscribe()
    }

    // Load schedules at startup
    fun loadSchedules(@Observes event: StartupEvent) {
        val schedules = scheduleRepository.getAll()
            .onEach { cronService.reloadSchedule(it) }
            .size

        Log.info("[cron] Loaded <$schedules> schedule(s) to execute.")
    }

    // Receive CRUD events from backend
    fun onScheduleUpdate(mqtt5Publish: Mqtt5Publish) {
        val scheduleId = mqtt5Publish.topic.levels.last().toLong()
        Log.debug("[cron] Received an update for schedule <$scheduleId>.")

        Mono.fromCallable { scheduleRepository.get(scheduleId) }
            .map { cronService.reloadSchedule(it) }
            .subscribe()
    }
}