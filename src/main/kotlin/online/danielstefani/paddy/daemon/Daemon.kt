package online.danielstefani.paddy.daemon

import com.fasterxml.jackson.annotation.JsonIgnore
import online.danielstefani.paddy.schedule.Schedule
import org.neo4j.ogm.annotation.NodeEntity
import org.neo4j.ogm.annotation.Relationship

@NodeEntity
open class Daemon {

    var id: String? = null

    var on: Boolean = false

    var lastPing: Long = 0

    @JsonIgnore
    @Relationship(type = "IS_SCHEDULED", direction = Relationship.Direction.OUTGOING)
    var schedules = setOf<Schedule>()
}