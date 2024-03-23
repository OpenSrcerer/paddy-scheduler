package online.danielstefani.paddy.cron

import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.parser.CronParser
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult
import io.quarkus.logging.Log
import io.reactivex.Completable
import io.reactivex.Flowable
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import online.danielstefani.paddy.mqtt.RxMqttClient
import online.danielstefani.paddy.schedule.Schedule
import online.danielstefani.paddy.schedule.ScheduleRepository
import reactor.core.publisher.Flux

@ApplicationScoped
class CronService(
    private val scheduleRepository: ScheduleRepository,
    private val mqttClient: RxMqttClient
) {
    companion object {
        private val cronParser = CronParser(
            CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ))
    }

    private val scheduledCrons = HashSet<Schedule>()

    fun reloadSchedule(id: Long, remove: Boolean = false) {
        scheduledCrons.removeIf { it.id == id }

        if (remove) return

        val schedule = scheduleRepository.get(id)!!
        if (schedule.isSingle()) {
            scheduledCrons.add(schedule)
            return
        }

        schedule.cron = cronParser.parse(schedule.periodic)
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    fun executeSchedules(): Flux<Mqtt5PublishResult> {
        val publishes = scheduledCrons
            .filter { it.shouldExecute() }
            .mapNotNull { executeSchedule(it) }

        return Flux.from(Flowable.concat(publishes))
    }

    @Transactional(Transactional.TxType.MANDATORY)
    private fun executeSchedule(schedule: Schedule): Flowable<Mqtt5PublishResult>? {
        // Run single schedules immediately, then delete them
        if (!schedule.shouldExecute()) {
            Log.debug("[cron->service] Schedule ${schedule.id} is not ready to run yet. Skipping.")
            return null
        }

        // Run the schedule, then update it in the database
        return runSchedule(schedule).concatWith(reSchedule(schedule))
            .doOnSubscribe {  Log.debug("[cron->service] Schedule ${schedule.id} is ready! Executing...") }
            .doOnComplete { Log.debug("[cron->service] Executed <${schedule.id!!}> successfully!") }
            .doOnError { Log.error("[cron->service] Failed to execute <${schedule.id!!}>", it) }
    }

    private fun runSchedule(schedule: Schedule): Flowable<Mqtt5PublishResult> {
        return mqttClient.publish(
            schedule.daemon!!.id!!,
            schedule.type!!.name.lowercase(),
            qos = MqttQos.EXACTLY_ONCE)!!
    }

    /*
     If single:
     Delete the schedule.

     If periodic:
     Update the last execution time

     For both:
     Reload
     */
    private fun reSchedule(schedule: Schedule): Completable {
        val dbActionCompletable = if (schedule.isSingle())
            Completable.fromCallable { scheduleRepository.delete(schedule.id!!) }
        else
            Completable.fromCallable {  }

        val reloadCompletable = Completable.fromCallable {
            reloadSchedule(schedule.id!!, schedule.isSingle())
        }

        return dbActionCompletable.concatWith(reloadCompletable)
    }
}