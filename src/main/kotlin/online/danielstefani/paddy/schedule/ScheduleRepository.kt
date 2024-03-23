package online.danielstefani.paddy.schedule

import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import org.neo4j.ogm.session.SessionFactory
import org.neo4j.ogm.session.queryForObject

@ApplicationScoped
class ScheduleRepository(
    private val factory: SessionFactory
) {
    fun get(id: Long): Schedule? {
        return with(factory.openSession()) {
            val query =
                """
                    MATCH (sx:Schedule)
                        WHERE ID(sx) = $id
                    RETURN sx
                """

            this.queryForObject(query, emptyMap())
        }
    }

    fun getAll(): Collection<Schedule> {
        return with(factory.openSession()) {
            this.loadAll(Schedule::class.java)
        }
    }

    fun update(
        id: Long,
        daemonId: String? = null,
        updater: (Schedule) -> Unit
    ): Schedule? {
        return with(factory.openSession()) {
            get(id)?.also {
                updater.invoke(it)

                this.save(it)
            }
        }

    }

    fun delete(id: Long): Schedule? {
        return with(factory.openSession()) {
            get(id)?.also {
                Log.info(it)
                this.delete(it)
            }
        }
    }
}