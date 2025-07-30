# ---------- build stage ----------
    FROM eclipse-temurin:17-jdk AS build
    WORKDIR /app
    COPY . .
    RUN ./mvnw -q -Dmaven.test.skip=true clean package
    
    # ---------- runtime stage ----------
    FROM eclipse-temurin:17-jre
    WORKDIR /app
    COPY --from=build /app/target/*.jar app.jar
    
    # Railway provides PORT at runtime, not build time
    # Use exec form to ensure proper signal handling
    ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -Dserver.address=0.0.0.0 -jar app.jar"]