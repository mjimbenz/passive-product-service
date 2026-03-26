# ---------- STAGE 1: Build ----------
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace


# Copiar certificado corporativo al contenedor
COPY proxy.crt /usr/local/share/ca-certificates/proxy.crt

# Importar certificado al truststore del JDK
RUN keytool -importcert -trustcacerts -noprompt \
    -alias corporate-proxy \
    -file /usr/local/share/ca-certificates/proxy.crt \
    -keystore $JAVA_HOME/lib/security/cacerts \
    -storepass changeit \

COPY pom.xml .
COPY src ./src

RUN mvn -q -DskipTests package


# ---------- STAGE 2: Runtime ----------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copiar solo el artefacto final
COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
