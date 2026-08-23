FROM amazoncorretto:25.0.4 AS build

RUN dnf install -y findutils \
    && dnf clean all

WORKDIR /workspace

COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle gradle.properties settings.gradle ./
COPY src ./src

RUN chmod +x gradlew \
    && ./gradlew bootJar --no-daemon \
    && find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -exec cp {} home-loan-calculator.jar \;

FROM amazoncorretto:25.0.4-al2023-headless

WORKDIR /opt/home-loan-calculator

COPY --from=build --chown=10001:10001 /workspace/home-loan-calculator.jar ./home-loan-calculator.jar
COPY --chown=10001:10001 entrypoint.sh ./entrypoint.sh

RUN chmod +x entrypoint.sh

USER 10001:10001

ENTRYPOINT ["/opt/home-loan-calculator/entrypoint.sh"]

# Enable native access for Java 23+.
CMD ["java", "--enable-native-access=ALL-UNNAMED", "--sun-misc-unsafe-memory-access=allow", "-jar", "home-loan-calculator.jar"]

EXPOSE 8080
