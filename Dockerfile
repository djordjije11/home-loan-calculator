FROM 379129775379.dkr.ecr.eu-central-1.amazonaws.com/platform/autoscout24/deploy/java:corretto-25
COPY --chown=10001:10001 build/libs/*-boot.jar /opt/home-loan-calculator/home-loan-calculator.jar
COPY --chown=10001:10001 entrypoint.sh /opt/home-loan-calculator/entrypoint.sh
WORKDIR /opt/home-loan-calculator
RUN chmod +x entrypoint.sh
USER 10001:10001
ENTRYPOINT ["/opt/home-loan-calculator/entrypoint.sh"]

# --------------------------------------------------------------------
# Enable native access for Java 23+
# Reference: JEP 471 (https://openjdk.org/jeps/471). Those flages:
# --enable-native-access=ALL-UNNAMED
# --sun-misc-unsafe-memory-access=allow
# allow libraries like zstd-jni and Kafka (protobuf) to continue loading native code and using restricted APIs safely.
# Without this, Java 23+ emits warnings and will block native access in future releases.
# --------------------------------------------------------------------
CMD ["java", "--enable-native-access=ALL-UNNAMED", "--sun-misc-unsafe-memory-access=allow", "-jar", "home-loan-calculator.jar"]
EXPOSE 8080
