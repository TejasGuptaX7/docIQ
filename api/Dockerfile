# ---------- build stage ----------
    FROM eclipse-temurin:17-jdk AS build
    WORKDIR /app
    COPY . .
    RUN ./mvnw -q -Dmaven.test.skip=true clean package
    
    # ---------- runtime stage ----------
    FROM eclipse-temurin:17-jre
    WORKDIR /app
    COPY --from=build /app/target/*.jar app.jar
    
    # Railway injects PORT (e.g. 44321).  Tell Spring to bind to it on 0.0.0.0
    ENV JAVA_OPTS="-Dserver.port=${PORT} -Dserver.address=0.0.0.0"
    
    ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
    