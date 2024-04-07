package online.danielstefani.paddy.daemon

import jakarta.enterprise.context.ApplicationScoped
import org.neo4j.ogm.session.SessionFactory
import org.neo4j.ogm.session.queryForObject

@ApplicationScoped
class DaemonRepository(
    private val factory: SessionFactory
) {

    fun get(id: String, username: String? = null): Daemon? {
        val query = """
                    MATCH (node:Daemon { id: "$id" })
                    RETURN node
                """

        with(factory.openSession()) {
            return this.queryForObject<Daemon>(query, emptyMap())
        }
    }

    fun updateState(
        id: String,
        on: Boolean? = null
    ): Daemon? {
        return with(factory.openSession()) {
            get(id)?.also {
                val query = """
                    MATCH (node:Daemon { id: "$id" })
                    SET node.on = ${on ?: !it.on}
                    RETURN node
                """

                this.query(query, mapOf<String, String>())
            }
        }
    }
}