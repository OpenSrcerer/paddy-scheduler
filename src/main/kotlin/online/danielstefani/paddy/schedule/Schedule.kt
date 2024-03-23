package online.danielstefani.paddy.schedule

import com.cronutils.model.Cron
import com.cronutils.model.time.ExecutionTime
import com.fasterxml.jackson.annotation.JsonIgnore
import io.quarkus.logging.Log
import online.danielstefani.paddy.daemon.Daemon
import org.neo4j.ogm.annotation.*
import java.time.Clock
import java.time.Instant
import java.time.ZonedDateTime
import java.util.*

@NodeEntity
open class Schedule {

    @Id
    @GeneratedValue
    var id: Long? = null

    // What the action will do: ON, OFF etc.
    var type: ScheduleType? = null

    var timezone = TimeZone.getTimeZone("UTC").toZoneId().id

    // Signifies that this Schedule will run only once.
    // Execution time in UNIX seconds
    var single: Long? = null

    // Signifies that this Schedule will run periodically.
    var periodic: String? = null

    // The last time this Schedule was executed
    var lastExecution: Long? = null

    // Signifies that this Schedule will finish running at some point.
    // If null, this Schedule runs indefinitely.
    var finish: Long? = null

    @JsonIgnore
    @Relationship(type = "IS_SCHEDULED", direction = Relationship.Direction.INCOMING)
    var daemon: Daemon? = null

    @Transient
    var cron: Cron? = null

    fun isSingle(): Boolean {
        return single != null
    }

    fun nextExecution(): Optional<Long> {
        if (isSingle()) return Optional.empty()

        return ExecutionTime.forCron(cron!!)
            .nextExecution(ZonedDateTime.now(Clock.systemUTC()))
            .map { it.toEpochSecond() }
    }

    fun shouldExecute(): Boolean {
        val isSingleAndReady = isSingle() && single!! > Instant.now().epochSecond
        val isPeriodicAndReady = nextExecution()
            .map { it > Instant.now().epochSecond }
            .orElse(false)

        return isSingleAndReady || isPeriodicAndReady
    }
}