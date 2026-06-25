# easygather-backend

Spring-Boot-Backend für **EasyGather** (Picknick- und Event-Bestellungen).

## Dokumentation (Abgabe 5)

| Dokument | Inhalt |
|----------|--------|
| **[DOKUMENTATION.md](./DOKUMENTATION.md)** | Backend-Abgabe-Doku (API, Datenmodell, Sicherheit, Testzugänge) |
| **[README.md](./README.md)** (dieses File) | **Iterationen 0–15** – Entwicklungsverlauf Schritt für Schritt |
| **[Frontend-DOKUMENTATION.md](https://github.com/htwg-in-schneider/frontend-easygather/blob/main/DOKUMENTATION.md)** | Vollständige Projektdoku (Use Cases, Design, URLs) |

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
- Basic validation: first and last name required; address fields optional on profile (required at order checkout in frontend)
- `DataLoader`: test users upserted with sample address data; new users can be added with `oauthId` and e-mail

### Iteration 9: Order process and persistence

- New entities `Order`, `OrderItem` with `OrderStatus` and `PaymentMethod`; fixed shipping cost **4,90 €**
- `OrderController`:
  - `POST /api/order` – create order for logged-in user (items, delivery address, payment method); persists to DB
  - `GET /api/order` – list own orders; `GET /api/order/{id}` – single order
- Optional order confirmation e-mail via Spring Mail (`SPRING_MAIL_USERNAME` / `SPRING_MAIL_PASSWORD`); order is saved even if mail is not configured
- Coupon code **EASY10** in order request: 10 % discount on subtotal
- `SecurityConfig`: `/api/order` requires authentication
- `ProfileController`: auto-link or create user on first profile access by JWT `sub` / e-mail (test users with roles still via `DataLoader`)

### Iteration 10: Delivery orders API for drivers (sample data)

- New entities `DeliveryOrder` and `DeliveryStatus` (`OFFEN`, `UNTERWEGS`, `GELIEFERT`); order linked to a FAHRER user
- `DeliveryOrderRepository`, `DeliveryController`: `GET /api/delivery/assigned` (orders for logged-in driver), `PUT /api/delivery/{id}/status` (status update; only own orders)
- `SecurityConfig`: `/api/delivery/**` requires authentication; controller checks `Role.FAHRER`
- Initial version used sample orders in `DataLoader` (later replaced in iteration 12)

### Iteration 11: Admin master data (categories, users, orders)

- Extended `CategoryController`: CRUD for admins (`POST` / `PUT` / `DELETE`); `GET` returns `CategorySummary` with product count; optional search `?title=`
- New `UserController`: `GET /api/user`, `GET /api/user/{id}`, `PUT /api/user/{id}` (admin only; role editable, no user creation)
- `OrderController`: `GET /api/order/admin/all` with search for admins; admin can open any order via `GET /api/order/{id}`
- `AdminAuth` helper for shared admin role checks; `SecurityConfig`: CSRF disabled, stateless sessions, category/user endpoints authenticated
- Category delete removes all products in that category (cascade)

### Iteration 12: Driver accept workflow, order numbers and status sync

- `DeliveryStatus`: `EINGEGANGEN`, `ANGENOMMEN`, `UNTERWEGS`, `GELIEFERT` (replaces `OFFEN`)
- `DeliveryOrder` linked to `Order` via `customer_order_id` (1:1); delivery created on `POST /api/order`
- `POST /api/delivery/{id}/accept`: first driver to accept gets the order; others receive 409
- `GET /api/delivery/assigned` returns `DriverDashboardResponse` with `available` and `myDeliveries`
- `PUT /api/delivery/{id}/status`: only own deliveries; transitions `ANGENOMMEN` → `UNTERWEGS` → `GELIEFERT`
- `OrderStatus.UNTERWEGS` synced when driver sets delivery to `UNTERWEGS`; `GELIEFERT` → `ABGESCHLOSSEN`
- `OrderNumberService`: system-wide sequential order numbers (`EG-0001`, `EG-0002`, …)
- `DeliveryOrder.orderCreatedAt` for driver dashboard sorting
- `H2SchemaMigration`: extends H2 ENUM columns for new statuses; `DataLoader` removes orphan sample deliveries

### Iteration 13: Deploy to production

- `Dockerfile` for deployment on [render.com](https://render.com) (Web Service, **Region Frankfurt / EU Central**)
- `server.port=${PORT:8081}` so Render can inject its port
- MariaDB via environment variables (`SPRING_DATASOURCE_URL`, driver, username, password) — never commit passwords
- `DataLoader` seeds categories, products, and test users when the database is empty
- `.github/workflows/verify.yml`: Maven build verification on push/PR
- **Live API:** `https://easygather-backend.onrender.com`
- **Note:** outgoing SMTP is blocked on Render; order confirmation e-mails do not work in production (orders are still saved)

### Iteration 14: Admin delivery management, search and image storage

- **Admin delivery API:** `GET /api/delivery/admin/all?q=` – all deliveries with optional search (order number, customer, driver, status)
- **Driver assignment:** `PUT /api/delivery/admin/{id}/assign` (body: `driverId`), `PUT /api/delivery/admin/{id}/unassign` – admin-only; resets delivery to `EINGEGANGEN` on unassign
- New DTO `AdminDeliverySummary`; extended `AdminOrderDetail` / `AdminOrderSummary` with delivery and driver fields
- **User API:** optional `?role=FAHRER` on `GET /api/user` for driver dropdown; improved search – full name, multi-word queries (e.g. `Max Kunde`), role labels (`kunde`, `fahrer`, `admin`)
- **Order admin search:** multi-word customer name matching (same token logic as users)
- **Image storage:** `Product.imageUrl` and `Category.imageUrl` as `@Lob` for Base64 uploads from the frontend; `DataLoader` no longer seeds Picsum URLs and removes placeholder URLs on startup
- `application.properties`: `server.tomcat.max-http-form-post-size=8MB` for larger image payloads

### Iteration 15: Picknickkorb-Konfigurator, Zubehör und Katalog-Erweiterungen

- **Picknickkorb konfigurieren:** new product in category `picknickkoerbe` (price 0 €) – entry point for building a custom basket from individual items
- **Configurator-only products** in `picknickkoerbe`: Servietten, Picknickdecke, Kerzen (10 Teelichter), Mehrwegbecher – seeded via `ensurePicknickConfiguratorProducts()`
- **Essen & Getränke:** new/updated products (Salami-Rucola-Sandwich, Stilles Wasser, Sprudelwasser, Antipasti-Platte, Obstplatte, Veganes Sandwich, Gemüsesticks); `refreshPricesForTitles()` updates prices on every startup
- **Events:** additional rental products (Schokobrunnen-Set, XXL Outdoor-Spiele-Set, Bluetooth-Musikbox, Popcornmaschine, Sonnenschirme)
- **Catalog cleanup:** `removeRetiredCatalogProducts()` removes obsolete titles (e.g. Becher, Snacks für Kinder); Familien-Korb included items updated
- **Included items:** extended `defaultIncludedItemsByTitle()` / `backfillIncludedItemsIfMissing()` for baskets, accessories, and new food items
- **H2 migration:** `H2SchemaMigration` drops legacy `product.price_per_day` column (fixes startup on older local databases)
- **MariaDB / Render:** same `H2SchemaMigration` adds missing `category.image_url`, `product.image_url`, and `product.included_items_text` columns on startup if absent (idempotent; fixes `/api/product` on existing production databases)
