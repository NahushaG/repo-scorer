# Start with a lightweight JDK image
FROM eclipse-temurin:17-jdk-alpine

# Set working directory inside container
WORKDIR /app

# Copy Maven build artifacts
COPY target/reposcorer-0.0.1-SNAPSHOT.jar app.jar

# Expose default port
EXPOSE 8080

# Environment variable for GitHub token (can also use Docker secrets)
ENV GITHUB_TOKEN=""

# Run the jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
