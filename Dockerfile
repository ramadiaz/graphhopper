# Build stage
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml files first for better layer caching
COPY pom.xml .
COPY core/pom.xml core/
COPY reader-gtfs/pom.xml reader-gtfs/
COPY tools/pom.xml tools/
COPY map-matching/pom.xml map-matching/
COPY web-bundle/pom.xml web-bundle/
COPY web-api/pom.xml web-api/
COPY web/pom.xml web/
COPY client-hc/pom.xml client-hc/
COPY navigation/pom.xml navigation/
COPY example/pom.xml example/

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
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

# Create directories for logs and graph cache
RUN mkdir -p logs graph-cache

# Expose the default port
EXPOSE 8989

# Set default JVM options (can be overridden)
ENV JAVA_OPTS="-Xmx2g -Xms2g"

# Default OSM file (can be overridden via environment variable or volume mount)
ENV OSM_FILE="berlin-latest.osm.pbf"

# Run the application
CMD java $JAVA_OPTS -D"dw.graphhopper.datareader.file=${OSM_FILE}" -jar graphhopper.jar server config.yml

