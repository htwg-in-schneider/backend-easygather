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

### Iteration 1c: REST-Controller with model class

- Model-Klassen `Product` und `Category` unter `src/main/java/.../model/`
- `ProductController` liefert Beispieldaten mit `id`, `title`, `description`, `category`, `price`, `imageUrl` (für späteres Frontend)
- Test GET: `curl http://localhost:8081/api/product`
- Test POST: wie Iteration 1b (PowerShell: `Invoke-RestMethod` oder `curl.exe` mit `--%`)

### Iteration 2: CORS Configuration

- `WebConfig` mit globaler CORS-Konfiguration (`config/WebConfig.java`)
- Erlaubt dem Frontend (z. B. `http://localhost:5173` oder GitHub Pages unter `https://htwg-in-schneider.github.io/frontend-easygather/`), die Backend-APIs ohne Cross-Origin-Fehler aufzurufen

### Iteration 3: Database Integration

- H2-Dateidatenbank unter `target/easygather-db`, MariaDB-Treiber in `pom.xml` (für Produktion später)
- `Product` als JPA-`@Entity` mit `equals`/`hashCode`
- `ProductRepository` (Spring Data JPA)
- `DataLoader` befüllt die DB beim ersten Start mit 7 EasyGather-Produkten
- `GET /api/product` liest aus der Datenbank (`productRepository.findAll()`)
- Test: Backend starten, `curl http://localhost:8081/api/product` – IDs werden von der DB vergeben (1, 2, 3, …)

### Iteration 4: CRUD for Products

- Vollständiges CRUD für Produkte:
  - `GET /api/product` – alle Produkte
  - `GET /api/product/{id}` – ein Produkt (404 wenn nicht gefunden)
  - `POST /api/product` – anlegen (201)
  - `PUT /api/product/{id}` – aktualisieren (404 wenn nicht gefunden)
  - `DELETE /api/product/{id}` – löschen (204 bei Erfolg, 404 wenn nicht gefunden)
- Keine Validierung der Entities vor dem Speichern
- Bruno-Requests unter `src/test/bruno` (`getProduct.yml`, `updateProduct.yml`, `deleteProduct.yml`, …)

### Iteration 5: Added 1:n relation Category – Product

- **1 Kategorie → n Produkte** (passend zu EasyGather, nicht Review wie bei Saitenweise)
- `Category` als JPA-Entity mit `title` und `shopCategory` (`picknickkoerbe`, `party-event`, `essen-getraenke`)
- `Product#category` als `@ManyToOne`; `Category#products` mit `@JsonIgnore` (keine Endlosschleife im JSON)
- `GET /api/category` – alle Kategorien
- `GET /api/product/category/{categoryId}` – Produkte einer Kategorie
- `POST /api/product` mit `"category": { "id": 1 }` im JSON-Body
- Bruno: `getCategories.yml`, `getProductsByCategory.yml`
- **Nach Schema-Wechsel:** H2-DB löschen (`Remove-Item -Recurse -Force .\target\easygather-db*`) und Backend neu starten
