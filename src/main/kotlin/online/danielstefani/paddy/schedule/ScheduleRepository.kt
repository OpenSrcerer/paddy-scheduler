package online.danielstefani.paddy.schedule

import jakarta.enterprise.context.ApplicationScoped
import org.neo4j.ogm.session.SessionFactory

@ApplicationScoped
class ScheduleRepository(
    private val factory: SessionFactory
) {
    fun getAll(): Collection<Schedule> {
        return with(factory.openSession()) {
            this.loadAll(Schedule::class.java)
        }
    }
}