# Build stage
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy all project files
COPY . .

# Build the application (skip tests for faster build)
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/web/target/graphhopper-web-*.jar ./graphhopper.jar

# Copy config file
COPY config.yml ./config.yml

# Copy entrypoint script
COPY docker-entrypoint.sh ./docker-entrypoint.sh
RUN chmod +x ./docker-entrypoint.sh

# Create directories for logs and graph cache
RUN mkdir -p logs graph-cache

# Expose the default port
EXPOSE 8989

# Set default JVM options (can be overridden)
ENV JAVA_OPTS="-Xmx2g -Xms2g"

# Default OSM file (can be overridden via environment variable or volume mount)
ENV OSM_FILE="/app/indonesia-latest.osm.pbf"

# Run the application
ENTRYPOINT ["./docker-entrypoint.sh"]

