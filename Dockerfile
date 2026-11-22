FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY src/main/java ./src/main/java
RUN mkdir -p out && find src/main/java -name "*.java" -print > sources.txt && \
    javac --add-modules jdk.httpserver -encoding UTF-8 -d out @sources.txt

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/out ./out
COPY web ./web
EXPOSE 8000
ENTRYPOINT ["java", "--add-modules", "jdk.httpserver", "-cp", "out", "pe.senati.facturacion.ServidorFactura"]
