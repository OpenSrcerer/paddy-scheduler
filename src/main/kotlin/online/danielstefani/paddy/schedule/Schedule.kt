package online.danielstefani.paddy.schedule

import com.cronutils.model.Cron
import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser
import com.fasterxml.jackson.annotation.JsonIgnore
import io.quarkus.logging.Log
import online.danielstefani.paddy.daemon.Daemon
import org.neo4j.ogm.annotation.*
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import kotlin.math.abs

@NodeEntity
open class Schedule {

    companion object {
        // Used to prefire events before their actual time to account
        // for latency of message propagation, etc.
        private const val LATENCY_SECONDS = 1

        private val cronParser = CronParser(
            CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX))
    }

    @Id
    @GeneratedValue
    var id: Long? = null

    // What the action will do: ON, OFF etc.
    var type: ScheduleType? = null

    // Defaults to UTC
    var timezone = TimeZone.getTimeZone("UTC").toZoneId().id

    // Signifies that this Schedule will run periodically.
    // Does not exist on single tasks.
    var periodic: String? = null

    // The next time this Schedule will be executed
    var nextExecution: Long? = null

    // Signifies that this Schedule will finish running at some point.
    // If null, this Schedule runs indefinitely.
    var finish: Long? = null

    @JsonIgnore
    @Relationship(type = "IS_SCHEDULED", direction = Relationship.Direction.INCOMING)
    var daemon: Daemon? = null

    @Transient
    var cron: Cron? = null

    /*
    Default the next execution as the last execution so that
    if this has never been run, it will be run on the last.

    Remember to update the last execution when finishing.
     */
    fun load() {
        cron = if (!isSingle()) cronParser.parse(periodic)
            else null
        nextExecution = nextExecution ?: nextExecution()
    }

    fun hasExpired(): Boolean {
        return isSingle() || finish != null && finish!! < Instant.now().epochSecond
    }

    fun isSingle(): Boolean {
        return periodic == null
    }

    fun nextExecution(
        zonedDateTime: ZonedDateTime = ZonedDateTime.now(ZoneId.of(timezone))
    ): Long {
        return execution { it.nextExecution(zonedDateTime) }
    }

    fun lastExecution(
        zonedDateTime: ZonedDateTime = ZonedDateTime.now(ZoneId.of(timezone))
    ): Long {
        return execution { it.lastExecution(zonedDateTime) }
    }

    fun shouldExecute(): Boolean {
        val timeNow = Instant.now().epochSecond
        val timeNowAdjusted = timeNow + LATENCY_SECONDS

        val zonedTimeNow = ZonedDateTime.ofInstant(Instant.ofEpochSecond(timeNow), ZoneId.of(timezone))
        val nextExec = nextExecution(zonedTimeNow)

        val isReady = nextExec < timeNowAdjusted

        Log.trace("""[cron->service] Schedule <${id!!}> status:
            [Single]:     ${isSingle()}
            [Ready]:      $isReady
            [Cron]:       ${periodic ?: "N/A"}
            [Timezone]:   $timezone
            [Last Exec.]: ${lastExecution(zonedTimeNow)} (UNIX s)
            [Time Now]:   $timeNow (UNIX s)
            [Next Exec.]: $nextExec (UNIX s)
            [Until Next]: ${abs(nextExec - timeNow)} (UNIX s)
            [Finish]:     ${finish ?: "N/A"} (UNIX s)
            [Until Fin.]: ${if (finish != null) abs(finish!! - timeNow) else "N/A"} (UNIX s)
        """.trimIndent())

        return isReady
    }

    private fun execution(selector: (ExecutionTime) -> Optional<ZonedDateTime>): Long {
        if (isSingle())
            return nextExecution!!

        return selector.invoke(ExecutionTime.forCron(cron!!))
            .map { it.toEpochSecond() }
            .orElseThrow {
                IllegalArgumentException("Cron execution time could not be generated for cron: <$periodic>!") }
    }
}