FROM gradle:8.4.0-jdk17-jammy AS BUILD

WORKDIR /appbuild

COPY . .

RUN gradle build -Dquarkus.package.type=uber-jar --no-daemon

FROM amazoncorretto:17-alpine as CORRETTO-JDK

#COPY --from=corretto-jdk /jre /app/jre
COPY --from=build /appbuild/build/paddy-scheduler-runner.jar /app/paddy-scheduler-runner.jar

WORKDIR /app

ARG DEBUG_OPT
ENV DEBUG_API_OPT=$DEBUG_OPT

CMD java $DEBUG_API_OPT -jar paddy-scheduler-runner.jar