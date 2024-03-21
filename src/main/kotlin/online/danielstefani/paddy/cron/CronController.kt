package online.danielstefani.paddy.cron

import io.quarkus.logging.Log
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import online.danielstefani.paddy.schedule.ScheduleRepository

@ApplicationScoped
class CronController(
    private val scheduleRepository: ScheduleRepository
) {

    @Scheduled(every = "1m")
    fun loadSchedules() {
        val schedules = scheduleRepository.getAll()
        Log.info("[cron] There are ${schedules.size} schedules in the DB!")
    }
}