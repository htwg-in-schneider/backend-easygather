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

### Iteration 1a: First REST Controller

- `ProductController` mit `GET /api/product` – liefert eine Liste von Produktnamen (Strings).
- In `application.properties`: DataSource/JPA vorerst deaktiviert (wie im Beispielprojekt).
- Test: `curl http://localhost:8081/api/product` oder Bruno unter `src/test/bruno`.

### Iteration 1b: JSON (de)serialization

- `ProductController` unterstützt **GET** und **POST** auf `/api/product` mit Java-Objekten (`title`, `description`) statt Strings.
- Test GET: `curl http://localhost:8081/api/product`
- Test POST: `curl -X POST http://localhost:8081/api/product -H "Content-Type: application/json" -d "{\"title\":\"Date-Korb\",\"description\":\"Kleine, stilvolle Auswahl für zwei Personen.\"}"`
- Bruno: `src/test/bruno/createProduct.yml`
