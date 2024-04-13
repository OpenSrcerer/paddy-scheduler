FROM ghcr.io/graalvm/graalvm-ce:ol8-java17-22.3.3 AS BUILD

WORKDIR /appbuild

COPY . .

RUN gu install native-image

RUN ./gradlew build --no-daemon -Dquarkus.package.type=native


FROM quay.io/quarkus/quarkus-micro-image:2.0 AS NATIVE

WORKDIR /work

COPY --from=BUILD /appbuild/* .

RUN chown 1001 /work \
    && chmod "g+rwX" /work \
    && chown 1001:root /work

EXPOSE 8080
USER 1001

CMD ["/work/quarkus-build/gen/paddy-scheduler-runner", "-Dquarkus.http.host=0.0.0.0"]
