FROM ubuntu:latest

# Install Java (OpenJDK)
RUN apt-get update && \
    apt-get install -y openjdk-11-jdk && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY ./PrimeCalculator-1.0-jar-with-dependencies.jar PrimeCalculator-1.0-jar-with-dependencies.jar

# Set the entry point for the container
ENTRYPOINT ["java", "-jar", "PrimeCalculator-1.0-jar-with-dependencies.jar"]
