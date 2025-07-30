FROM eclipse-temurin:17 AS build
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# This is the key - using shell form to expand PORT variable
CMD java -jar app.jar --server.port=${PORT:-8080}