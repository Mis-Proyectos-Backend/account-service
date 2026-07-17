# Account Service

Microservicio encargado de la administración de cuentas bancarias.

## Tecnologías

- Java 17
- Spring Boot
- Spring WebFlux
- MongoDB
- Redis
- Kafka
- Eureka Client
- Docker
- Maven
- OpenAPI
- JUnit 5
- Mockito
- JaCoCo

## Funcionalidades

- CRUD de cuentas.
- Depósitos.
- Retiros.
- Transferencias entre cuentas.
- Consulta de saldo.
- Validaciones para cuentas de ahorro, corriente y plazo fijo.
- Publicación de eventos mediante Kafka.
- Cache mediante Redis.

## Ejecución local

```bash
mvn clean package
mvn spring-boot:run
```

## Puerto

```
8082
```

## Documentación OpenAPI

```
src/main/resources/openapi/account-openapi.yml
```

## Docker

```bash
docker build -t account-service .
```

## Pruebas

```bash
mvn test
```

## Cobertura

```
target/site/jacoco/index.html
```

## Infraestructura

La infraestructura Docker se encuentra en el repositorio **bank-infra**.