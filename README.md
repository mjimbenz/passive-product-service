# Passive Product Service

## Descripción

Este es un microservicio desarrollado con Spring Boot para la gestión de productos pasivos bancarios. Permite crear, consultar, actualizar y eliminar productos pasivos como cuentas de ahorro, cuentas corrientes y depósitos a plazo fijo. El servicio está diseñado de manera reactiva utilizando WebFlux y se integra con MongoDB para el almacenamiento de datos.

## Características

- **Operaciones CRUD**: Crear, leer, actualizar y eliminar productos pasivos.
- **Gestión de Saldos**: Consultar y actualizar saldos de productos.
- **Filtrado por Cliente**: Obtener productos activos de un cliente específico.
- **Eliminación Lógica**: Soporte para eliminación suave (soft delete).
- **API RESTful**: Documentada con OpenAPI/Swagger.
- **Arquitectura Reactiva**: Utiliza Spring WebFlux para operaciones no bloqueantes.
- **Descubrimiento de Servicios**: Integrado con Eureka para registro de servicios.
- **Configuración Centralizada**: Utiliza Spring Cloud Config para gestión de configuración.
- **Resiliencia**: Implementa circuit breakers con Resilience4j.
- **Logging Estructurado**: Usa Logstash para logs estructurados.
- **Trazabilidad**: Micrometer para métricas y trazas.

## Tecnologías Utilizadas

- **Java 17**
- **Spring Boot 4.0.4**
- **Spring WebFlux** (para programación reactiva)
- **Spring Data MongoDB Reactive**
- **Spring Cloud (Config, Eureka)**
- **MongoDB** (base de datos NoSQL)
- **OpenAPI 3.0** (documentación de API)
- **Lombok** (reducción de boilerplate)
- **Resilience4j** (resiliencia y circuit breakers)
- **RxJava 3** (programación reactiva)
- **Micrometer** (métricas y trazabilidad)
- **Logstash** (logging estructurado)
- **Maven** (gestión de dependencias y build)
- **Docker** (contenedorización)

## Prerrequisitos

Antes de ejecutar el servicio, asegúrate de tener instalados:

- **Java 17** o superior
- **Maven 3.6+**
- **MongoDB** (local o en contenedor)
- **Eureka Server** (para registro de servicios)
- **Config Server** (para configuración centralizada)

## Instalación

1. Clona el repositorio:
   ```bash
   git clone <url-del-repositorio>
   cd passive-product-service
   ```

2. Compila el proyecto:
   ```bash
   mvn clean compile
   ```

3. Ejecuta las pruebas:
   ```bash
   mvn test
   ```

4. Empaqueta la aplicación:
   ```bash
   mvn clean package
   ```

## Ejecución

### Ejecutar Localmente

1. Asegúrate de que MongoDB esté ejecutándose.
2. Configura las propiedades necesarias (puerto, configuración externa, etc.).
3. Ejecuta la aplicación:
   ```bash
   mvn spring-boot:run
   ```
   O
   ```bash
   java -jar target/passive-product-service-0.0.1-SNAPSHOT.jar
   ```

El servicio estará disponible en `http://localhost:8082` (configurable).

### Ejecutar con Docker

1. Construye la imagen Docker:
   ```bash
   docker build -t passive-product-service .
   ```

2. Ejecuta el contenedor:
   ```bash
   docker run -p 8082:8082 passive-product-service
   ```

## Configuración

El servicio utiliza Spring Cloud Config para la configuración centralizada. Las configuraciones se obtienen desde el servidor de configuración en `http://localhost:8888`.

Propiedades importantes:
- `spring.application.name`: passive-product-service
- Puerto del servidor (por defecto 8082)
- URL de MongoDB
- Configuración de Eureka

## API Endpoints

La API está documentada con OpenAPI y se puede acceder a la documentación en `/swagger-ui.html` cuando el servicio esté ejecutándose.

### Endpoints Principales

- `GET /` - Obtener todos los productos pasivos activos
- `POST /` - Crear un nuevo producto pasivo
- `GET /{id}` - Obtener producto por ID (solo activos)
- `PUT /{id}` - Actualizar producto pasivo
- `DELETE /{id}` - Eliminar producto pasivo (soft delete)
- `GET /{id}/balance` - Obtener saldo del producto
- `PATCH /{id}/balance` - Actualizar saldo del producto
- `GET /customer/{customerId}` - Obtener productos activos de un cliente

### Modelos de Datos

- **PassiveProduct**: Representa un producto pasivo con campos como id, customerId, accountType (SAVING, CURRENT, FIXED_TERM), balance, etc.
- **PassiveProductRequest**: Para crear/actualizar productos.
- **BalanceResponse**: Respuesta con el saldo.
- **BalanceUpdateRequest**: Para actualizar el saldo.

## Arquitectura

Este servicio forma parte de una arquitectura de microservicios:

- **Cliente**: Eureka para descubrimiento
- **Configuración**: Spring Cloud Config
- **Base de Datos**: MongoDB reactiva
- **Comunicación**: HTTP RESTful con WebFlux
- **Resiliencia**: Circuit breakers, retries con Resilience4j
- **Observabilidad**: Logs con Logstash, métricas con Micrometer

## Pruebas

Ejecuta las pruebas con:
```bash
mvn test
```

Las pruebas incluyen:
- Pruebas unitarias
- Pruebas de integración
- Pruebas de WebClient
- Cobertura con JaCoCo

## Contribución

1. Crea una rama para tu feature: `git checkout -b feature/nueva-funcionalidad`
2. Realiza tus cambios y agrega pruebas
3. Ejecuta las pruebas: `mvn test`
4. Haz commit: `git commit -am 'Agrega nueva funcionalidad'`
5. Push a la rama: `git push origin feature/nueva-funcionalidad`
6. Crea un Pull Request

## Licencia

Este proyecto está bajo la licencia [especificar licencia].

## Contacto

Para preguntas o soporte, contacta al equipo de desarrollo.
