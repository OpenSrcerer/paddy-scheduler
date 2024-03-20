package online.danielstefani.paddy.util

import io.smallrye.mutiny.Uni
import reactor.core.publisher.Mono

fun <T> Uni<T>.toMono(): Mono<T> {
    return Mono.create { sink ->
        this.subscribe().with(
            { item -> sink.success(item) },
            { error -> sink.error(error) })
    }
}