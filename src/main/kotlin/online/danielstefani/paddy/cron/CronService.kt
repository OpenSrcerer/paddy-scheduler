package online.danielstefani.paddy.cron

import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult
import io.quarkus.logging.Log
import io.reactivex.Completable
import io.reactivex.Flowable
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import online.danielstefani.paddy.daemon.DaemonRepository
import online.danielstefani.paddy.mqtt.RxMqttClient
import online.danielstefani.paddy.schedule.Schedule
import online.danielstefani.paddy.schedule.ScheduleRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@ApplicationScoped
class CronService(
    private val scheduleRepository: ScheduleRepository,
    private val daemonRepository: DaemonRepository,
    private val mqttClient: RxMqttClient
) {
    internal val scheduledCrons = HashSet<Schedule>()

    fun reloadSchedule(id: Long) {
        scheduledCrons.removeIf { it.id == id }
        val schedule = scheduleRepository.get(id).also { it?.load() } ?: return
        scheduledCrons.add(schedule)
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    fun executeSchedules(): Mono<Int> {
        Log.debug("[cron->service] There are [${scheduledCrons.size}] schedule(s) in the pool.")

        val executed = scheduledCrons
            .filter { it.shouldExecute() }
            .map { executeSchedule(it) }

        return Flux.from(Flowable.concat(executed))
            .collectList()
            .map { executed.size }
    }

    @Transactional(Transactional.TxType.MANDATORY)
    private fun executeSchedule(schedule: Schedule): Flowable<Mqtt5PublishResult> {
        // Run the schedule, then update it in the database
        return runSchedule(schedule)
            .concatWith(reSchedule(schedule))
            .doOnSubscribe { Log.debug("[cron->service] Schedule ${schedule.id} is ready! Executing...") }
            .doOnComplete { Log.debug("[cron->service] Executed <${schedule.id!!}> successfully!") }
            .doOnError { Log.error("[cron->service] Failed to execute <${schedule.id!!}>", it) }
    }

    /*
    1) Update the status of the Daemon in the DB.
    2) Send a notification through MQTT to inform the Daemon
     */
    private fun runSchedule(schedule: Schedule): Flowable<Mqtt5PublishResult> {
        val dbActionCompletable = Completable.fromCallable {
            daemonRepository.updateState(schedule.daemon!!.id!!, schedule.type!!.toBoolean())
        }
        val publishCompletable = mqttClient.publish(
            schedule.daemon!!.id!!,
            schedule.type!!.name.lowercase(),
            qos = MqttQos.EXACTLY_ONCE)!!

        return publishCompletable.concatWith(dbActionCompletable)
    }

    /*
     - If single:
     Delete the schedule.

     - If periodic:
     Update the last execution time

     - For both:
     Reload
     */
    private fun reSchedule(schedule: Schedule): Completable {
        val dbActionCompletable = if (schedule.hasExpired())
            Completable.fromCallable {
                Log.debug("[cron->service] Schedule <${schedule.id}> has expired, removing.")
                scheduleRepository.delete(schedule.id!!)
            }
        else
            Completable.fromCallable {
                scheduleRepository.update(schedule.id!!) {
                    it.nextExecution = schedule.nextExecution()
                }
            }

        val reloadCompletable = Completable.fromCallable { reloadSchedule(schedule.id!!) }

        return dbActionCompletable.concatWith(reloadCompletable)
    }
}