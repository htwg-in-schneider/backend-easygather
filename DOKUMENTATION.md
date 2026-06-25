# EasyGather – Backend-Dokumentation (Abgabe 5)

**Team:** Ardonesa Maloku, Leah Reinbold  
**Repository:** [easygather-backend](https://github.com/htwg-in-schneider/easygather-backend)

> Diese Datei ergänzt die **Backend-spezifische** Abgabe-Dokumentation.  
> Die **vollständige Projektdoku** (Use Cases, Design, Testzugänge, Frontend) steht im Frontend-Repo:  
> **[frontend-easygather/DOKUMENTATION.md](https://github.com/htwg-in-schneider/frontend-easygather/blob/main/DOKUMENTATION.md)**

> Die **Entwicklungsschritte** (Iterationen 0–15) bleiben im [README.md](./README.md).

---

## Inhaltsverzeichnis

1. [Laufzeit und URLs](#1-laufzeit-und-urls)
2. [Datenmodell](#2-datenmodell)
3. [REST-API (Überblick)](#3-rest-api-überblick)
4. [Sicherheit und Validierung](#4-sicherheit-und-validierung)
5. [Testdaten und Testzugänge](#5-testdaten-und-testzugänge)
6. [Deployment](#6-deployment)
7. [Hardcodierte Werte](#7-hardcodierte-werte)
8. [Verweise](#8-verweise)

---

## 1. Laufzeit und URLs

| Umgebung | URL / Befehl |
|----------|----------------|
| Lokal | `http://localhost:8081` · `.\mvnw.cmd spring-boot:run` |
| Produktion | https://easygather-backend.onrender.com |
| Health-Check (öffentlich) | `GET /api/product` · `GET /api/category` |

Konfiguration: `src/main/resources/application.properties`  
Produktion: Umgebungsvariablen `SPRING_DATASOURCE_*` (MariaDB), `PORT` (Render).

---

## 2. Datenmodell

### 2.1 Vergleich zur ursprünglichen Spezifikation (UML)

| Geplant (Blatt 3) | Umsetzung |
|-------------------|-----------|
| Konto mit `passwortHash`, `kontotyp` | `User` mit `oauthId` (Auth0), `Role` enum |
| Persistenter Warenkorb | Warenkorb nur im **Frontend**; Bestellung aus Cart-Payload |
| Zahlung als eigene Entität mit Status | `paymentMethod` an `Order` (simuliert) |
| Lieferadresse als Entität | Felder in `Order` (Straße, PLZ, Ort, …) |
| Lieferstatus `offen/unterwegs/geliefert` | `DeliveryOrder.status`: `EINGEGANGEN`, `ANGENOMMEN`, `UNTERWEGS`, `GELIEFERT` |

### 2.2 Zentrale JPA-Entitäten

| Entität | Zweck |
|---------|--------|
| `Category` | Shop-Kategorien (`shopCategory`-Slug, optional `imageUrl`) |
| `Product` | Artikel mit Preis, `includedItems`, Kategorie, optionalem Bild |
| `User` | Profil + Rolle (`KUNDE`, `FAHRER`, `ADMIN`), verknüpft mit Auth0 `sub` |
| `Order` / `OrderItem` | Kundenbestellung inkl. Adresse, Zahlungsart, Bestellnummer |
| `DeliveryOrder` | Lieferauftrag, Status, optional zugewiesener Fahrer |

Schema-Updates: `spring.jpa.hibernate.ddl-auto=update`  
Zusätzlich: `H2SchemaMigration.java` (idempotente Spalten-Migration für ältere MariaDB-Instanzen auf Render).

### 2.3 Daten beim Start (`DataLoader.java`)

Bei **leerer** Datenbank: Kategorien, Produkte, Demo-Bestellungen.  
Bei **bestehender** DB: Preis-Updates, neue Produkte (`ensureFoodProductsExist`, `ensurePicknickConfiguratorProducts`, …), Entfernen veralteter Artikel, Backfill `includedItems`.

---

## 3. REST-API (Überblick)

### Öffentlich (GET, ohne JWT)

| Endpoint | Beschreibung |
|----------|--------------|
| `GET /api/product` | Produkte (Filter: `name`, `shopCategory`, `maxPrice`) |
| `GET /api/product/{id}` | Einzelprodukt |
| `GET /api/category` | Kategorien |
| `GET /api/category/translation` | Anzeigenamen für Filter |

### Authentifiziert (JWT / Auth0)

| Bereich | Endpunkte (Auszug) |
|---------|-------------------|
| Profil | `GET/PUT /api/profile` |
| Bestellungen (Kunde) | `GET /api/order`, `GET /api/order/{id}`, `POST /api/order` |
| Bestellungen (Admin) | `GET /api/order/admin/all?q=` |
| Lieferungen (Fahrer) | `GET /api/delivery`, `PUT /api/delivery/{id}/status`, Annahme-Workflow |
| Lieferungen (Admin) | `GET /api/delivery/admin/all?q=`, Zuweisung Fahrer |
| Stammdaten Admin | `POST/PUT/DELETE /api/product`, `/api/category` |
| Nutzer Admin | `GET /api/user`, `PUT /api/user/{id}` (kein POST) |

Bruno-Tests: `src/test/bruno/`

---

## 4. Sicherheit und Validierung

### 4.1 `SecurityConfig.java`

- **Chain 1:** Öffentliche Shop-Reads (`GET /api/product`, `/api/category/...`) – **ohne** OAuth2-Filter
- **Chain 2:** Resource Server mit JWT; rollenbasierte `requestMatchers`
- Admin-only: Produkt-/Kategorie-Schreibzugriffe, User-API, Admin-Lieferungen
- Fahrer: Liefer-Endpoints mit Fahrer- bzw. Admin-Prüfung in `DeliveryController`

### 4.2 Validierung (Beispiele)

| Bereich | Backend |
|---------|---------|
| Bestellung | Pflichtfelder Adresse, PLZ (5-stellig), Positionen, Coupon `EASY10` |
| Produkt/Kategorie | Titel, Preis, Kategorie vorhanden |
| Profil | Pflichtfelder Name/Adresse |
| Nutzer (Admin) | Rolle aus erlaubter Menge |

Tests: `SecurityConfigTest.java`, Maven `verify` in CI (`.github/workflows/verify.yml`).

---

## 5. Testdaten und Testzugänge

### 5.1 In der Datenbank (`DataLoader.loadInitialUsers`)

| Rolle | E-Mail | Auth0-Verknüpfung |
|-------|--------|-------------------|
| KUNDE | `maloku.ardonesa+kunde@gmail.com` | `auth0|6a3a6001ffaa52d5c9653b0f` |
| FAHRER | `maloku.ardonesa+fahrer@gmail.com` | `auth0|6a3a605699022f4b7e9c6581` |
| FAHRER | `maloku.ardonesa+fahrer2@gmail.com` | `auth0|6a3c0fcebda5fa3340b5031c` |
| ADMIN | `maloku.ardonesa+admin@gmail.com` | `auth0|6a3a606fffaa52d5c9653b5c` |

### 5.2 Passwörter (Auth0)

| Rolle | E-Mail | Passwort |
|-------|--------|----------|
| Kunde | `maloku.ardonesa+kunde@gmail.com` | `testk123!` |
| Admin | `maloku.ardonesa+admin@gmail.com` | `testa123!` |
| Fahrer | `maloku.ardonesa+fahrer@gmail.com` | `testf123!` |
| Fahrer 2 | `maloku.ardonesa+fahrer2@gmail.com` | `testf123!` |

Login erfolgt im **Frontend** über Auth0; das Backend prüft nur das JWT und die gespeicherte Rolle.

---

## 6. Deployment

| Aspekt | Details |
|--------|---------|
| Plattform | [render.com](https://render.com), Region Frankfurt |
| Build | `Dockerfile`, `server.port=${PORT:8081}` |
| Datenbank | MariaDB (HTWG Cloud), Credentials nur als Env-Vars |
| Auto-Deploy | Push auf `main` → Render baut neu |
| CI | GitHub Actions `verify.yml` (Maven `verify`) |
| E-Mail | SMTP auf Render blockiert – Bestellbestätigung per E-Mail in Prod **nicht** möglich |

Siehe README **Iteration 13**.

---

## 7. Hardcodierte Werte

| Was | Wo |
|-----|-----|
| Versand **4,90 €** | `OrderController.SHIPPING_COST` |
| Gutschein **EASY10** | `OrderController.VALID_COUPON_CODE` |
| Auth0 Issuer/Audience | `application.properties` (`okta.oauth2.*`) |
| Demo-Produkte/Preise | `DataLoader.java` |

Frontend-Spiegelung: Tabelle in [frontend README / DOKUMENTATION](https://github.com/htwg-in-schneider/frontend-easygather/blob/main/DOKUMENTATION.md#5-weitere-anforderungen-aufgabe-5).

---

## 8. Verweise

| Dokument | Inhalt |
|----------|--------|
| [DOKUMENTATION.md (Frontend)](https://github.com/htwg-in-schneider/frontend-easygather/blob/main/DOKUMENTATION.md) | **Haupt-Abgabedokumentation** (Spezifikation, Use Cases, Design, URLs) |
| [README.md](./README.md) | **Iterationen 0–15** – Backend-Entwicklung |
| [frontend-easygather/README.md](https://github.com/htwg-in-schneider/frontend-easygather/blob/main/README.md) | **Iterationen 0–22** – Frontend-Entwicklung |
