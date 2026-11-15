# Construcción en dos etapas: la primera compila con JDK y Maven; la segunda solo lleva el JRE y el jar,
# así la imagen final es pequeña y no incluye herramientas de compilación.

# ---- Etapa 1: compilar
FROM eclipse-temurin:25-jdk AS compilacion
WORKDIR /app

# Primero solo lo que define las dependencias: mientras no cambie el pom, Docker reutiliza esta capa
# y no vuelve a descargar todo en cada construcción.
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q package -DskipTests

# ---- Etapa 2: ejecutar
FROM eclipse-temurin:25-jre
WORKDIR /app

# No se ejecuta como administrador dentro del contenedor.
RUN useradd --system --no-create-home polimarket
USER polimarket

COPY --from=compilacion /app/target/backend-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
