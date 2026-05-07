FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/raf-example-gateway-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
