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

### Iteration 6: Search and filter products

- `GET /api/product` mit optionalen Query-Parametern (kombinierbar):
  - `name` – Suche im Titel (Teilstring, case-insensitive)
  - `categoryId` – Filter nach Kategorie-ID (`1`, `2`, `3`)
  - `shopCategory` – Filter nach Shop-Slug (alle drei Kategorien):
    - `picknickkoerbe` → Picknickkörbe
    - `party-event` → Party & Event
    - `essen-getraenke` → Essen & Getränke
  - `maxPrice` – nur Produkte mit Preis ≤ angegebenem Wert (beliebige Zahl, z. B. `25` oder `50`)
- Ohne Parameter: alle Produkte
- Beispiele:
  - `GET /api/product?shopCategory=party-event`
  - `GET /api/product?maxPrice=25`
  - `GET /api/product?shopCategory=essen-getraenke&maxPrice=15`
  - `GET /api/product?name=Korb&categoryId=1`
- `GET /api/category` – alle Kategorien
- `GET /api/category/translation` – Map `shopCategory` → Anzeigename (für Filter-Dropdown im Frontend)
- Mehr Beispielprodukte im `DataLoader`
- Bruno: `getAllProductsFilterAndSearch.yml`, `getCategoryTranslation.yml`

### Iteration 7: User authentication with Auth0 and Spring Security

- Added `okta-spring-boot-starter` dependency in `pom.xml`
- Auth0 configuration in `application.properties` (`okta.oauth2.issuer`, `okta.oauth2.audience`)
- `SecurityConfig`: OAuth2 resource server; `GET /api/product` public, `POST`/`PUT`/`DELETE` on `/api/product` require authentication
- `User` entity (`app_user` table), `Role` enum (`KUNDE`, `FAHRER`, `ADMIN`), `UserRepository`
- `ProfileController`: `GET /api/profile` returns the logged-in user from the database (matched by JWT `sub` / `oauthId`)
- `ProductController`: admin role check before create, update, and delete (403 for non-admins)
- `DataLoader`: upserts three test users (Kunde, Fahrer, Admin) with Auth0 `oauthId` values
- **After schema change:** restart backend; users are upserted on every start, products load only if DB is empty

### Iteration 8: User profile update

- Extended `User` entity with `firstName`, `lastName`, `street`, `postalCode`, and `city` (replaces single `name` field)
- New `ProfileUpdateRequest` DTO for profile updates
- `ProfileController`: `PUT /api/profile` updates the logged-in user's name and address (email and role stay unchanged)
- Basic validation: all profile fields required and non-blank
- `DataLoader`: test users upserted with sample address data

### Iteration 9: Delivery orders API for drivers (sample data)

- New entities `DeliveryOrder` and `DeliveryStatus` (`OFFEN`, `UNTERWEGS`, `GELIEFERT`); order linked to a FAHRER user
- `DeliveryOrderRepository`, `DeliveryController`: `GET /api/delivery/assigned` (orders for logged-in driver), `PUT /api/delivery/{id}/status` (status update; only own orders)
- `SecurityConfig`: `/api/delivery/**` requires authentication; controller checks `Role.FAHRER`
- `DataLoader`: seeds three sample orders (EG-124, EG-128, EG-131) for `maloku.ardonesa+fahrer@gmail.com`
- **Scope note:** no connection to customer orders yet — sample assignments only. Real order creation, multi-driver accept, and admin assignment are planned for later iterations.
