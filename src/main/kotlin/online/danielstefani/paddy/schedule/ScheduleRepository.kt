package online.danielstefani.paddy.schedule

import jakarta.enterprise.context.ApplicationScoped
import online.danielstefani.paddy.util.get
import org.neo4j.ogm.session.SessionFactory

@ApplicationScoped
class ScheduleRepository(
    private val factory: SessionFactory
) {
    companion object {
        // Represents how deep should neo4j
        // load the entity's relationships
        const val SCHEDULE_LOAD_DEPTH = 2
    }

    fun get(id: Long): Schedule? {
        return with(factory.openSession()) {
            this.load(Schedule::class.java, id, SCHEDULE_LOAD_DEPTH)
        }
    }

    fun getAll(): Collection<Schedule> {
        return with(factory.openSession()) {
            this.query(
                "MATCH (sx:Schedule) RETURN sx",
                emptyMap<String, String>()
            ).get()
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

                it.daemon = null

                this.save(it)
            }
        }
    }

    fun delete(id: Long): Schedule? {
        return with(factory.openSession()) {
            get(id)?.also {
                this.delete(it)
            }
        }
    }
}