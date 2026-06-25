package de.htwg.in.schneider.easygather.backend.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.htwg.in.schneider.easygather.backend.model.Category;
import de.htwg.in.schneider.easygather.backend.model.Order;
import de.htwg.in.schneider.easygather.backend.model.Product;
import de.htwg.in.schneider.easygather.backend.model.Role;
import de.htwg.in.schneider.easygather.backend.model.User;
import de.htwg.in.schneider.easygather.backend.repository.CategoryRepository;
import de.htwg.in.schneider.easygather.backend.repository.OrderRepository;
import de.htwg.in.schneider.easygather.backend.repository.ProductRepository;
import de.htwg.in.schneider.easygather.backend.repository.UserRepository;
import de.htwg.in.schneider.easygather.backend.service.DeliveryService;
import de.htwg.in.schneider.easygather.backend.service.OrderNumberService;

@Configuration
public class DataLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataLoader.class);

    @Bean
    public CommandLineRunner loadData(CategoryRepository categoryRepository, ProductRepository productRepository,
            UserRepository userRepository, OrderRepository orderRepository, DeliveryService deliveryService,
            OrderNumberService orderNumberService) {
        return args -> {
            loadInitialUsers(userRepository);
            orderNumberService.backfillMissingOrderNumbers();
            deliveryService.removeOrphanDeliveries();
            deliveryService.syncDeliveryOrderNumbersFromCustomerOrders();
            syncDeliveriesFromExistingOrders(orderRepository, deliveryService);

            if (categoryRepository.count() == 0) {
                LOGGER.info("Database is empty. Loading initial data...");
                loadInitialData(categoryRepository, productRepository);
            } else {
                LOGGER.info("Database already contains data. Skipping data loading.");
            }
            clearPlaceholderProductImages(productRepository);
            backfillIncludedItemsIfMissing(productRepository);
            refreshIncludedItemsForTitles(productRepository, List.of(
                    "Schokobrunnen-Set",
                    "XXL Outdoor-Spiele-Set",
                    "Stilles Wasser",
                    "Sprudelwasser",
                    "Kerzen",
                    "Mehrwegbecher",
                    "Familien-Korb"));
            refreshDescriptionsForTitles(productRepository, Map.of(
                    "Schokobrunnen-Set",
                    "5-stöckiges Schokoladen-Highlight für Gartenpartys – inklusive 4 kg Schokolade, Obst und Zubehör.",
                    "XXL Outdoor-Spiele-Set",
                    "Riesenspaß im Freien mit XXL Tic-Tac-Toe, Wackelturm, Vier gewinnt und Cornhole – ideal für Sommerfeste und Teamevents.",
                    "Stilles Wasser",
                    "Erfrischendes stilles Mineralwasser in einer Mehrwegflasche – ideal für unterwegs.",
                    "Sprudelwasser",
                    "Prickelndes Mineralwasser in einer Mehrwegflasche – erfrischend für heiße Sommertage.",
                    "Kerzen",
                    "Zehn Teelichter für stimmungsvolle Picknick-Abende.",
                    "Mehrwegbecher",
                    "Ein Mehrwegbecher – ideal für Getränke unterwegs."));
            refreshPricesForTitles(productRepository, Map.of(
                    "Stilles Wasser", 1.20,
                    "Sprudelwasser", 1.20,
                    "Servietten", 0.20,
                    "Picknickdecke", 4.90,
                    "Kerzen", 1.50,
                    "Mehrwegbecher", 0.80));
            removeRetiredCatalogProducts(productRepository);
            ensureEventProductsExist(categoryRepository, productRepository);
            ensureFoodProductsExist(categoryRepository, productRepository);
            ensurePicknickConfiguratorProducts(categoryRepository, productRepository);
        };
    }

    private void backfillIncludedItemsIfMissing(ProductRepository productRepository) {
        Map<String, List<String>> defaults = defaultIncludedItemsByTitle();
        for (Product product : productRepository.findAll()) {
            if (!product.getIncludedItems().isEmpty()) {
                continue;
            }
            List<String> items = defaults.get(product.getTitle());
            if (items != null) {
                product.setIncludedItems(items);
                productRepository.save(product);
            }
        }
    }

    private Map<String, List<String>> defaultIncludedItemsByTitle() {
        Map<String, List<String>> defaults = new HashMap<>();
        defaults.put("Date-Korb", List.of(
                "2 Sandwiches",
                "Obst",
                "Zitronenlimonade (2x 0,33l)",
                "Servietten",
                "Picknickdecke inkl. Kerzen"));
        defaults.put("Standard-Korb", List.of(
                "4 Sandwiches",
                "Obst & Gemüsesticks",
                "2 Getränke",
                "Servietten",
                "Picknickdecke"));
        defaults.put("Familien-Korb", List.of(
                "6 Sandwiches",
                "Obstplatte",
                "4 Getränke",
                "Servietten & Becher",
                "Picknickdecke"));
        defaults.put("Picknickkorb konfigurieren", List.of(
                "Einzelprodukte nach Wahl",
                "Essen & Getränke aus dem Shop",
                "Korb-Zubehör optional"));
        defaults.put("Servietten", List.of(
                "20 Servietten",
                "Papier, unbedruckt"));
        defaults.put("Picknickdecke", List.of(
                "1 Picknickdecke (150 × 130 cm)",
                "Wasserabweisend & waschbar"));
        defaults.put("Gemüsesticks", List.of(
                "Möhrensticks",
                "Gurkensticks",
                "Paprikasticks",
                "Kleiner Joghurt-Dip"));
        defaults.put("Kerzen", List.of(
                "10 Teelichter",
                "Sichere Halterung"));
        defaults.put("Mehrwegbecher", List.of(
                "1 Mehrwegbecher",
                "Stabil für unterwegs"));
        defaults.put("LED-Lichterkette", List.of(
                "10 m Lichterkette",
                "Batterien",
                "Befestigungsclips"));
        defaults.put("Geburtstags-Deko Paket", List.of(
                "Happy-Birthday-Banner",
                "Tischdeko",
                "Luftballons",
                "Kerzen"));
        defaults.put("Schokobrunnen-Set", List.of(
                "5-stufige Schokobrunnen",
                "4 kg Schokolade (Vollmilch, Zartbitter & Weiß)",
                "Obstplatte (Erdbeeren, Banane, Marshmallows)",
                "80 Spieße & Dipp-Schälchen",
                "Reinigungs-Set"));
        defaults.put("XXL Outdoor-Spiele-Set", List.of(
                "XXL Tic-Tac-Toe (Holz-X/O & Seil-Gitter)",
                "XXL Wackelturm (54 Holzklötze)",
                "XXL Vier gewinnt (Brett, Spielsteine & Körbe)",
                "Cornhole-Set (2 Bretter & Beanbags)",
                "Spielanleitungen & Transporttaschen"));
        defaults.put("Bluetooth-Musikbox", List.of(
                "Bluetooth-Lautsprecher (IPX5, 20 h Akku)",
                "USB-C-Ladekabel",
                "Smartphone-Halterung",
                "Party-Playlist per QR-Code"));
        defaults.put("Popcornmaschine", List.of(
                "Profi-Popcornmaschine",
                "Mais für ca. 100 Portionen",
                "3 Gewürzmischungen (salzig, süß, Karamell)",
                "50 Portionstüten",
                "Reinigungs-Set"));
        defaults.put("Sonnenschirme (groß)", List.of(
                "2 große Sonnenschirme (Ø 3 m)",
                "2 Schwerlast-Ständer",
                "Transporttaschen",
                "Aufbauanleitung"));
        defaults.put("Pizza Margherita (groß)", List.of(
                "1 große Pizza Margherita",
                "Pizzakarton",
                "Servietten"));
        defaults.put("Salami-Rucola-Sandwich", List.of(
                "Ciabatta-Brötchen",
                "Salamischeiben",
                "Frischer Rucola",
                "Tomatenscheiben",
                "Kräuterfrischkäse-Aufstrich",
                "Olivenöl & Meersalz"));
        defaults.put("Hausgemachte Zitronenlimonade", List.of(
                "2 Flaschen à 0,75 l",
                "Mehrwegflaschen",
                "Eiswürfelbeutel"));
        defaults.put("Stilles Wasser", List.of(
                "1 Flasche à 0,5 l",
                "Mehrwegflasche"));
        defaults.put("Sprudelwasser", List.of(
                "1 Flasche à 0,5 l",
                "Mehrwegflasche"));
        defaults.put("Antipasti-Platte", List.of(
                "Gemischte Oliven",
                "Getrocknete Tomaten",
                "Mozzarella-Kugeln",
                "Grissini",
                "Pesto-Dip"));
        defaults.put("Obstplatte", List.of(
                "Saisonales Obst",
                "Frische Beeren",
                "Minze zum Garnieren",
                "Für ca. 4–6 Personen"));
        defaults.put("Veganes Sandwich", List.of(
                "Vollkornbrötchen",
                "Hummus-Aufstrich",
                "Gurkenscheiben",
                "Tomatenscheiben",
                "Frischer Rucola",
                "Paprikastreifen"));
        return defaults;
    }

    private void ensureEventProductsExist(CategoryRepository categoryRepository, ProductRepository productRepository) {
        Optional<Category> partyEvent = categoryRepository.findByShopCategory("party-event");
        if (partyEvent.isEmpty()) {
            return;
        }
        Category category = partyEvent.get();
        List<Product> eventProducts = List.of(
                buildProduct(
                        "Schokobrunnen-Set",
                        "5-stöckiges Schokoladen-Highlight für Gartenpartys – inklusive 4 kg Schokolade, Obst und Zubehör.",
                        79.90),
                buildProduct(
                        "XXL Outdoor-Spiele-Set",
                        "Riesenspaß im Freien mit XXL Tic-Tac-Toe, Wackelturm, Vier gewinnt und Cornhole – ideal für Sommerfeste und Teamevents.",
                        54.90),
                buildProduct(
                        "Bluetooth-Musikbox",
                        "Kraftvoller Outdoor-Lautsprecher mit langer Akkulaufzeit – für Musik auf der Wiese oder Terrasse.",
                        42.90),
                buildProduct(
                        "Popcornmaschine",
                        "Kino-Feeling für dein Event – frisches Popcorn direkt vor Ort, inklusive Mais und Tüten.",
                        68.90),
                buildProduct(
                        "Sonnenschirme (groß)",
                        "Schatten für große Gruppen: zwei große Schirme mit stabilen Ständern für Garten und Open-Air.",
                        59.90));
        for (Product template : eventProducts) {
            upsertProductIfMissing(productRepository, category, template);
        }
    }

    private void ensureFoodProductsExist(CategoryRepository categoryRepository, ProductRepository productRepository) {
        Optional<Category> foodAndDrinks = categoryRepository.findByShopCategory("essen-getraenke");
        if (foodAndDrinks.isEmpty()) {
            return;
        }
        Category category = foodAndDrinks.get();
        List<Product> foodProducts = List.of(
                buildProduct(
                        "Salami-Rucola-Sandwich",
                        "Frisch belegtes Ciabatta mit Salami, Rucola und Tomate – herzhaft und perfekt für unterwegs.",
                        7.50),
                buildProduct(
                        "Stilles Wasser",
                        "Erfrischendes stilles Mineralwasser in einer Mehrwegflasche – ideal für unterwegs.",
                        1.20),
                buildProduct(
                        "Sprudelwasser",
                        "Prickelndes Mineralwasser in einer Mehrwegflasche – erfrischend für heiße Sommertage.",
                        1.20),
                buildProduct(
                        "Antipasti-Platte",
                        "Mediterrane Auswahl zum Teilen – perfekt als herzhafte Ergänzung zum Picknick.",
                        18.90),
                buildProduct(
                        "Obstplatte",
                        "Bunte Obstauswahl der Saison – frisch, leicht und ideal für Gruppen.",
                        14.90),
                buildProduct(
                        "Veganes Sandwich",
                        "Herzhaft belegtes Vollkornbrötchen mit Hummus und frischem Gemüse – vegan und sättigend.",
                        7.50),
                buildProduct(
                        "Gemüsesticks",
                        "Frische Gemüsesticks mit Dip – knackig, leicht und perfekt fürs Picknick.",
                        4.90));
        for (Product template : foodProducts) {
            upsertProductIfMissing(productRepository, category, template);
        }
    }

    private void ensurePicknickConfiguratorProducts(CategoryRepository categoryRepository,
            ProductRepository productRepository) {
        Optional<Category> picnicBaskets = categoryRepository.findByShopCategory("picknickkoerbe");
        if (picnicBaskets.isEmpty()) {
            return;
        }
        Category category = picnicBaskets.get();
        List<Product> configuratorProducts = List.of(
                buildProduct(
                        "Picknickkorb konfigurieren",
                        "Stelle dir deinen persönlichen Picknickkorb aus Einzelprodukten zusammen.",
                        0.0),
                buildProduct(
                        "Servietten",
                        "Servietten für unterwegs – praktisch für Picknick und Events.",
                        0.20),
                buildProduct(
                        "Picknickdecke",
                        "Weiche Picknickdecke für entspannte Stunden im Freien.",
                        4.90),
                buildProduct(
                        "Kerzen",
                        "Zehn Teelichter für stimmungsvolle Picknick-Abende.",
                        1.50),
                buildProduct(
                        "Mehrwegbecher",
                        "Ein Mehrwegbecher – ideal für Getränke unterwegs.",
                        0.80));
        for (Product template : configuratorProducts) {
            upsertProductIfMissing(productRepository, category, template);
        }
    }

    private void refreshIncludedItemsForTitles(ProductRepository productRepository, List<String> titles) {
        Map<String, List<String>> defaults = defaultIncludedItemsByTitle();
        for (Product product : productRepository.findAll()) {
            if (product.getTitle() == null || !titles.contains(product.getTitle())) {
                continue;
            }
            List<String> items = defaults.get(product.getTitle());
            if (items != null) {
                product.setIncludedItems(items);
                productRepository.save(product);
            }
        }
    }

    private void refreshDescriptionsForTitles(ProductRepository productRepository, Map<String, String> descriptions) {
        for (Product product : productRepository.findAll()) {
            String description = descriptions.get(product.getTitle());
            if (description != null) {
                product.setDescription(description);
                productRepository.save(product);
            }
        }
    }

    private void refreshPricesForTitles(ProductRepository productRepository, Map<String, Double> prices) {
        for (Product product : productRepository.findAll()) {
            Double price = prices.get(product.getTitle());
            if (price != null) {
                product.setPrice(price);
                productRepository.save(product);
            }
        }
    }

    private void removeRetiredCatalogProducts(ProductRepository productRepository) {
        List<String> retiredTitles = List.of("Luftballon-Girlande-Set", "Gartenfackel-Set", "Becher", "Snacks für Kinder");
        for (Product product : productRepository.findAll()) {
            if (product.getTitle() != null && retiredTitles.contains(product.getTitle())) {
                productRepository.delete(product);
                LOGGER.info("Removed retired catalog product: {}", product.getTitle());
            }
        }
    }

    private Product buildProduct(String title, String description, double price) {
        Product product = new Product();
        product.setTitle(title);
        product.setDescription(description);
        product.setPrice(price);
        product.setIncludedItems(defaultIncludedItemsByTitle().get(title));
        return product;
    }

    private void upsertProductIfMissing(ProductRepository productRepository, Category category, Product template) {
        boolean exists = productRepository.findAll().stream()
                .anyMatch(product -> titleMatches(product.getTitle(), template.getTitle()));
        if (exists) {
            return;
        }
        template.setCategory(category);
        productRepository.save(template);
        LOGGER.info("Added catalog product: {}", template.getTitle());
    }

    private boolean titleMatches(String existingTitle, String expectedTitle) {
        return existingTitle != null && existingTitle.equalsIgnoreCase(expectedTitle);
    }

    private void clearPlaceholderProductImages(ProductRepository productRepository) {
        for (Product product : productRepository.findAll()) {
            String imageUrl = product.getImageUrl();
            if (imageUrl != null && imageUrl.contains("picsum.photos")) {
                product.setImageUrl(null);
                productRepository.save(product);
            }
        }
    }

    private void syncDeliveriesFromExistingOrders(OrderRepository orderRepository, DeliveryService deliveryService) {
        for (Order order : orderRepository.findAllWithItems()) {
            deliveryService.createFromOrder(order);
        }
    }

    private void loadInitialUsers(UserRepository userRepository) {
        upsertUser(userRepository, "Max", "Kunde", "Alfred-Wachtel-Straße 8", "78462", "Konstanz",
                "maloku.ardonesa+kunde@gmail.com", "auth0|6a3a6001ffaa52d5c9653b0f", Role.KUNDE);
        upsertUser(userRepository, "Lea", "Fahrer", "Alfred-Wachtel-Straße 8", "78462", "Konstanz",
                "maloku.ardonesa+fahrer@gmail.com", "auth0|6a3a605699022f4b7e9c6581", Role.FAHRER);
        upsertUser(userRepository, "Tom", "Fahrer", "Alfred-Wachtel-Straße 8", "78462", "Konstanz",
                "maloku.ardonesa+fahrer2@gmail.com", "auth0|6a3c0fcebda5fa3340b5031c", Role.FAHRER);
        upsertUser(userRepository, "Admin", "EasyGather", "Alfred-Wachtel-Straße 8", "78462", "Konstanz",
                "maloku.ardonesa+admin@gmail.com", "auth0|6a3a606fffaa52d5c9653b5c", Role.ADMIN);
    }

    private void upsertUser(UserRepository userRepository, String firstName, String lastName, String street,
            String postalCode, String city, String email, String oauthId, Role role) {
        Optional<User> existing = userRepository.findByOauthId(oauthId);
        if (existing.isEmpty()) {
            existing = userRepository.findByEmail(email);
        }
        if (existing.isPresent()) {
            User user = existing.get();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setStreet(street);
            user.setPostalCode(postalCode);
            user.setCity(city);
            user.setEmail(email);
            user.setOauthId(oauthId);
            user.setRole(role);
            userRepository.save(user);
            LOGGER.info("Updated existing {} user with email={}", role, email);
        } else {
            User user = new User();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setStreet(street);
            user.setPostalCode(postalCode);
            user.setCity(city);
            user.setEmail(email);
            user.setOauthId(oauthId);
            user.setRole(role);
            userRepository.save(user);
            LOGGER.info("Created new {} user with email={}", role, email);
        }
    }

    private void loadInitialData(CategoryRepository categoryRepository, ProductRepository productRepository) {
        Category picnicBaskets = new Category();
        picnicBaskets.setTitle("Picknickkörbe");
        picnicBaskets.setShopCategory("picknickkoerbe");

        Category partyEvent = new Category();
        partyEvent.setTitle("Party & Event");
        partyEvent.setShopCategory("party-event");

        Category foodAndDrinks = new Category();
        foodAndDrinks.setTitle("Essen & Getränke");
        foodAndDrinks.setShopCategory("essen-getraenke");

        categoryRepository.saveAll(Arrays.asList(picnicBaskets, partyEvent, foodAndDrinks));

        Product dateKorb = new Product();
        dateKorb.setTitle("Date-Korb");
        dateKorb.setDescription("Kleine, stilvolle Auswahl für zwei Personen – ideal für ein entspanntes Date im Park.");
        dateKorb.setCategory(picnicBaskets);
        dateKorb.setPrice(45.00);
        dateKorb.setIncludedItems(defaultIncludedItemsByTitle().get("Date-Korb"));

        Product standardKorb = new Product();
        standardKorb.setTitle("Standard-Korb");
        standardKorb.setDescription("Der flexible Allrounder für Treffen im Park mit Freunden oder Kolleg:innen.");
        standardKorb.setCategory(picnicBaskets);
        standardKorb.setPrice(65.00);
        standardKorb.setIncludedItems(defaultIncludedItemsByTitle().get("Standard-Korb"));

        Product familienKorb = new Product();
        familienKorb.setTitle("Familien-Korb");
        familienKorb.setDescription("Großer Mix für Groß und Klein – Snacks, Getränke und kleine Überraschungen inklusive.");
        familienKorb.setCategory(picnicBaskets);
        familienKorb.setPrice(95.00);
        familienKorb.setIncludedItems(defaultIncludedItemsByTitle().get("Familien-Korb"));

        Product konfigurierbarerKorb = buildProduct(
                "Picknickkorb konfigurieren",
                "Stelle dir deinen persönlichen Picknickkorb aus Einzelprodukten zusammen.",
                0.0);
        konfigurierbarerKorb.setCategory(picnicBaskets);

        Product servietten = buildProduct(
                "Servietten",
                "Servietten für unterwegs – praktisch für Picknick und Events.",
                0.20);
        servietten.setCategory(picnicBaskets);

        Product picknickdecke = buildProduct(
                "Picknickdecke",
                "Weiche Picknickdecke für entspannte Stunden im Freien.",
                4.90);
        picknickdecke.setCategory(picnicBaskets);

        Product kerzen = buildProduct(
                "Kerzen",
                "Zehn Teelichter für stimmungsvolle Picknick-Abende.",
                1.50);
        kerzen.setCategory(picnicBaskets);

        Product mehrwegbecher = buildProduct(
                "Mehrwegbecher",
                "Ein Mehrwegbecher – ideal für Getränke unterwegs.",
                0.80);
        mehrwegbecher.setCategory(picnicBaskets);

        Product gemuesesticks = buildProduct(
                "Gemüsesticks",
                "Frische Gemüsesticks mit Dip – knackig, leicht und perfekt fürs Picknick.",
                4.90);
        gemuesesticks.setCategory(foodAndDrinks);

        Product ledLichterkette = new Product();
        ledLichterkette.setTitle("LED-Lichterkette");
        ledLichterkette.setDescription("Stimmungsvolle Lichterkette für Abende im Freien – batteriebetrieben und wetterfest.");
        ledLichterkette.setCategory(partyEvent);
        ledLichterkette.setPrice(24.99);
        ledLichterkette.setIncludedItems(defaultIncludedItemsByTitle().get("LED-Lichterkette"));

        Product geburtstagsDeko = new Product();
        geburtstagsDeko.setTitle("Geburtstags-Deko Paket");
        geburtstagsDeko.setDescription("Tischdeko, Banner und Accessoires für kleine Feiern im Freien oder zu Hause.");
        geburtstagsDeko.setCategory(partyEvent);
        geburtstagsDeko.setPrice(35.00);
        geburtstagsDeko.setIncludedItems(defaultIncludedItemsByTitle().get("Geburtstags-Deko Paket"));

        Product schokobrunnen = buildProduct(
                "Schokobrunnen-Set",
                "5-stöckiges Schokoladen-Highlight für Gartenpartys – inklusive 4 kg Schokolade, Obst und Zubehör.",
                79.90);
        schokobrunnen.setCategory(partyEvent);

        Product outdoorSpiele = buildProduct(
                "XXL Outdoor-Spiele-Set",
                "Riesenspaß im Freien mit XXL Tic-Tac-Toe, Wackelturm, Vier gewinnt und Cornhole – ideal für Sommerfeste und Teamevents.",
                54.90);
        outdoorSpiele.setCategory(partyEvent);

        Product musikbox = buildProduct(
                "Bluetooth-Musikbox",
                "Kraftvoller Outdoor-Lautsprecher mit langer Akkulaufzeit – für Musik auf der Wiese oder Terrasse.",
                42.90);
        musikbox.setCategory(partyEvent);

        Product popcornmaschine = buildProduct(
                "Popcornmaschine",
                "Kino-Feeling für dein Event – frisches Popcorn direkt vor Ort, inklusive Mais und Tüten.",
                68.90);
        popcornmaschine.setCategory(partyEvent);

        Product sonnenschirme = buildProduct(
                "Sonnenschirme (groß)",
                "Schatten für große Gruppen: zwei große Schirme mit stabilen Ständern für Garten und Open-Air.",
                59.90);
        sonnenschirme.setCategory(partyEvent);

        Product pizza = new Product();
        pizza.setTitle("Pizza Margherita (groß)");
        pizza.setDescription("Frische Pizza zum Teilen – perfekt als Ergänzung zu Picknick und Party.");
        pizza.setCategory(foodAndDrinks);
        pizza.setPrice(12.50);
        pizza.setIncludedItems(defaultIncludedItemsByTitle().get("Pizza Margherita (groß)"));

        Product limonade = new Product();
        limonade.setTitle("Hausgemachte Zitronenlimonade");
        limonade.setDescription("Erfrischend und nicht zu süß – inklusive Mehrwegflaschen für nachhaltige Events.");
        limonade.setCategory(foodAndDrinks);
        limonade.setPrice(8.50);
        limonade.setIncludedItems(defaultIncludedItemsByTitle().get("Hausgemachte Zitronenlimonade"));

        Product salamiSandwich = buildProduct(
                "Salami-Rucola-Sandwich",
                "Frisch belegtes Ciabatta mit Salami, Rucola und Tomate – herzhaft und perfekt für unterwegs.",
                7.50);
        salamiSandwich.setCategory(foodAndDrinks);

        Product stillesWasser = buildProduct(
                "Stilles Wasser",
                "Erfrischendes stilles Mineralwasser in einer Mehrwegflasche – ideal für unterwegs.",
                1.20);
        stillesWasser.setCategory(foodAndDrinks);

        Product sprudelwasser = buildProduct(
                "Sprudelwasser",
                "Prickelndes Mineralwasser in einer Mehrwegflasche – erfrischend für heiße Sommertage.",
                1.20);
        sprudelwasser.setCategory(foodAndDrinks);

        Product antipastiPlatte = buildProduct(
                "Antipasti-Platte",
                "Mediterrane Auswahl zum Teilen – perfekt als herzhafte Ergänzung zum Picknick.",
                18.90);
        antipastiPlatte.setCategory(foodAndDrinks);

        Product obstplatte = buildProduct(
                "Obstplatte",
                "Bunte Obstauswahl der Saison – frisch, leicht und ideal für Gruppen.",
                14.90);
        obstplatte.setCategory(foodAndDrinks);

        Product veganesSandwich = buildProduct(
                "Veganes Sandwich",
                "Herzhaft belegtes Vollkornbrötchen mit Hummus und frischem Gemüse – vegan und sättigend.",
                7.50);
        veganesSandwich.setCategory(foodAndDrinks);

        productRepository.saveAll(Arrays.asList(
                dateKorb,
                standardKorb,
                familienKorb,
                konfigurierbarerKorb,
                servietten,
                picknickdecke,
                kerzen,
                mehrwegbecher,
                ledLichterkette,
                geburtstagsDeko,
                schokobrunnen,
                outdoorSpiele,
                musikbox,
                popcornmaschine,
                sonnenschirme,
                pizza,
                limonade,
                salamiSandwich,
                stillesWasser,
                sprudelwasser,
                antipastiPlatte,
                obstplatte,
                veganesSandwich,
                gemuesesticks));
        LOGGER.info("Initial data loaded successfully.");
    }
}