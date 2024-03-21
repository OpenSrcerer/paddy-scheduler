package online.danielstefani.paddy.util

import org.neo4j.ogm.model.Result

inline fun <reified T> Result.get(): List<T> {
    return with(this.queryResults().iterator()) {
        if (!this.hasNext())
            emptyList<T>()

        this.asSequence().toList().flatMap { it.values.map { it as T } }
    }
}