FROM eclipse-temurin:17 AS build
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
COPY start.sh .
RUN chmod +x start.sh

CMD ["./start.sh"]