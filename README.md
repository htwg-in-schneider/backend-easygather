# easygather-backend

Spring-Boot-Backend für **EasyGather** (Picknick- und Event-Bestellungen).

## How to Run

```sh
mvn spring-boot:run
```

Oder unter Windows:

```sh
.\mvnw.cmd spring-boot:run
```

Die Anwendung startet auf **http://localhost:8081**.

## Iterations

### Iteration 0: Spring Boot project from start.spring.io

- Maven-Projekt mit Spring Web, Spring Data JPA und H2.
- `application.properties`: Port 8081, H2-Datei-Datenbank unter `target/easygather-db`.
