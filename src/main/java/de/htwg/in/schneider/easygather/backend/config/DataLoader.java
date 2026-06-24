package de.htwg.in.schneider.easygather.backend.config;

import java.util.Arrays;
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
        };
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

        Product standardKorb = new Product();
        standardKorb.setTitle("Standard-Korb");
        standardKorb.setDescription("Der flexible Allrounder für Treffen im Park mit Freunden oder Kolleg:innen.");
        standardKorb.setCategory(picnicBaskets);
        standardKorb.setPrice(65.00);

        Product familienKorb = new Product();
        familienKorb.setTitle("Familien-Korb");
        familienKorb.setDescription("Großer Mix für Groß und Klein – Snacks, Getränke und kleine Überraschungen inklusive.");
        familienKorb.setCategory(picnicBaskets);
        familienKorb.setPrice(95.00);

        Product ledLichterkette = new Product();
        ledLichterkette.setTitle("LED-Lichterkette");
        ledLichterkette.setDescription("Stimmungsvolle Lichterkette für Abende im Freien – batteriebetrieben und wetterfest.");
        ledLichterkette.setCategory(partyEvent);
        ledLichterkette.setPrice(24.99);

        Product geburtstagsDeko = new Product();
        geburtstagsDeko.setTitle("Geburtstags-Deko Paket");
        geburtstagsDeko.setDescription("Tischdeko, Banner und Accessoires für kleine Feiern im Freien oder zu Hause.");
        geburtstagsDeko.setCategory(partyEvent);
        geburtstagsDeko.setPrice(35.00);

        Product pizza = new Product();
        pizza.setTitle("Pizza Margherita (groß)");
        pizza.setDescription("Frische Pizza zum Teilen – perfekt als Ergänzung zu Picknick und Party.");
        pizza.setCategory(foodAndDrinks);
        pizza.setPrice(12.50);

        Product limonade = new Product();
        limonade.setTitle("Hausgemachte Zitronenlimonade");
        limonade.setDescription("Erfrischend und nicht zu süß – inklusive Mehrwegflaschen für nachhaltige Events.");
        limonade.setCategory(foodAndDrinks);
        limonade.setPrice(8.50);

        productRepository.saveAll(Arrays.asList(
                dateKorb,
                standardKorb,
                familienKorb,
                ledLichterkette,
                geburtstagsDeko,
                pizza,
                limonade));
        LOGGER.info("Initial data loaded successfully.");
    }
}