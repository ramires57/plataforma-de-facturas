FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY src/main/java ./src/main/java
RUN mkdir -p out && javac --add-modules jdk.httpserver -encoding UTF-8 -d out \
    src/main/java/pe/senati/facturacion/Factura.java \
    src/main/java/pe/senati/facturacion/CalculadoraFactura.java \
    src/main/java/pe/senati/facturacion/PdfFactura.java \
    src/main/java/pe/senati/facturacion/ServidorFactura.java

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/out ./out
COPY web ./web
EXPOSE 8000
ENTRYPOINT ["java", "--add-modules", "jdk.httpserver", "-cp", "out", "pe.senati.facturacion.ServidorFactura"]
