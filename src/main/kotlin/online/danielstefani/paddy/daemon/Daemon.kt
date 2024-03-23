package online.danielstefani.paddy.daemon

import org.neo4j.ogm.annotation.Id
import org.neo4j.ogm.annotation.NodeEntity

@NodeEntity
open class Daemon {

    @Id
    var id: String? = null

    var on: Boolean = false

}